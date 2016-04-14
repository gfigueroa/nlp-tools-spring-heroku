package com.figueroa.nlp.rankup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.KeywordExtractionMethod;
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
import com.figueroa.util.ExceptionLogger;
import com.figueroa.util.ExceptionLogger.DebugLevel;

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
    private final ExceptionLogger logger;
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

    // Constructor
    public RankUp(ExceptionLogger logger,
            //TextRank textRank, 
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

        this.logger = logger;
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

    public void setKeyPhraseFeatures(List<KeyPhrase> keyPhrases, Abstract abs, boolean force)
            throws Exception {

        // Check if the phrase features have been set or not
        if (!abs.phraseFeaturesSet() || force) {
            
            // First, get RAKE keywords
            HashMap<String, Double> rakeKeyphrases = null;
            if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.TEXTRANK) {
                rakeKeyphrases = rake.runRake(abs.getOriginalText());
            }
            
            // Then, calculate individual keyphrase features
            for (KeyPhrase keyPhrase : keyPhrases) {
                PhraseFeatures.setPhraseFeatures(
                        keyPhrase, abs, trainingAbstracts, abstractManager, posTagger,
                        lemmatizer, rakeKeyphrases, logger);
            }
            
            abs.setPhraseFeatures();
        }
    }

    // Remove stop phrases and any subphrases that have lower ranks than their superphrases
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

    // Main entry point of RankUp
    public List<KeyPhrase> runRankUp(
            Abstract abs,
            //boolean newMethod, 
            boolean printGephiGraphs,
            GraphBasedKeywordExtractor keywordExtractor) {

        try {
            // 1. Run Keyword Extractor (TextRank, RAKE, etc.) 
            // (3.1 Construct Graph and 3.2 Ranking Nodes)
            logger.debug("1. Extracting " + keywordExtractor.keywordExtractionMethod + 
                    " keyphrases...", DebugLevel.INFO);
            List<KeyPhrase> originalKeyphraseListFull = keywordExtractor.extractKeyphrases(abs);

            // Get Keyphrases to use in ErrorDetector and ErrorCorrector
            ArrayList<KeyPhrase> feedbackKeyphraseList;
            ArrayList<KeyPhrase> realKeyphraseList;
            // In TextRank, use all keyphrases in the graph
            if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.TEXTRANK) {
                feedbackKeyphraseList = (ArrayList<KeyPhrase>) originalKeyphraseListFull;
                realKeyphraseList = feedbackKeyphraseList;
            }
            // In RAKE, only use Word Nodes
            else if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.RAKE) {
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
            logger.debug("2. Setting keyphrase features...", DebugLevel.INFO);
            setKeyPhraseFeatures(feedbackKeyphraseList, abs, false);
            
            // 3. Assign keyphrase sets
            logger.debug("3. Assigning keyphrase sets...", DebugLevel.INFO);
            keyPhraseGraph = new KeyPhraseGraph(
                    logger, 
                    feedbackKeyphraseList,
                    rankUpProperties.setAssignmentApproach,
                    rankUpProperties.featureLowerBound,
                    rankUpProperties.featureUpperBound,
                    PhraseFeatures.getFeatureFromErrorDetectingApproach(
                            rankUpProperties.errorDetectingApproach));

            // 4. Assign expected scores (3.3 Detect Errors)
            logger.debug("4. Assigning expected scores...", DebugLevel.INFO);
            ErrorDetector.assignExpectedScores(keyPhraseGraph,
                    rankUpProperties.expectedScoreValue, MINMAX_MID_BUG_FIX);

            // Print Feature Sets
            PhraseFeatures.Feature feature = 
                    PhraseFeatures.getFeatureFromErrorDetectingApproach(
                            rankUpProperties.errorDetectingApproach);
            logger.debug("**** Initial " + feature + " Sets ****", DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.LOW, DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.MID, DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.HIGH, DebugLevel.DEBUG);
            
            // 5. Perform error Feedback (3.4 Error Feedback)
            logger.debug("5. Performing error feedback...", DebugLevel.INFO);
            if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.TEXTRANK) {
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
            else if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.RAKE){
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
            if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.TEXTRANK) {
                rankUpKeyphrases = keyPhraseGraph.getSortedKeyphrases();
            }
            else if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.RAKE) {
                rankUpKeyphrases = getSortedKeyphrases(realKeyphraseList);
            }
            
            logger.debug("**** Final " + feature + " Sets ****", DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.LOW, DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.MID, DebugLevel.DEBUG);
            keyPhraseGraph.printFeatureSet(SetLevel.HIGH, DebugLevel.DEBUG);

            // 6. Apply postprocessing 
            if (rankUpProperties.postprocess) {
                logger.debug("6. Applying Postprocessing...", DebugLevel.INFO);
                rankUpKeyphrases =
                        postProcessKeyphrases(rankUpKeyphrases);  // Full post-processing
            }

            return rankUpKeyphrases;
        }
        catch (Exception e) {
            logger.debug("Exception in runHybridRank: " + e.getMessage(),
                    DebugLevel.ERROR);
            e.printStackTrace();
            return null;
        }
    }
    
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
