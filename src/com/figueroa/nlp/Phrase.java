package com.figueroa.nlp;

/**
 * Phrase for RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class Phrase {
    
    protected String text;
    
    public Phrase(String text) {
        this.text = cleanText(text);
    }

    /**
     * Cleans the text of HTML tags and other inconsistencies, and trims it
     * @param text
     * @return clean text
     */
    public static String cleanText(String text) {
        // remove quotation marks
        text = text.replaceAll("\\\"", "");

        // remove html tags
        text = text.replaceAll("\\<.*?>", "");
        text = text.replaceAll("[<>]", "");

        // remove brackets and keys
        text = text.replaceAll("\\[", " ");
        text = text.replaceAll("\\]", " ");
        text = text.replaceAll("\\{", " ");
        text = text.replaceAll("\\}", " ");

        // fix parentheses
        if (text.contains("(") && !text.contains(")")) {
            text = text + ")";
        }
        // remove parentheses from single-word phrases
        if (text.split(" ").length == 1) {
            text = text.replaceAll("\\(", "");
            text = text.replaceAll("\\)", "");
        }

        // replace double spaces for 1 space
        text = text.replaceAll("  ", " ");

        text = text.trim();

        return text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
