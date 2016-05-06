package com.figueroa.nlp.rankup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.GraphBasedKeywordExtractionMethod;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetLevel;
import com.figueroa.nlp.POSTagger;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.nlp.Stopwords;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.rake.RakeNode;
import com.figueroa.nlp.rake.RakeNode.RakeNodeType;
import com.figueroa.util.Abstract;
import com.figueroa.util.AbstractManager;

/**
 * RankUp is an unsupervised approach for keyphrase extraction
 * from short articles based on back propagation.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2013
 */
public class RankUp {

    // Logger and debugging
	private static final Logger logger = Logger.getLogger(RankUp.class);
    private int errorCorrectorIterations;

    // MySQL AbstractManager and DatabaseManager
    private final AbstractManager abstractManager;

    // POSTagger and Lemmatizer
    private final POSTagger posTagger;
    private final Lemmatizer lemmatizer;

    // Stopwords
    private final Stopwords stopwords;

    // KeyPhrases
    private KeyPhraseGraph keyPhraseGraph;
    private List<KeyPhrase> originalKeyphraseList;

    // RankUp Properties
    private final RankUpProperties rankUpProperties;

    // Training Abstracts
    private final Collection<Abstract> trainingAbstracts;
    
    // Changes and bug fixes
    private final boolean MINMAX_MID_BUG_FIX;
    private final boolean CORRECT_NEGATIVE_WEIGHTS;
    private final boolean DENORMALIZE_MODIFICATION_VALUE;
    private final boolean USE_DIFFERENTIAL_CONVERGENCE;
    
    // RAKE
    private final Rake rake;

    /**
     * RankUp Constructor, initialized with all its necessary components.
     * @param abstractManager
     * @param posTagger
     * @param lemmatizer
     * @param stopwords
     * @param rake
     * @param rankUpProperties
     * @param trainingAbstracts
     * @param minMaxMidBugFix
     * @param correctNegativeWeights
     * @param denormalizeModificationValue
     * @param useDifferentialConvergence
     * @throws Exception
     */
    public RankUp(
            AbstractManager abstractManager,
            POSTagger posTagger, 
            Lemmatizer lemmatizer, 
            Stopwords stopwords,
            Rake rake,
            RankUpProperties rankUpProperties, 
            Collection<Abstract> trainingAbstracts,
            boolean minMaxMidBugFix, 
            boolean correctNegativeWeights,
            boolean denormalizeModificationValue,
            boolean useDifferentialConvergence)
            throws Exception {

        //this.textRank = textRank;
        this.abstractManager = abstractManager;
        this.posTagger = posTagger;
        this.lemmatizer = lemmatizer;
        this.stopwords = stopwords;
        this.rake = rake;
        this.keyPhraseGraph = null;
        this.rankUpProperties = rankUpProperties;
        this.trainingAbstracts = trainingAbstracts;
        this.MINMAX_MID_BUG_FIX = minMaxMidBugFix;
        this.CORRECT_NEGATIVE_WEIGHTS = correctNegativeWeights;
        this.DENORMALIZE_MODIFICATION_VALUE = denormalizeModificationValue;
        this.USE_DIFFERENTIAL_CONVERGENCE = useDifferentialConvergence;
    }

    /**
     * Set the features for all keyphrases extracted by the base keyphrase extraction
     * algorithm.
     * @param keyPhrases
     * @param abs
     * @param force
     * @throws Exception
     */
    public void setKeyPhraseFeatures(List<KeyPhrase> keyPhrases, Abstract abs, boolean force)
            throws Exception {

        // Check if the phrase features have been set or not
        if (!abs.phraseFeaturesSet() || force) {
            
            // First, get RAKE keywords
            HashMap<String, Double> rakeKeyphrases = null;
            if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.TEXTRANK) {
                rakeKeyphrases = rake.runRake(abs.getOriginalText());
            }
            
            // Then, calculate individual keyphrase features
            for (KeyPhrase keyPhrase : keyPhrases) {
                PhraseFeatures.setPhraseFeatures(
                        keyPhrase, abs, trainingAbstracts, abstractManager, posTagger,
                        lemmatizer, rakeKeyphrases);
            }
            
            abs.setPhraseFeatures();
        }
    }

    /**
     * Remove stop phrases and any subphrases that have lower ranks than their superphrases.
     * @param oldKeyphrases
     * @return
     * @throws Exception
     */
    private ArrayList postProcessKeyphrases(List<KeyPhrase> oldKeyphrases) throws Exception {

        ArrayList<KeyPhrase> newKeyphrases = new ArrayList<>();

        for (KeyPhrase oldKeyphrase : oldKeyphrases) {
            String originalOldKeyphrase = oldKeyphrase.text;
            String stemmedOldKeyphrase = oldKeyphrase.getFeatures() != null ?
                    oldKeyphrase.getFeatures().stemmedPhrase :
                    lemmatizer.stemText(oldKeyphrase.text, false);
            boolean hasSuperPhrase = false;
            for (KeyPhrase newKeyphrase : newKeyphrases) {
                String originalNewKeyphrase = newKeyphrase.text;
                String stemmedNewKeyphrase = newKeyphrase.getFeatures() != null ?
                        newKeyphrase.getFeatures().stemmedPhrase : 
                        lemmatizer.stemText(newKeyphrase.text, false);

                if (originalNewKeyphrase.contains(originalOldKeyphrase) ||
                        originalNewKeyphrase.contains(stemmedOldKeyphrase) ||
                        stemmedNewKeyphrase.contains(originalOldKeyphrase) ||
                        stemmedNewKeyphrase.contains(stemmedOldKeyphrase)) {
                    hasSuperPhrase = true;
                    break;
                }
                // Old version
//                if (newKeyphrase.text.contains(oldKeyphrase.text)) {
//                    hasSuperPhrase = true;
//                    break;
//                }
            }
            //Remove subphrases and stop phrases
            if (!hasSuperPhrase && !stopwords.isStopPhrase(oldKeyphrase.text)) {
                if (oldKeyphrase.getFeatures() != null) {
                    if (!stopwords.isStopPhrase(
                            oldKeyphrase.getFeatures().stemmedPhrase)) {
                        newKeyphrases.add(oldKeyphrase);
                    }
                }
                else {
                    newKeyphrases.add(oldKeyphrase);
                }
            }
        }

        return newKeyphrases;
    }

    /**
     * Main RankUp algorithm.
     * @param abs
     * @param printGephiGraphs
     * @param keywordExtractor
     * @return
     */
    public List<KeyPhrase> runRankUp(
            Abstract abs,
            //boolean newMethod, 
            boolean printGephiGraphs,
            GraphBasedKeywordExtractor keywordExtractor) {

        try {
            // 1. Run Keyword Extractor (TextRank, RAKE, etc.) 
            // (3.1 Construct Graph and 3.2 Ranking Nodes)
            logger.info("1. Extracting " + keywordExtractor.keywordExtractionMethod + 
                    " keyphrases...");
            List<KeyPhrase> originalKeyphraseListFull = keywordExtractor.extractKeyphrases(abs);

            // Get Keyphrases to use in ErrorDetector and ErrorCorrector
            ArrayList<KeyPhrase> feedbackKeyphraseList;
            ArrayList<KeyPhrase> realKeyphraseList;
            // In TextRank, use all keyphrases in the graph
            if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.TEXTRANK) {
                feedbackKeyphraseList = (ArrayList<KeyPhrase>) originalKeyphraseListFull;
                realKeyphraseList = feedbackKeyphraseList;
            }
            // In RAKE, only use Word Nodes
            else if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.RAKE) {
                    feedbackKeyphraseList = new ArrayList<>();
                    realKeyphraseList = new ArrayList<>();
                for (KeyPhrase keyphrase : originalKeyphraseListFull) {
                    RakeNode rakeNode = (RakeNode) keyphrase.getNode();
                    if (rakeNode.type == RakeNodeType.WORD) {
                        feedbackKeyphraseList.add(keyphrase);
                    }
                    else if (rakeNode.type == RakeNodeType.KEYWORD) {
                        realKeyphraseList.add(keyphrase);
                    }
                }
            }
            else {
                feedbackKeyphraseList = null;
                realKeyphraseList = null;
            }
            originalKeyphraseList = realKeyphraseList;
            
            // 2. Set keyphrase features
            logger.info("2. Setting keyphrase features...");
            setKeyPhraseFeatures(feedbackKeyphraseList, abs, false);
            
            // 3. Assign keyphrase sets
            logger.info("3. Assigning keyphrase sets...");
            keyPhraseGraph = new KeyPhraseGraph(
                    feedbackKeyphraseList,
                    rankUpProperties.setAssignmentApproach,
                    rankUpProperties.featureLowerBound,
                    rankUpProperties.featureUpperBound,
                    PhraseFeatures.getFeatureFromErrorDetectingApproach(
                            rankUpProperties.errorDetectingApproach));

            // 4. Assign expected scores (3.3 Detect Errors)
            logger.info("4. Assigning expected scores...");
            ErrorDetector.assignExpectedScores(keyPhraseGraph,
                    rankUpProperties.expectedScoreValue, MINMAX_MID_BUG_FIX);

            // Print Feature Sets
            PhraseFeatures.Feature feature = 
                    PhraseFeatures.getFeatureFromErrorDetectingApproach(
                            rankUpProperties.errorDetectingApproach);
            logger.debug("**** Initial " + feature + " Sets ****");
            keyPhraseGraph.printFeatureSet(SetLevel.LOW);
            keyPhraseGraph.printFeatureSet(SetLevel.MID);
            keyPhraseGraph.printFeatureSet(SetLevel.HIGH);
            
            // 5. Perform error Feedback (3.4 Error Feedback)
            logger.info("5. Performing error feedback...");
            if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.TEXTRANK) {
                errorCorrectorIterations = TextRankErrorCorrector.performErrorFeedback(
                        abs,
                        keyPhraseGraph, 
                        keywordExtractor.textRank,
                        rankUpProperties.useWholeTextRankGraph, 
                        rankUpProperties.learningRate,
                        rankUpProperties.standardErrorThreshold,
                        rankUpProperties.convergenceScheme, 
                        rankUpProperties.convergenceRule,
                        rankUpProperties.revertGraphs,
                        printGephiGraphs,
                        CORRECT_NEGATIVE_WEIGHTS,
                        DENORMALIZE_MODIFICATION_VALUE,
                        USE_DIFFERENTIAL_CONVERGENCE);
            }
            else if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.RAKE){
                errorCorrectorIterations = RakeErrorCorrector.performErrorFeedback(
                        abs,
                        realKeyphraseList,
                        keyPhraseGraph, 
                        keywordExtractor.rake,
                        rankUpProperties.learningRate,
                        rankUpProperties.standardErrorThreshold,
                        rankUpProperties.convergenceScheme, 
                        rankUpProperties.convergenceRule,
                        rankUpProperties.revertGraphs,
                        printGephiGraphs,
                        CORRECT_NEGATIVE_WEIGHTS);
            }
            
            // Sort keyphrases
            List<KeyPhrase> rankUpKeyphrases = null;
            if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.TEXTRANK) {
                rankUpKeyphrases = keyPhraseGraph.getSortedKeyphrases();
            }
            else if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.RAKE) {
                rankUpKeyphrases = getSortedKeyphrases(realKeyphraseList);
            }
            
            logger.debug("**** Final " + feature + " Sets ****");
            keyPhraseGraph.printFeatureSet(SetLevel.LOW);
            keyPhraseGraph.printFeatureSet(SetLevel.MID);
            keyPhraseGraph.printFeatureSet(SetLevel.HIGH);

            // 6. Apply postprocessing 
            if (rankUpProperties.postprocess) {
                logger.info("6. Applying Postprocessing...");
                rankUpKeyphrases =
                        postProcessKeyphrases(rankUpKeyphrases);  // Full post-processing
            }

            return rankUpKeyphrases;
        }
        catch (Exception e) {
            logger.error("Exception in runHybridRank: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get the original set of keyphrases extracted by the base extraction algorithm.
     * @return
     */
    public List<KeyPhrase> getOriginalKeyphraseSet() {
        
        Comparator comparator =
                new Comparator<KeyPhrase>() {
                    @Override
                    public int compare(KeyPhrase a, KeyPhrase b) {
                        Double scoreA = (!Double.isNaN(a.getOriginalTextRankScore()) ? 
                            a.getOriginalTextRankScore() : 0);
                        Double scoreB = (!Double.isNaN(b.getOriginalTextRankScore()) ? 
                            b.getOriginalTextRankScore() : 0);
                        return scoreB.compareTo(scoreA);
                    }
                };
        
        HashMap<String, KeyPhrase> uniqueNewKeyphrases = new HashMap<>();
        for (int i = originalKeyphraseList.size() - 1; i >= 0; i--) {
            // Only add nodes with Metric score
            if (originalKeyphraseList.get(i).getOriginalTextRankScore() == -1.0) {
                continue;
            }
            
            uniqueNewKeyphrases.put(originalKeyphraseList.get(i).text, 
                    originalKeyphraseList.get(i));
        }

        ArrayList<KeyPhrase> sortedUniqueNewKeyphrases = 
                new ArrayList<>(uniqueNewKeyphrases.values());
        Collections.sort(sortedUniqueNewKeyphrases, comparator);
        
        return sortedUniqueNewKeyphrases;
    }
    
    /**
     * Get the sorted list of keyphrases extracted by RankUp
     * @param keyphrases
     * @return
     */
    public List<KeyPhrase> getSortedKeyphrases(List<KeyPhrase> keyphrases) {

        List<KeyPhrase> sortedKeyphrases = keyphrases;
        
        Collections.sort(sortedKeyphrases,
            new Comparator<KeyPhrase>() {

                @Override
                public int compare(KeyPhrase n1, KeyPhrase n2) {
                    Double n1FinalTextRankScore = n1.getFinalTextRankScore();
                    Double n2FinalTextRankScore = n2.getFinalTextRankScore();
                    return n2FinalTextRankScore.compareTo(n1FinalTextRankScore);
                }
            });

        return sortedKeyphrases;
    }
    
    public KeyPhraseGraph getKeyPhraseGraph() {
        return keyPhraseGraph;
    }
    
    public RankUpProperties getRankUpProperties() {
        return rankUpProperties;
    }
    
    public int getRankUpIterations() {
        return errorCorrectorIterations;
    }
}
