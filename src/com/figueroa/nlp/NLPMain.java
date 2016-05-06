package com.figueroa.nlp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.figueroa.controller.WebServiceController.KeyPhraseSimple;
import com.figueroa.nlp.KeyPhrase.RankingMethod;
import com.figueroa.nlp.rankup.RankUpMain;
import com.figueroa.nlp.textrank.LanguageModel;
import com.figueroa.nlp.textrank.MetricVector;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.WordNet;

/**
 *
 * @author gfigueroa
 */
public class NLPMain {

	// Resources
	public final static String RESOURCES_PATH = 
			/*"WEB-INF" + File.separator + */"resources" + File.separator;
	
    // POSTagger and Lemmatizer
	public final static String POS_TAGGER_MODEL_PATH =
    		RESOURCES_PATH + "pos_models" + File.separator + 
    		"english-left3words-distsim.tagger";
    
	public final static String POS_TAGGER_CONFIG_PATH =
    		RESOURCES_PATH + "pos_models" +  File.separator + 
    		"english-left3words-distsim.tagger.props";
    
	public final static String TAG_SEPARATOR = "_";
	public final static String WN_HOME = RESOURCES_PATH + "WordNet-3.0";
	
    // TextRank
    public final static String TEXTRANK_RESOURCES_PATH = RESOURCES_PATH + "textrank";
    public final static String log4j_conf = TEXTRANK_RESOURCES_PATH + 
    		File.separator + "log4j.properties";
    public final static String LANG_CODE = "en";
    public static LanguageModel languageModel;
    private final static Stopwords stopwords = new Stopwords();

//    /**
//     * Initialize the NLPMain class with the context path of the application.
//     * Initializes all required classes for the NLP tools.
//     * TODO: Make this initialization more efficient (once for entire app)
//     * @param contextPath
//     */
//    public NLPMain(String contextPath) {
//        // Check if path ends with separator
//    	if (!contextPath.endsWith(File.separator)) {
//    		contextPath += File.separator;
//    	}
//    	
//    	// Load POSTagger
//        posTagger = new POSTagger(contextPath + POS_TAGGER_MODEL_PATH, contextPath + 
//                POS_TAGGER_CONFIG_PATH, TAG_SEPARATOR);
//        // Load Lemmatizer
//        lemmatizer = new Lemmatizer(contextPath + WN_HOME, posTagger);
//    }
    
    /**
     * Lemmatize each term in a given string.
     * @param contextPath the absolute root path of the project
     * @param text
     * @return the lemmatized text
     * @throws Exception
     */
    public static String lemmatizeText(String contextPath, String text) throws Exception {
    	// Check if path ends with separator
    	if (!contextPath.endsWith(File.separator)) {
    		contextPath += File.separator;
    	}
    	
    	// Load POSTagger
        POSTagger posTagger = new POSTagger(contextPath + POS_TAGGER_MODEL_PATH, 
        		TAG_SEPARATOR);
    	// Load Lemmatizer
        Lemmatizer lemmatizer = new Lemmatizer(contextPath + WN_HOME, posTagger);
        
        String lemmatizedText = lemmatizer.stemText(text, false);
        return lemmatizedText;
    }
    
    /**
     * Assign POS tags to each term in the given string. 
     * Uses the Penn Treebank tagset.
     * @param contextPath the absolute root path of the project
     * @param text
     * @return the POS-tagged text
     * @throws Exception
     */
    public static String tagText(String contextPath, String text) throws Exception {
    	// Check if path ends with separator
    	if (!contextPath.endsWith(File.separator)) {
    		contextPath += File.separator;
    	}
    	
    	// Load POSTagger
        POSTagger posTagger = new POSTagger(contextPath + POS_TAGGER_MODEL_PATH, 
        		TAG_SEPARATOR);
        
        String taggedText = posTagger.tagText(text);
        return taggedText;
    }
    
    /**
     * Extract keywords from a given text using the TextRank algorithm
     * @param contextPath the absolute root path of the project
     * @param text
     * @return an ArrayList of KeyPhrase
     * @throws Exception
     */
    public static ArrayList<KeyPhraseSimple> extractKeywords(String contextPath, 
    		String text, String method) throws Exception {
    	
    	// Check if path ends with separator
    	if (!contextPath.endsWith(File.separator)) {
    		contextPath += File.separator;
    	}
    	
    	RankingMethod rankingMethod = 
    			KeyPhrase.getRankingMethodFromString(method);
    	
    	ArrayList<KeyPhraseSimple> keywords = new ArrayList<>();
    	switch (rankingMethod) {
    		case TEXTRANK:
				languageModel = 
			    		LanguageModel.buildLanguage(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
				WordNet.buildDictionary(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
				TextRank textRank = new TextRank(stopwords, languageModel);
				
				Collection<MetricVector> metricVectorCollection = textRank.run(text);
				
				for (MetricVector metricVector : metricVectorCollection) {
					String keyword = metricVector.value.text;
					double score = metricVector.metric;
					KeyPhraseSimple keyphrase = new KeyPhraseSimple(keyword, score);
					keywords.add(keyphrase);
				}
				Collections.sort(keywords);
				break;
    	
    		case RANKUP:
    		case RAKE:
    		case TFIDF:
    		case RIDF:
    		case CLUSTEREDNESS:
	    		List<KeyPhrase> keyphrases = 
	    				(ArrayList<KeyPhrase>) RankUpMain.extractRankUpKeywords(
	    						contextPath,
	    						text,
	    						rankingMethod);
	    		for (KeyPhrase rankUpKeyphrase : keyphrases) {
	    			KeyPhraseSimple keyphrase = 
	    					new KeyPhraseSimple(rankUpKeyphrase.text,
	    							rankUpKeyphrase.getRanking(rankingMethod));
	    			keywords.add(keyphrase);
	    		}
	    		break;
	    	default:
	    		return null;
    	}
    	
	    return keywords;
    }
}