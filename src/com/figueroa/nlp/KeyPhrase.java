package com.figueroa.nlp;

import java.util.Comparator;
import com.figueroa.util.MiscUtils;

/**
 * Keyphrase for RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class KeyPhrase extends Phrase implements Comparable<KeyPhrase> {

    public static enum ScoreDirection {
        NO_CHANGE, INCREASE, DECREASE
    }
    
    private double finalTextRankScore;
    private final double originalTextRankScore;
    private double score;
    private final double originalScore;
    private final double originalNodeScore;
    private double expectedScore;
    private final Node node;
    
    // Previous scores
    private double previousFinalTextRankScore;
    private double previousScore;

    public KeyPhrase(String text, 
            double score, 
            double originalTextRankScore,
            Node node) {
        super(text);
        this.finalTextRankScore = -1.0; // The final TexTrank score (metric) assigned
        this.originalTextRankScore = originalTextRankScore; // The original TextRank score (metric) assigned
        this.score = score;
        this.originalScore = score; // The first score assigned
        this.originalNodeScore = node != null ? 
                node.getRank() : 0; // The first TextRank node score assigned
        this.expectedScore = -1.0;
        this.node = node;
        this.previousFinalTextRankScore = -1.0;
        this.previousScore = -1.0;
    }

    public double getFinalTextRankScore() {
        return finalTextRankScore;
    }
    
    public double getOriginalTextRankScore() {
        return originalTextRankScore;
    }
    
    public double getScore() {
        return score;
    }

    public double getOriginalScore() {
        return originalScore;
    }

    public double getOriginalNodeScore() {
        return originalNodeScore;
    }

    public double getExpectedScore() {
        return expectedScore;
    }
    
    public Node getNode() {
        return node;
    }
    
    public double getPreviousFinalTextRankScore() {
        return previousFinalTextRankScore;
    }
    
    public double getPreviousScore() {
        return previousScore;
    }
    
    public void setFinalTextRankScore(double finalTextRankScore) {
        this.previousFinalTextRankScore = this.finalTextRankScore;
        this.finalTextRankScore = finalTextRankScore;
    }
    
    public void setScore(double score) {
        // First, set the previous score
        this.previousScore = this.score;
        this.score = score;
    }

    public void setExpectedScore(double expectedScore) {
        this.expectedScore = expectedScore;
    }
    
    public static String getScoreDirectionString(ScoreDirection scoreDirection) {
        if (scoreDirection == null) {
            return null;
        }
        switch (scoreDirection) {
            case NO_CHANGE:
                return "=";
            case INCREASE:
                return "\u2191";
            case DECREASE:
                return "\u2193";
            default:
                return null;
        }
    }
    
    /**
     * Get the direction of the finalTextRankScore in comparison with the 
     * originalTextRankScore
     * @return 
     */
    public ScoreDirection getFinalTextRankScoreDirection() {
        if (finalTextRankScore > originalTextRankScore) {
            return ScoreDirection.INCREASE;
        }
        else if (finalTextRankScore < originalTextRankScore) {
            return ScoreDirection.DECREASE;
        }
        else {
            return ScoreDirection.NO_CHANGE;
        }
    }
    
    /**
     * Get the direction of the current score (score) in comparison with the 
     * original score (originalScore)
     * @return 
     */
    public ScoreDirection getCurrentScoreDirection() {
        if (score > originalScore) {
            return ScoreDirection.INCREASE;
        }
        else if (score < originalScore) {
            return ScoreDirection.DECREASE;
        }
        else {
            return ScoreDirection.NO_CHANGE;
        }
    }
    
    /**
     * Get the current direction of the TextRank node rank in comparison with the 
     * original TextRank node score (originalNodeScore)
     * @return 
     */
    public ScoreDirection getCurrentNodeScoreDirection() {
        if (node.getRank() > originalNodeScore) {
            return ScoreDirection.INCREASE;
        }
        else if (node.getRank() < originalNodeScore) {
            return ScoreDirection.DECREASE;
        }
        else {
            return ScoreDirection.NO_CHANGE;
        }
    }
    
    /**
     * Get the direction of the current score (score) that is expected in comparison
     * with the expected score (expectedScore)
     * @return 
     */
    public ScoreDirection getExpectedScoreDirection() {
        if (expectedScore == -1) {
            return ScoreDirection.NO_CHANGE;
        }
        else if (expectedScore > originalScore) {
            return ScoreDirection.INCREASE;
        }
        else if (expectedScore < originalScore) {
            return ScoreDirection.DECREASE;
        }
        else {
            return ScoreDirection.NO_CHANGE;
        }
    }
    
    /**
     * Return whether the finalTexTrankScore direction was as intended by the
     * expected score (expectedScore)
     * @return true if score direction is as intended, false otherwise. 
     * Returns null if the expectedScoreDirection is neither up nor down
     */
    public Boolean finalTextRankScoreDirectionIsCorrect() {
        if (getExpectedScoreDirection() == ScoreDirection.NO_CHANGE) {
            return null;
        }
        else {
            if (originalTextRankScore == -1.0) {
                return null;
            }
            else {
                return (getFinalTextRankScoreDirection() == getExpectedScoreDirection());
            }
        }
    }
    
    /**
     * Return whether the current score (score) direction was as intended by the
     * expected score (expectedScore)
     * @return true if score direction is as intended, false otherwise. 
     * Returns null if the expectedScoreDirection is neither up nor down
     */
    public Boolean currentScoreDirectionIsCorrect() {
        if (getExpectedScoreDirection() == ScoreDirection.NO_CHANGE) {
            return null;
        }
        else {
            return (getCurrentScoreDirection() == getExpectedScoreDirection());
        }
    }
    
    /**
     * Return whether the final TextRank node score (textRankNodeScore) direction was as intended by the
     * expected score (expectedScore)
     * @return true if TextTank score direction is as intended, false otherwise. 
     * Returns null if the expectedScoreDirection is neither up nor down
     */
    public Boolean textRankNodeScoreDirectionIsCorrect() {
        if (getExpectedScoreDirection() == ScoreDirection.NO_CHANGE) {
            return null;
        }
        else {
            return (getCurrentNodeScoreDirection() == getExpectedScoreDirection());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof KeyPhrase) {
            KeyPhrase keyPhrase = (KeyPhrase) o;
            return keyPhrase.node.key.equals(this.node.key);
        }
        else {
            return false;
        }
    }

    /**
     * Compare method for sort ordering.
     * @param that
     */
    public int compareTo(final KeyPhrase that) {
        if (this.score > that.score) {
            return -1;
        }
        else if (this.score < that.score) {
            return 1;
        }
        else {
            return this.text.compareTo(that.text);
        }
    }

    /**
     * Ascending comparator for TFIDF_STEMMED
     */
    public static Comparator<KeyPhrase> TFIDFStemmedComparatorAscending =
            new Comparator<KeyPhrase>() {

        public int compare(KeyPhrase keyPhrase1, KeyPhrase keyPhrase2) {
            Double keyPhraseTFIDFStemmed1 = keyPhrase1.getFeatures().tfidfStemmed;
            Double keyPhraseTFIDFStemmed2 = keyPhrase2.getFeatures().tfidfStemmed;

            // Ascending order
            return keyPhraseTFIDFStemmed1.compareTo(keyPhraseTFIDFStemmed2);
        }
    };

    /**
     * Descending comparator for TFIDF_STEMMED
     */
    public static Comparator<KeyPhrase> TFIDFStemmedComparatorDescending =
            new Comparator<KeyPhrase>() {

        public int compare(KeyPhrase keyPhrase1, KeyPhrase keyPhrase2) {
            Double keyPhraseTFIDFStemmed1 = keyPhrase1.getFeatures().tfidfStemmed;
            Double keyPhraseTFIDFStemmed2 = keyPhrase2.getFeatures().tfidfStemmed;

            // Ascending order
            return keyPhraseTFIDFStemmed2.compareTo(keyPhraseTFIDFStemmed1);
        }
    };
    
    /**
     * Reverts the keyphrase values to their previous states
     */
    public void revertKeyPhrase() {
        // Check that previous text rank score is not negative
        finalTextRankScore = previousFinalTextRankScore >= 0 ? 
                previousFinalTextRankScore : originalTextRankScore;
        score = previousScore >= 0 ?
                previousScore : originalScore;
    }
    
    /**
     * Resets the keyphrase scores for reusing
     */
    public void resetKeyPhraseScores() {
        this.finalTextRankScore = -1.0; // The final TexTrank score (metric) assigned
        this.score = originalScore;
        this.expectedScore = -1.0;
        this.previousFinalTextRankScore = -1.0;
        this.previousScore = -1.0;
    }
    
    @Override
    public String toString() {
        String output = "";

        String adjustedText = text;
        while (adjustedText.length() < 40) {
            adjustedText = adjustedText.concat(" ");
        }

        String scoreString = MiscUtils.convertDoubleToFixedCharacterString(score, 2);
        
        output += adjustedText;
        output += "S: " + scoreString; // Current Score

        return output;
    }
    
//    @Override
//    public String toString() {
//        String output = "";
//
//        String adjustedText = text;
//        while (adjustedText.length() < 45) {
//            adjustedText = adjustedText.concat(" ");
//        }
//
//        String previousFinalTextRankScoreString =
//                MiscUtils.convertDoubleToFixedCharacterString(previousFinalTextRankScore, 2);
//        String finalTextRankScoreString = 
//                MiscUtils.convertDoubleToFixedCharacterString(finalTextRankScore, 2);
//        String originalTextRankScoreString = 
//                MiscUtils.convertDoubleToFixedCharacterString(originalTextRankScore, 2);
//        
//        String previousScoreString = MiscUtils.convertDoubleToFixedCharacterString(previousScore, 2);
//        String scoreString = MiscUtils.convertDoubleToFixedCharacterString(score, 2);
//        String originalScoreString = MiscUtils.convertDoubleToFixedCharacterString(originalScore, 2);
//        
//        String previousNodeScoreString = 
//                MiscUtils.convertDoubleToFixedCharacterString(node.getPreviousRank(), 2);
//        String textRankNodeScoreString = MiscUtils.convertDoubleToFixedCharacterString(node.getRank(), 2);
//        String originalNodeScoreString = 
//                MiscUtils.convertDoubleToFixedCharacterString(originalNodeScore, 2);
//        
//        String expectedScoreString = MiscUtils.convertDoubleToFixedCharacterString(expectedScore, 2);
//        
//        output += adjustedText;
//        output += "FTRS: " + finalTextRankScoreString;                  // Final TextRank Score (metric)
//        output += " (P: " + previousFinalTextRankScoreString + ")";
//        output += " [" + getScoreDirectionString(getFinalTextRankScoreDirection());
//        output += " (" + MiscUtils.getBooleanCheckString(finalTextRankScoreDirectionIsCorrect()) + ")]";
//        output += ", OTRS: " + originalTextRankScoreString;             // Original TextRank Score (metric)
//        output += ", S: " + scoreString;                                // Current Score
//        output += " (P: " + previousScoreString + ")";
//        output += " [" + getScoreDirectionString(getCurrentScoreDirection());
//        output += " (" + MiscUtils.getBooleanCheckString(currentScoreDirectionIsCorrect()) + ")]";
//        output += ", OS: " + originalScoreString;                       // Original Score
//        output += ", TRNS: " + textRankNodeScoreString;                 // Current TextRank Node Score
//        output += " (P: " + previousNodeScoreString + ")";
//        output += " [" + getScoreDirectionString(getCurrentNodeScoreDirection());
//        output += " (" + MiscUtils.getBooleanCheckString(textRankNodeScoreDirectionIsCorrect()) + ")]";
//        output += ", OTRNS: " + originalNodeScoreString;        // Original TextRank Node Score
//        output += ", ES: " + expectedScoreString;                       // Expected Score
//        output += " [" + getScoreDirectionString(getExpectedScoreDirection()) + "]";
//
//        return output;
//    }
}
