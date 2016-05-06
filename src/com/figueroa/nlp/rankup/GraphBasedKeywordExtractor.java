package com.figueroa.nlp.rankup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.rake.RakeNode;
import com.figueroa.nlp.textrank.MetricVector;
import com.figueroa.nlp.textrank.SynsetLink;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.TextRankGraph;
import com.figueroa.nlp.textrank.TextRankNode;
import com.figueroa.util.Abstract;
import com.figueroa.util.AbstractManager;

/**
 * A wrapper class for the keyword extractor algorithm to use
 * (e.g. TextRank, RAKE, etc.)
 * 
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2015
 */
public class GraphBasedKeywordExtractor {
    
	private static final Logger logger = Logger.getLogger(AbstractManager.class);
	
    public static enum GraphBasedKeywordExtractionMethod {
        TEXTRANK, RAKE
    }
    
    /**
     * Get an KeywordExtractionMethod enum type from a string
     * @param methodString
     * @return a KeywordExtractionMethod
     */
    public static GraphBasedKeywordExtractionMethod getGraphBasedKeywordExtractionMethodFromString(
            String methodString) {
        
        // Default is TextRank
        if (methodString == null) {
            return GraphBasedKeywordExtractionMethod.TEXTRANK;
        }
        
        for (GraphBasedKeywordExtractionMethod method : GraphBasedKeywordExtractionMethod.values()) {
            if (methodString.equalsIgnoreCase(method.name())) {
                return method;
            }
        }
        
        return null;
    }
    
    private final RankUpProperties rankUpProperties;
    public final GraphBasedKeywordExtractionMethod keywordExtractionMethod;
    
    public final TextRank textRank;
    public final Rake rake;
    
    /**
     * Initialize the extractor with the TextRank method.
     * @param logger
     * @param rankUpProperties
     * @param textRank 
     */
    public GraphBasedKeywordExtractor(RankUpProperties rankUpProperties,
            TextRank textRank) {
        this.rankUpProperties = rankUpProperties;
        this.textRank = textRank;
        this.rake = null;
        this.keywordExtractionMethod = GraphBasedKeywordExtractionMethod.TEXTRANK;
    }
    
    /**
     * Initialize the extractor with the RAKE method.
     * @param logger
     * @param rankUpProperties
     * @param rake 
     */
    public GraphBasedKeywordExtractor(
    		RankUpProperties rankUpProperties,
            Rake rake) {
        this.rankUpProperties = rankUpProperties;
        this.rake = rake;
        this.textRank = null;
        this.keywordExtractionMethod = GraphBasedKeywordExtractionMethod.RAKE;
    }
    
    public List<KeyPhrase> extractKeyphrases(Abstract abs) throws Exception {
        
        List<KeyPhrase> keyphrases;
        // Check if the abstract doesn't already have the keyphrases from before
        if ((keyphrases = abs.getOriginalKeyphraseSet()) != null) {
            // Reset scores and weights
            abs.resetScoresAndWeights();

            // If RAKE, set RAKE back to original state
            if (keywordExtractionMethod == GraphBasedKeywordExtractionMethod.RAKE) {
                rake.setRakeToOriginalState(abs);
            }
            
            return keyphrases;
        }
        
        switch (keywordExtractionMethod) {
            case TEXTRANK:
                keyphrases = extractTextRankKeyphrases(abs);
                break;
            case RAKE:
                keyphrases = extractRakeKeyphrases(abs);
                break;
            default:
                return null;
        }
        
        abs.setOriginalKeyphraseSet(keyphrases);
        
        return keyphrases;
    }
    
    /**
     * Extracts the keyphrases from an abstract (text) using the TextRank method.
     * @param abs
     * @return
     * @throws Exception 
     */
    private List<KeyPhrase> extractTextRankKeyphrases(Abstract abs)
            throws Exception {

        TreeSet<KeyPhrase> textRankKeyphrases = new TreeSet<>();

        // Step 1: Extract TextRank Keyphrases
        logger.info("1.1 Running TextRank...");

        Collection<MetricVector> metricVectorCollection =
                textRank.run(abs.getOriginalText());
        TreeSet<MetricVector> metricSpace = new TreeSet<>(metricVectorCollection);
        
        // Set originalTextRankNodeScores after running for first time
        for (TextRankNode node : textRank.getGraph().values()) {
            node.setOriginalRank(node.getRank());
        }

        // Step 2: Map metric vectors with RankUp keyphrase nodes
        logger.info("1.2 Mapping metric vectors to RankUp keyphrase nodes...");
        if (!rankUpProperties.useWholeTextRankGraph) {
            for (MetricVector mv : metricSpace) {

                double metric = mv.metric;

                KeyPhrase keyphrase = new KeyPhrase(mv.value.text, metric,
                        metric, mv.value.getParentNode());
                textRankKeyphrases.add(keyphrase);
            }
        }
        else {
            TextRankGraph textRankGraph = textRank.getGraph();
            for (TextRankNode node : textRankGraph.values()) {
                // Don't add synset links
                if (!(node.value instanceof SynsetLink)) {
                    double originalTextRankScore = -1.0;
                    for (MetricVector mv : metricSpace) {
                        if (mv.value.getParentNode().equals(node)) {
                            originalTextRankScore = mv.metric;
                            break;
                        }
                    }
                    // Check that node rank is a valid value
                    double nodeRank = node.getRank();

                    KeyPhrase keyphrase = new KeyPhrase(node.value.text, nodeRank,
                            originalTextRankScore, node);
                    textRankKeyphrases.add(keyphrase);
                }
            }
        }

        return new ArrayList<>(textRankKeyphrases);
    }
    
    /**
     * Extracts the keyphrases from an abstract (text) using the RAKE method.
     * @param abs
     * @return
     * @throws Exception 
     */
    private List<KeyPhrase> extractRakeKeyphrases(Abstract abs)
            throws Exception {
        
        ArrayList<KeyPhrase> rakeKeyphrases = new ArrayList<>();

        // Step 1: Extract Rake Keyphrases
        logger.info("1.1 Running RAKE...");

        rake.runRake(abs.getOriginalText());

        // Step 2: Map Rake Nodes with RankUp keyphrase nodes
        logger.info("1.2 Mapping RAKE Nodes to RankUp keyphrase nodes...");
        for (RakeNode rakeNode : rake.rakeFullGraph.values()) {
            KeyPhrase keyphrase = 
                    new KeyPhrase(rakeNode.text, rakeNode.rank, rakeNode.rank, rakeNode);
            rakeKeyphrases.add(keyphrase);
        }
        
        // Set abstract's rake graph, keyphrases and words
        abs.setRakeFullGraph(rake.rakeFullGraph);
        abs.setCurrentRakeKeyphrases(rake.currentKeyphrases);
        abs.setCurrentRakeWords(rake.currentWords);

        return rakeKeyphrases;
    }
    
}
