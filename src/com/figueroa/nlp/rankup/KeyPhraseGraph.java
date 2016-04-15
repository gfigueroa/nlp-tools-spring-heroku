package com.figueroa.nlp.rankup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;

import com.figueroa.nlp.rankup.PhraseFeatures.Feature;
import com.figueroa.nlp.rake.RakeNode;
import com.figueroa.nlp.rake.RakeNode.RakeNodeType;
import com.figueroa.nlp.textrank.KeyWord;
import com.figueroa.nlp.textrank.NGram;
import com.figueroa.nlp.textrank.TextRankNode;
import com.figueroa.util.AbstractManager;
import com.figueroa.util.MiscUtils;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Node;

/**
 * The KeyPhraseSet contains all the keyphrases extracted for RankUp. It contains
 * several important statistics and features of the set.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class KeyPhraseGraph extends TreeMap<String, KeyPhrase> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AbstractManager.class);
    private final List<KeyPhrase> keyphrases;

    // Solution Expector
    private final Feature feature;
    private final double featureLowerBound;
    private final double featureUpperBound;
    public final static double ORIGINAL_SCORE_LOWER_BOUND = 0.5;
    public final static double ORIGINAL_SCORE_UPPER_BOUND = 0.5;

    // Sets
    private final ArrayList<KeyPhrase> lowOriginalScoreSet;
    private final ArrayList<KeyPhrase> midOriginalScoreSet;
    private final ArrayList<KeyPhrase> highOriginalScoreSet;
    private final ArrayList<KeyPhrase> lowFeatureSet;
    private final ArrayList<KeyPhrase> midFeatureSet;
    private final ArrayList<KeyPhrase> highFeatureSet;
    

    public static enum SetLevel {
        LOW, MID, HIGH
    }
    
    public static enum SetAssignmentApproach {
        MEAN, IQR
    }

    public static SetAssignmentApproach getSetAssignmentApproachFromString(
            String setAssignmentApproachString) {

        if (setAssignmentApproachString == null) {
            return null;
        }
        else if (setAssignmentApproachString.equalsIgnoreCase("mean")) {
            return SetAssignmentApproach.MEAN;
        }
        else if (setAssignmentApproachString.equalsIgnoreCase("iqr")) {
            return SetAssignmentApproach.IQR;
        }
        else {
            return null;
        }
    }

    public KeyPhraseGraph(
            List<KeyPhrase> keyphrases,
            SetAssignmentApproach setAssignmentApproach,
            double featureLowerBound, 
            double featureUpperBound,
            Feature feature) {

        this.keyphrases = keyphrases;
        
        this.feature = feature;
        this.featureLowerBound = featureLowerBound;
        this.featureUpperBound = featureUpperBound;

        for (KeyPhrase keyphrase : keyphrases) {
            String key = keyphrase.getNode() != null ? 
                    keyphrase.getNode().key :
                    Integer.toString(keyphrase.hashCode());
            this.put(key, keyphrase);
        }

        this.lowOriginalScoreSet = new ArrayList<>();
        this.midOriginalScoreSet = new ArrayList<>();
        this.highOriginalScoreSet = new ArrayList<>();

        this.lowFeatureSet = new ArrayList<>();
        this.midFeatureSet = new ArrayList<>();
        this.highFeatureSet = new ArrayList<>();

        assignOriginalScoreSets();
        assignFeatureSets(setAssignmentApproach);
    }

    public List<KeyPhrase> getAllKeyphrases() {
        return keyphrases;
    }

    public ArrayList<KeyPhrase> getNGramKeyphrases() {
        ArrayList<KeyPhrase> nGramKeyPhrases = new ArrayList<>();

        for (KeyPhrase keyPhrase : keyphrases) {
            if (keyPhrase.getNode() instanceof TextRankNode) {
                TextRankNode node = (TextRankNode) keyPhrase.getNode();
                
                if (node.value instanceof NGram) {
                    nGramKeyPhrases.add(keyPhrase);
                }
            }
            else if (keyPhrase.getNode() instanceof RakeNode) {
                RakeNode node = (RakeNode) keyPhrase.getNode();
                
                if (node.type == RakeNodeType.KEYWORD) {
                    nGramKeyPhrases.add(keyPhrase);
                }
            }
        }

        return nGramKeyPhrases;
    }

    public List<KeyPhrase> getSortedKeyphrases() {

        List<KeyPhrase> sortedKeyphrases;

        sortedKeyphrases = getNGramKeyphrases();
        
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

    private void assignOriginalScoreSets() {

        double originalScoreMean = getOriginalScoreMean();
        double originalScoreStandardDeviation = getOriginalScoreStandardDeviation();

        for (KeyPhrase keyphrase : keyphrases) {
            double originalScore = keyphrase.getScore();
            // Assign to Low TextRank set
            if (originalScore
                    < originalScoreMean - (ORIGINAL_SCORE_LOWER_BOUND * originalScoreStandardDeviation)) {
                lowOriginalScoreSet.add(keyphrase);
            }
            // Assign to High TextRank set
            else if (originalScore
                    > originalScoreMean + (ORIGINAL_SCORE_UPPER_BOUND * originalScoreStandardDeviation)) {
                highOriginalScoreSet.add(keyphrase);
            }
            // Assign to Mid TextRank set
            else {
                midOriginalScoreSet.add(keyphrase);
            }
        }
    }

    private void assignFeatureSets(SetAssignmentApproach setAssignmentApproach) {

        for (KeyPhrase keyphrase : keyphrases) {

            double featureValue = keyphrase.getFeatures().getFeatureValue(feature, keyphrase);

            double lowSetThreshold = 0;
            double highSetThreshold = 0;
            switch (setAssignmentApproach) {
                case MEAN:
                    double featureMean = getFeatureMean();
                    double featureStandardDeviation = getFeatureStandardDeviation();
                    lowSetThreshold = featureMean -
                            (featureLowerBound * featureStandardDeviation);
                    highSetThreshold = featureMean +
                            (featureUpperBound * featureStandardDeviation);
                    break;
                case IQR:
                    double featureQ3 = getFeatureQ3(feature);
                    double featureQ1 = getFeatureQ1(feature);
                    double featureIQR = featureQ3 - featureQ1;
                    lowSetThreshold = featureQ1 - (featureIQR * 1.5);
                    highSetThreshold = featureQ3 + (featureIQR * 1.5);
                    break;
            }

            // Assign to Low  set
            if (featureValue < lowSetThreshold) {
                lowFeatureSet.add(keyphrase);
            }
            // Assign to High set
            else if (featureValue > highSetThreshold) {
                highFeatureSet.add(keyphrase);
            }
            // Assign to Mid set
            else {
                midFeatureSet.add(keyphrase);
            }
        }
    }
    
    public ArrayList<KeyPhrase> getOriginalScoreSet(SetLevel setLevel) {
        switch (setLevel) {
            case LOW:
                return lowOriginalScoreSet;
            case MID:
                return midOriginalScoreSet;
            case HIGH:
                return highOriginalScoreSet;
            default:
                return null;
        }
    }

    public ArrayList<KeyPhrase> getFeatureSet(SetLevel setLevel) {

        switch (setLevel) {
            case LOW:
                    return lowFeatureSet;
            case MID:
                    return midFeatureSet;
            case HIGH:
                    return highFeatureSet;
            default:
                return null;
        }
    }

    public double getFeatureSetMin(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double min = Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double featureValue = keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
            if (featureValue <= min) {
                min = featureValue;
            }
        }

        return min;
    }

    public double getFeatureSetMax(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double max = -Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double featureValue = keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
            if (featureValue >= max) {
                max = featureValue;
            }
        }

        return max;
    }
    
    public double getFeatureSetMean(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double mean = 0;

        for (KeyPhrase keyPhrase : set) {
            mean += keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
        }

        mean /= set.size();
        return mean;
    }

    public double getFeatureSetMinTextRankScore(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double min = Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double score = keyPhrase.getScore();
            if (score<= min) {
                min = score;
            }
        }

        return min;
    }

    public double getFeatureSetMaxTextRankScore(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double max = -Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double score = keyPhrase.getScore();
            if (score >= max) {
                max = score;
            }
        }

        return max;
    }

    public double getFeatureSetMeanTextRankScore(SetLevel setLevel) {

        ArrayList<KeyPhrase> set = getFeatureSet(setLevel);
        double mean = 0;

        for (KeyPhrase keyPhrase : set) {
            mean += keyPhrase.getScore();
        }

        mean /= set.size();
        return mean;
    }

    public double getTextRankScoreSetMin(SetLevel setLevel) {
        ArrayList<KeyPhrase> set = getOriginalScoreSet(setLevel);
        double min = Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double score = keyPhrase.getScore();
            if (score <= min) {
                min = score;
            }
        }

        return min;
    }

    public double getTextRankScoreSetMax(SetLevel setLevel) {
        ArrayList<KeyPhrase> set = getOriginalScoreSet(setLevel);
        double max = -Double.MAX_VALUE;

        for (KeyPhrase keyPhrase : set) {
            double score = keyPhrase.getScore();
            if (score >= max) {
                max = score;
            }
        }

        return max;
    }

    public double getTextRankScoreSetMean(SetLevel setLevel) {
        ArrayList<KeyPhrase> set = getOriginalScoreSet(setLevel);
        double mean = 0.0;

        for (KeyPhrase keyPhrase : set) {
            mean += keyPhrase.getScore();
        }

        mean /= set.size();
        return mean;
    }

    public double getOriginalScoreMean() {

        double mean = 0;

        for (KeyPhrase keyphrase : keyphrases) {
            mean += keyphrase.getScore();
        }

        mean /= keyphrases.size();
        return mean;
    }

    public double getTextRankScoreVariance() {

        double variance = 0;
        double mean = getOriginalScoreMean();

        for (KeyPhrase keyphrase : keyphrases) {
            variance += Math.pow(keyphrase.getScore() - mean, 2);
        }

        variance /= (keyphrases.size() - 1);
        return variance;
    }

    public double getOriginalScoreStandardDeviation() {

        double variance = getTextRankScoreVariance();

        double standardDeviation = Math.sqrt(variance);
        return standardDeviation;
    }

    public double getFeatureMean() {

        double mean = 0;

        for (KeyPhrase keyPhrase : keyphrases) {
            double featureValue = keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
            mean += featureValue;
        }

        mean /= keyphrases.size();
        return mean;
    }

    public double getFeatureVariance() {

        double variance = 0;
        double mean = getFeatureMean();

        for (KeyPhrase keyPhrase : keyphrases) {

            double featureValue = keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
            variance += Math.pow(featureValue - mean, 2);
        }

        variance /= (keyphrases.size() - 1);
        return variance;
    }

    public double getFeatureStandardDeviation() {

        double variance = getFeatureVariance();

        double standardDeviation = Math.sqrt(variance);
        return standardDeviation;
    }

    public double getFeatureQ1(Feature feature) {
        List<KeyPhrase> keyPhrases = getFeatureSortedKeyPhrases(true);
        int size = keyPhrases.size();
        int q1Position = (int) MathUtils.round((size / 4.0), 0);

        KeyPhrase keyPhrase = keyPhrases.get(q1Position);
        return keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
    }

    public double getFeatureQ3(Feature feature) {
        List<KeyPhrase> keyPhrases = getFeatureSortedKeyPhrases(true);
        int size = keyPhrases.size();
        int q3Position = (int) MathUtils.round((size / 4.0), 0) * 3;
        
        if (q3Position >= keyPhrases.size()) {
            q3Position = keyPhrases.size() - 1;
        }

        KeyPhrase keyPhrase = keyPhrases.get(q3Position);
        return keyPhrase.getFeatures().getFeatureValue(feature, keyPhrase);
    }

    public List<KeyPhrase> getFeatureSortedKeyPhrases(boolean ascending) {
        List<KeyPhrase> sortedKeyPhrases = new ArrayList<>(this.values());

        Comparator comparator =
                new Comparator<KeyPhrase>() {
                    public int compare(KeyPhrase a, KeyPhrase b) {
                        Double featureA = a.getFeatures().getFeatureValue(feature, a);
                        Double featureB = b.getFeatures().getFeatureValue(feature, b);
                        return featureB.compareTo(featureA);
                    }
                };
        
        Collections.sort(sortedKeyPhrases, comparator);

        return sortedKeyPhrases;
    }

    public void printStatistics() {
        logger.debug("**** STATISTICS ****");
        logger.debug("Score Mean: " + MathUtils.round(getOriginalScoreMean(), 2));
        logger.debug("Score Standard Deviation: " + 
                MathUtils.round(getOriginalScoreStandardDeviation(), 2));
        logger.debug(feature + " Mean: "
                + MathUtils.round(getFeatureMean(), 2));
        logger.debug(feature + " Variance: "
                + MathUtils.round(getFeatureVariance(), 2));
        logger.debug(feature + " Standard Deviation: "
                + MathUtils.round(getFeatureStandardDeviation(), 2));
        logger.debug("");
    }

    /*
    Keyphrase		& TFIDF 	& RAKE score 	& Exp. score	& Direction	\\
    considered		& 0.04		& 1.0		& -		& -  		\\
    */
    public void printFeatureSetLatex(SetLevel setLevel) {
        logger.debug(setLevel + " " + feature +
                " (" +
                "MIN " + feature + ": " +
                MathUtils.round(getFeatureSetMin(setLevel), 2) +
                ", MAX " + feature + ": " +
                MathUtils.round(getFeatureSetMax(setLevel), 2) +
                ", MEAN " + feature + ": " +
                MathUtils.round(getFeatureSetMean(setLevel), 2) +
                ", MIN S: " +
                MathUtils.round(getFeatureSetMinTextRankScore(setLevel), 2) +
                ", MAX S: " +
                MathUtils.round(getFeatureSetMaxTextRankScore(setLevel), 2) +
                ", MEAN S: " +
                MathUtils.round(getFeatureSetMeanTextRankScore(setLevel), 2) +
                ")");
        for (KeyPhrase keyphrase : getFeatureSet(setLevel)) {
            logger.debug(
                    keyphrase.text + 
                    "\t& " + 
                    MiscUtils.convertDoubleToFixedCharacterString(keyphrase.getFeatures().tfidfStemmed, 2) +
                    "\t& " + 
                    MiscUtils.convertDoubleToFixedCharacterString(keyphrase.getOriginalTextRankScore(), 2) + 
                    "\t& " +
                    MiscUtils.convertDoubleToFixedCharacterString(keyphrase.getExpectedScore(), 2) +
                    "\t& " +
                    keyphrase.getExpectedScoreDirection() +
                    "\\\\");
        }
        logger.debug("");
    }
    
    public void printFeatureSet(SetLevel setLevel) {
        logger.debug(setLevel + " " + feature +
                " (" +
                "MIN " + feature + ": " +
                MathUtils.round(getFeatureSetMin(setLevel), 2) +
                ", MAX " + feature + ": " +
                MathUtils.round(getFeatureSetMax(setLevel), 2) +
                ", MEAN " + feature + ": " +
                MathUtils.round(getFeatureSetMean(setLevel), 2) +
                ", MIN S: " +
                MathUtils.round(getFeatureSetMinTextRankScore(setLevel), 2) +
                ", MAX S: " +
                MathUtils.round(getFeatureSetMaxTextRankScore(setLevel), 2) +
                ", MEAN S: " +
                MathUtils.round(getFeatureSetMeanTextRankScore(setLevel), 2) +
                ")");
        for (KeyPhrase keyphrase : getFeatureSet(setLevel)) {
            logger.debug(keyphrase.toString() + "\t" + keyphrase.getFeatures().toString());
        }
        logger.debug("");
    }
    
    /**
     * Calculate the overall correctness of the graph's keyphrase final text rank scores. 
     * Correctness means the percentage of keyphrases that were correctly changed 
     * according to the Error Detection Approach (some keyphrase final TextRank
     * scores have to increase, others have to decrease)
     * @return the percentage of keyphrase final TextRank scores that were 
     * changed correctly by RankUp
     */
    public Double getKeyphraseFinalTextRankScoreCorrectness() {
        
        int correct = 0;
        int incorrect = 0;
        for (KeyPhrase keyphrase : keyphrases) {
            if (keyphrase.finalTextRankScoreDirectionIsCorrect() != null) {
                if (keyphrase.finalTextRankScoreDirectionIsCorrect()) {
                    correct++;
                }
                else {
                    incorrect++;
                }
            }
        }
        
        // Check for no scores
        if (correct + incorrect == 0) {
            return null;
        }
        
        return (double) correct / (correct + incorrect);
    }
    
    /**
     * Calculate the overall correctness of the graph's keyphrase scores. 
     * Correctness means the percentage of keyphrases that were correctly changed 
     * according to the Error Detection Approach (some keyphrase scores have to increase, 
     * others have to decrease)
     * @return the percentage of keyphrase scores that were changed correctly by RankUp
     */
    public Double getKeyphraseScoreCorrectness() {
        
        int correct = 0;
        int incorrect = 0;
        for (KeyPhrase keyphrase : keyphrases) {
            if (keyphrase.currentScoreDirectionIsCorrect() != null) {
                if (keyphrase.currentScoreDirectionIsCorrect()) {
                    correct++;
                }
                else {
                    incorrect++;
                }
            }
        }
        
        // Check for no scores
        if (correct + incorrect == 0) {
            return null;
        }
        
        return (double) correct / (correct + incorrect);
    }
    
    /**
     * Calculate the overall correctness of the graph's TextRank node scores. 
     * Correctness means the percentage of keyphrases that were correctly changed 
     * according to the Error Detection Approach (some TextRank node scores have to increase, 
     * others have to decrease)
     * @return the percentage of TextRank node scores that were changed correctly by RankUp
     */
    public Double getTextRankNodeScoreCorrectness() {
        
        int correct = 0;
        int incorrect = 0;
        for (KeyPhrase keyphrase : keyphrases) {
            if (keyphrase.textRankNodeScoreDirectionIsCorrect() != null) {
                if (keyphrase.textRankNodeScoreDirectionIsCorrect()) {
                    correct++;
                }
                else {
                    incorrect++;
                }
            }
        }
        
        // Check for no scores
        if (correct + incorrect == 0) {
            return null;
        }
        
        return (double) correct / (correct + incorrect);
    }
    
    public void printGraph(boolean onlyPrintNGramEdges) {
        logger.trace("");
        logger.trace("*** Keyphrase Graph ***");
        for (KeyPhrase keyphrase : keyphrases) {
            
//            // For debugging only
//            if (keyphrase.getExpectedScore() < 0 && keyphrase.getNode().d_j == 0) {
//                continue;
//            }
            
            String keyphraseString = keyphrase.toString();
            Node node = keyphrase.getNode();
            HashMap<Node, Double> edges = node.getEdges();

            logger.trace(keyphraseString);
            logger.trace("\tEdges: ");
            for (Node en : edges.keySet()) {
                if (en instanceof TextRankNode) {
                    TextRankNode edgeNode = (TextRankNode) en;
                    if (!onlyPrintNGramEdges || 
                            (onlyPrintNGramEdges && edgeNode.value instanceof KeyWord)) {

                        String edgeText = edgeNode.value.text;
    //                    if (edgeNode.value instanceof SynsetLink) {
    //                        SynsetLink edgeNodeValue = (SynsetLink) edgeNode.value;
    //                        edgeText += " [relation: " + edgeNodeValue.relation + ", " +
    //                                "parent: " + edgeNodeValue.parent.value.text + "]";
    //                    }

                        String edgeInformation = "\t\t" + edgeText + 
                                " (" + edgeNode.value.getClass().getSimpleName() + ") - " + 
                                MathUtils.round(edges.get(edgeNode), 2);
                        logger.trace(edgeInformation);
                    }
                }
                else if (en instanceof RakeNode) {
                    RakeNode edgeNode = (RakeNode) en;
                    String edgeText = edgeNode.text;
                    String edgeInformation = "\t\t" + edgeText + 
                            " (" + edgeNode.getClass().getSimpleName() + ") - " + 
                            MathUtils.round(edges.get(edgeNode), 2);
                    logger.trace(edgeInformation);
                }
            }
        }
        Double keyphraseFinalTextRankScoreCorrectness = 
                getKeyphraseFinalTextRankScoreCorrectness();
        Double keyphraseScoreCorrectness = getKeyphraseScoreCorrectness();
        Double textRankNodeScoreCorrectness = getTextRankNodeScoreCorrectness();
        logger.trace("");
        logger.trace("Keyphrase final TextRank score correctness: " + 
                MathUtils.round(keyphraseFinalTextRankScoreCorrectness, 2));
        logger.trace("Keyphrase score correctness: " + 
                MathUtils.round(keyphraseScoreCorrectness, 2));
        logger.trace("TextRank node score correctness: " + 
                MathUtils.round(textRankNodeScoreCorrectness, 2));
        logger.trace("");
    }
}
