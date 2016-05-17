package com.figueroa.nlp;

import edu.mit.jwi.item.POS;
import java.io.BufferedReader;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.StringReader;
import java.util.*;

/**
 *
 * Implementation of the Stanford POS Tagger
 * bidirectional-distsim-wsj-0-18.tagger
 * Trained on WSJ sections 0-18 using a bidirectional architecture and
 * including word shape and distributional similarity features.
 * Penn Treebank tagset.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * April 2011
 **/
public class POSTagger {

    private String modelFile;
    private MaxentTagger tagger;
    private String separator;
    private HashMap<String, POS> posTags; // A mapping between Penn Treebank Tagset and Wordnet POS Tags
    
    /**
     * Constructor for the POSTagger class. Initializes the class with the given
     * model file directory, configuration file directory and default separator
     * for POS tags
     * @param modelF: the model file directory
     * @param configF: the configuration file directory
     * @param sep: the default separator between a term and a POS tag
     */
    public POSTagger(String modelF, String sep) {
        modelFile = modelF;
        separator = sep;

        posTags = createPOSTags();

        try {
            //DataInputStream is = new DataInputStream(new FileInputStream(configFile));
            //TaggerConfig config = TaggerConfig.readConfig(is);

            //String[] args = {"-model", modelFile, "-verboseResults", "false"};
            //TaggerConfig config = new TaggerConfig(args);

            //config.setProperty("model", "left3words-wsj-0-18.tagger");
            //config.setProperty("verboseResults", "false");

            //tagger = new MaxentTagger(modelFile, config);
            tagger = new MaxentTagger();
            tagger = new MaxentTagger(modelFile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * POS-tags the given string, term by term
     * @param text
     * @return the tagged text
     */
    public String tagText(String text) {

        // Clean original text
        text = text.replaceAll("\\<.*?>", "");     // Remove HTML tags
        text = text.replaceAll(separator, " ");   // Remove separators
        text = text.replaceAll("  ", " ");        // Remove double spaces
        text = text.trim();

        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new StringReader(text)));

        String taggedText = "";
        for (List<HasWord> sentence : sentences) {
            ArrayList<TaggedWord> tSentence = (ArrayList<TaggedWord>) tagger.tagSentence(sentence);
            taggedText = taggedText.concat(Sentence.listToString(tSentence, false, separator) + " ");
        }

        // Clean processed text
        taggedText = taggedText.replaceAll("  ", " ");  // Remove double spaces
        taggedText = taggedText.trim();

        return taggedText;
    }

    /**
     * Gets the word part from an already tagged term
     * @param taggedWord
     * @return word part from a tagged term
     */
    public String getWord(String taggedWord) {
        String word;
        if (!(taggedWord.indexOf(separator) == -1)) {
            word = taggedWord.substring(0, taggedWord.indexOf(separator));
        }
        else {
            word = taggedWord;
        }

        return word;
    }

    /**
     * Gets tag part from an already tagged term
     * @param taggedWord
     * @return tag part from a tagged term
     */
    public String getTag(String taggedWord) {
        String tag;
        if (!(taggedWord.indexOf(separator) == -1)) {
            tag = taggedWord.substring(taggedWord.indexOf(separator) + 1);
        }
        else {
            tag = "NONE";
        }

        return tag;
    }

    /**
     * Gets the default separator used in the tagger
     * @return
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Gets the WordNet POS mapping from the given POS Tag
     * @param posTag: the Penn Treebank POS tag
     * @return the WordNet POS instance
     */
    public POS getPOS(String posTag) {
        return posTags.get(posTag);
    }

    /**
     * Creates a mapping between WordNet POS Tags (simple) and the POS Tags used in this
     * tagger (Penn Treebank tagset).
     * @return a HashMap mapping composite Tags (e.g. JJ, NNP, VB, etc.) with 
     * simple Tags (e.g. Adjective, Noun, Verb, etc.)
     */
    private HashMap<String, POS> createPOSTags() {
        posTags = new HashMap<String, POS>();

        // Available types: POS.ADJECTIVE, POS.ADVERB, POS.NOUN, POS.VERB

        //Keys: POS Tag, Values: POS

        posTags.put("JJ", POS.ADJECTIVE);
        posTags.put("JJR", POS.ADJECTIVE);
        posTags.put("JJS", POS.ADJECTIVE);
        posTags.put("NN", POS.NOUN);
        posTags.put("NNP", POS.NOUN);
        posTags.put("NNPS", POS.NOUN);
        posTags.put("NNS", POS.NOUN);
        posTags.put("RB", POS.ADVERB);
        posTags.put("RBR", POS.ADVERB);
        posTags.put("RBS", POS.ADVERB);
        posTags.put("VB", POS.VERB);
        posTags.put("VBD", POS.VERB);
        posTags.put("VBG", POS.VERB);
        posTags.put("VBN", POS.VERB);
        posTags.put("VBP", POS.VERB);
        posTags.put("VBZ", POS.VERB);

        return posTags;
    }
}
