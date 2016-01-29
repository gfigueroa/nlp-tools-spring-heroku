package com.figueroa.nlp;

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
    
    private double score;

    public KeyPhrase(String text, 
            double score) {
        super(text);
        this.score = score;
    }
    
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
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
}
