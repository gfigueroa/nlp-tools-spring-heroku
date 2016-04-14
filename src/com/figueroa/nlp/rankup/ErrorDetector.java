package com.figueroa.nlp.rankup;

import java.util.ArrayList;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetLevel;
import com.figueroa.nlp.KeyPhrase;

/**
 * Static class.
 * The ErrorDetector detects errors in keyphrase scores and assigns expected scores.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2013
 */
public class ErrorDetector {

    // Property
    public static enum ErrorDetectingApproach {
        STOPWORDS, TFIDF, RIDF, CLUSTEREDNESS, RAKE
    }

    // Property
    public static enum ExpectedScoreValue {
        MINMAX_MID, MINMAX, AVERAGE
    }
    
    /**
     * Get an ErrorDetectingApproach enum type from a string
     * @param approachString
     * @return an ErrorDetectingApproach
     */
    public static ErrorDetectingApproach getErrorDetectingApproachFromString(
            String approachString) {
        
        if (approachString.equalsIgnoreCase("STOPWORDS")) {
            return ErrorDetectingApproach.STOPWORDS;
        }
        else if (approachString.equalsIgnoreCase("TFIDF")) {
            return ErrorDetectingApproach.TFIDF;
        }
        else if (approachString.equalsIgnoreCase("RIDF")) {
            return ErrorDetectingApproach.RIDF;
        }
        else if (approachString.equalsIgnoreCase("CLUSTEREDNESS")) {
            return ErrorDetectingApproach.CLUSTEREDNESS;
        }
        else if (approachString.equalsIgnoreCase("RAKE")) {
            return ErrorDetectingApproach.RAKE;
        }
        else {
            return null;
        }
    }

    /**
     * Get an ExpectedScoreValue enum type from a string
     * @param valueString
     * @return an ExpectedScoreValue
     */
    public static ExpectedScoreValue getExpectedScoreValueFromString(
            String valueString) {

        if (valueString.equalsIgnoreCase("MINMAX_MID")) {
            return ExpectedScoreValue.MINMAX_MID;
        }
        else if (valueString.equalsIgnoreCase("MINMAX")) {
            return ExpectedScoreValue.MINMAX;
        }
        else if (valueString.equalsIgnoreCase("AVERAGE")) {
            return ExpectedScoreValue.AVERAGE;
        }
        else {
            return null;
        }

    }

    /**
     * Assign the expected scores to each node in the graph in 1 of 3 possible
     * ExpectedScoreValue types.
     * @param keyPhraseGraph
     * @param expectedScoreValue: the type of value used for the expected score.
     * In the RankUp paper, the MINMAX approach is used.
     * @param minMaxMidBugFix : a bug fix
     */
    public static void assignExpectedScores(
            KeyPhraseGraph keyPhraseGraph,
            ExpectedScoreValue expectedScoreValue,
            boolean minMaxMidBugFix) {

        double maxExpectedScore = 0;
        double minExpectedScore = 0;

        ArrayList<KeyPhrase> lowSet =
                keyPhraseGraph.getFeatureSet(KeyPhraseGraph.SetLevel.LOW);
        ArrayList<KeyPhrase> highSet =
                keyPhraseGraph.getFeatureSet(KeyPhraseGraph.SetLevel.HIGH);

        // Decide which value to use for expected score
        switch (expectedScoreValue) {
            case MINMAX:
                // Bug fix
                if (minMaxMidBugFix) {
                    
                    // Max expected score (for low set) is lowest score of HIGH
                    maxExpectedScore =
                            keyPhraseGraph.getFeatureSetMinTextRankScore(
                                    SetLevel.HIGH);
                    // Min expected score (for high set) is highest score of LOW
                    minExpectedScore =
                            keyPhraseGraph.getFeatureSetMaxTextRankScore(
                                    SetLevel.LOW);
                }
                else { // old method (never occurs!)
                    // Max expected score (for low set) is highest score of LOW
                    maxExpectedScore =
                            keyPhraseGraph.getFeatureSetMaxTextRankScore(
                                    SetLevel.LOW);
                    // Min expected score (for high set) is lowest score of HIGH
                    minExpectedScore =
                            keyPhraseGraph.getFeatureSetMinTextRankScore(
                                    SetLevel.HIGH);
                }
                break;
            case MINMAX_MID:
                
                // Bug fix (if MID set is empty, use MINMAX approach
                if (minMaxMidBugFix && 
                        keyPhraseGraph.getFeatureSet(SetLevel.MID).isEmpty()) {
                    
                    // Max expected score (for low set) is lowest score of HIGH
                    maxExpectedScore =
                            keyPhraseGraph.getFeatureSetMinTextRankScore(
                                    SetLevel.HIGH);
                    // Min expected score (for high set) is highest score of LOW
                    minExpectedScore =
                            keyPhraseGraph.getFeatureSetMaxTextRankScore(
                                    SetLevel.LOW);
                }
                else {
                    // Max expected score (for low set) is lowest score of MID
                    maxExpectedScore =
                            keyPhraseGraph.getFeatureSetMinTextRankScore(
                                    SetLevel.MID);
                    // Min expected score (for high set) is highest score of MID
                    minExpectedScore =
                            keyPhraseGraph.getFeatureSetMaxTextRankScore(
                                    SetLevel.MID);
                }
                break;
            case AVERAGE:
                // Min and MAx Expected scores are average score of all keyphrases
                maxExpectedScore = keyPhraseGraph.getOriginalScoreMean();
                minExpectedScore = keyPhraseGraph.getOriginalScoreMean();
                break;
        }

        // Change expected scores of keyphrases in LOW set
        for (KeyPhrase keyPhrase : lowSet) {
            if (keyPhrase.getScore() > maxExpectedScore) {
                keyPhrase.setExpectedScore(maxExpectedScore);
                keyPhrase.getNode().setExpectedScore(maxExpectedScore);
            }
        }

        // Change expected scores of keyphrases in HIGH set
        for (KeyPhrase keyPhrase : highSet) {
            if (keyPhrase.getScore() < minExpectedScore) {
                keyPhrase.setExpectedScore(minExpectedScore);
                keyPhrase.getNode().setExpectedScore(minExpectedScore);
            }
        }

    }

}
