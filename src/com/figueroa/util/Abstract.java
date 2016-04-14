package com.figueroa.util;

import java.util.HashMap;
import java.util.List;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.nlp.rake.RakeNode;
import com.figueroa.nlp.textrank.TextRankNode;
import com.figueroa.nlp.textrank.TextRank;

/**
 * Representation of an Abstract text
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class Abstract {

    private final int abstractId;
    private String originalText;
    private String stemmedText;
    private Type type;
    private List<KeyPhrase> originalKeyphraseSet;
    private boolean phraseFeaturesSet;
    private TextRank textRank;
    private HashMap<String, RakeNode> rakeFullGraph;
    private HashMap<String, Double> currentRakeKeyphrases;
    private HashMap<String, Double> currentRakeWords;

    public enum Type {
        TRAINING, TESTING, VALIDATION, ALL
    }
    
    public static Type getTypeFromString(String typeString) {
        for (Type type : Type.values()) {
            if (type.toString().equalsIgnoreCase(typeString)) {
                return type;
            }
        }
        
        return null;
    }

    public Abstract(int abstractId, String originalText, Type type, Lemmatizer lemmatizer)
            throws Exception {

        this.abstractId = abstractId;
        this.originalText = cleanText(originalText);
        this.type = type;
        originalKeyphraseSet = null;
        phraseFeaturesSet = false;
        textRank = null;

        try {
            stemmedText = lemmatizer.stemText(originalText, false);
        }
        catch (Exception e) {
            throw new Exception("Exception in Abstract creation: " + e.getMessage());
        }
    }

    public Abstract(int abstractId, String originalText, String stemmedText, Type type)
            throws Exception {
        this.abstractId = abstractId;
        this.originalText = cleanText(originalText);
        this.stemmedText = cleanText(stemmedText);
        this.type = type;
    }

    // Cleans the text of HTML tags and other inconsistencies, and trims it
    public static String cleanText(String text) {
        // remove quotation marks
        text = text.replaceAll("\\\"", "");

        // remove html tags
        text = text.replaceAll("\\<.*?>", " ");
        text = text.replaceAll("[<>]", " ");

        // remove brackets and keys
        //text = text.replaceAll("\\( ", ". ");
        //text = text.replaceAll("\\) ", ". ");
        text = text.replaceAll("\\[", " ");
        text = text.replaceAll("\\]", " ");
        text = text.replaceAll("\\{", " ");
        text = text.replaceAll("\\}", " ");
        text = text.replaceAll("\\|", " ");
        
        // remove backslashes
        text = text.replaceAll("\\\\", " ");

        // Add space after commas and periods
        text = text.replaceAll(",", ", ");
        text = text.replaceAll("\\.", ". ");

        // replace two or more spaces for 1 space
        text = text.replaceAll(" +", " ");
        
        // replace new line and space with new line
        text = text.replaceAll("\n ", "\n");

        text = text.trim();

        return text;
    }

    // Returns the number of words in the abstract text
    public int wordCount() {
        return originalText.split(" ").length;
    }

    public int getAbstractId() {
        return abstractId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getStemmedText() {
        return stemmedText;
    }

    public Type getType() {
        return type;
    }
    
    public void setOriginalKeyphraseSet(List<KeyPhrase> originalKeyphraseSet) {
        this.originalKeyphraseSet = originalKeyphraseSet;   
    }
    
    public List<KeyPhrase> getOriginalKeyphraseSet() {
        return originalKeyphraseSet;
    }
    
    public boolean phraseFeaturesSet() {
        return phraseFeaturesSet;
    }
    
    public void setPhraseFeatures() {
        phraseFeaturesSet = true;
    }
    
    public void resetScoresAndWeights() {
        
        // First reset keyphrase set
        if (originalKeyphraseSet != null) {
            for (KeyPhrase keyphrase : originalKeyphraseSet) {
                keyphrase.resetKeyPhraseScores();
            }
        }
        
        // Then, reset textrank graph
        if (textRank != null) {
            for (TextRankNode node : textRank.getGraph().values()) {
                node.resetNodeScoresAndWeights();
            }
        }
        
        // Or RAKE graph
        if (rakeFullGraph != null) {
            for (RakeNode node : rakeFullGraph.values()) {
                node.resetNodeScoresAndWeights();
            }
        }
        
    }
    
    public void setTextRank(TextRank textRank) {
        this.textRank = textRank;
    }
    
    public TextRank getTextRank() {
        return textRank;
    }
    
    public HashMap<String, RakeNode> getRakeFullGraph() {
        return rakeFullGraph;
    }

    public void setRakeFullGraph(HashMap<String, RakeNode> rakeFullGraph) {
        this.rakeFullGraph = rakeFullGraph;
    }
    
    public HashMap<String, Double> getCurrentRakeKeyphrases() {
        return currentRakeKeyphrases;
    }

    public void setCurrentRakeKeyphrases(HashMap<String, Double> currentRakeKeyphrases) {
        this.currentRakeKeyphrases = currentRakeKeyphrases;
    }

    public HashMap<String, Double> getCurrentRakeWords() {
        return currentRakeWords;
    }

    public void setCurrentRakeWords(HashMap<String, Double> currentRakeWords) {
        this.currentRakeWords = currentRakeWords;
    }
}
