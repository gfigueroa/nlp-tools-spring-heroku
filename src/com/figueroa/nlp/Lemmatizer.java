package com.figueroa.nlp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import edu.mit.jwi.*;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;
import java.util.List;

/**
 *
 * Implementation of the WordNet Stemmer
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * April 2011
 */
public class Lemmatizer {

    private IDictionary dict;  // The WordNet dictionary
    private POSTagger posTagger;
    private WordnetStemmer stemmer;

    /**
     * Constructor for the Lemmatizer class. The Lemmatizer requires an instance
     * of the POSTagger class, as it first needs to tag each term in a string
     * for higher precision.
     * Initializes the class with the given WordNet home directory and
     * POSTagger instance.
     * @param wnh: WordNet home directory
     * @param posTag: instance of POSTagger
     */
    public Lemmatizer(String wnh, POSTagger posTag) {
        // construct the URL to the Wordnet dictionary directory
        String wnhome = wnh;
        String path = wnhome + File.separator + "dict";

        posTagger = posTag;

        try {
            URL url = new URL("file", null, path);

            // construct the dictionary object and open it
            dict = new Dictionary(url);
            dict.open();

            stemmer = new WordnetStemmer(dict);
        }
        catch (MalformedURLException e) {
            System.err.println("Exception in Stemmer creation: " + e.getMessage());
            e.printStackTrace();
        } 
        catch (IOException e) {
        	System.err.println("Exception in Stemmer creation: " + e.getMessage());
        	e.printStackTrace();
		}
    }

    /**
     * Lemmatizes the given text, term by term.
     * @param text
     * @param textIsTagged: whether the text has already been POS-tagged or not.
     * @return
     */
    public String stemText(String text, boolean textIsTagged) {

        String taggedText;
        if (!textIsTagged) {
            taggedText = posTagger.tagText(text);
        }
        else {
            taggedText = text;
        }

        if (!taggedText.contains(posTagger.getSeparator())) {
            return taggedText;
        }

        String stemmedText = "";
        String[] tokenizedText = taggedText.split(" ");

        for (int i = 0; i < tokenizedText.length; i++) {
            String currToken = tokenizedText[i];

            String currWord = posTagger.getWord(currToken);
            String currTag = posTagger.getTag(currToken);

            POS pos = posTagger.getPOS(currTag);
            if (pos == null) {
                stemmedText = stemmedText.concat(currWord + " ");
            }
            else {
                List<String> stems = stemmer.findStems(currWord, pos);
                if (stems.isEmpty()) {
                    stemmedText = stemmedText.concat(currWord + " ");
                }
                else if (stems.get(0).equals("-LRB-")) {
                	stemmedText = stemmedText.concat("(");
                }
                else if (stems.get(0).equals("-RRB-")) {
                	stemmedText = stemmedText.concat(")");
                }
                else {
                    stemmedText = stemmedText.concat(stems.get(0) + " ");
                }
            }
        }

        stemmedText = stemmedText.trim(); // Clean text

        /*if (stemmedText.contains("-LRB-") || stemmedText.contains("-RRB-")) {
        return text;
        }*/

        if (stemmedText.isEmpty()) {
            if (textIsTagged) {
                stemmedText = posTagger.getWord(text);
            }
            else {
                stemmedText = text;
            }
        }

        return stemmedText;
    }

    /**
     * Returns the instance of the POSTagger
     * @return POSTagger instance for this Lemmatizer
     */
    public POSTagger getPosTagger() {
        return posTagger;
    }
}
