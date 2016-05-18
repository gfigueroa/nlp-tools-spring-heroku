package com.figueroa.nlp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

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
@Component
public class NLPMain {
	
	// Singleton instance
	private static NLPMain instance = null;
	
	// Resources
	public final String contextPath; // Class path for resources
	public final static String RESOURCES_PATH = 
			"resources" + File.separator;
	
    // POSTagger and Lemmatizer
	public final static String POS_TAGGER_MODEL_PATH =
    		RESOURCES_PATH + "pos_models" + File.separator + 
    		"english-left3words-distsim.tagger";
	public final static String POS_TAGGER_CONFIG_PATH =
    		RESOURCES_PATH + "pos_models" +  File.separator + 
    		"english-left3words-distsim.tagger.props";
	public final POSTagger posTagger;
    
	public final static String TAG_SEPARATOR = "_";
	public final static String WN_HOME = RESOURCES_PATH + "WordNet-3.0";
	public final Lemmatizer lemmatizer;
	
    // TextRank
    public final static String TEXTRANK_RESOURCES_PATH = RESOURCES_PATH + "textrank";
    public final static String log4j_conf = TEXTRANK_RESOURCES_PATH + 
    		File.separator + "log4j.properties";
    public final static String LANG_CODE = "en";
    public static LanguageModel languageModel;
    private final static Stopwords stopwords = new Stopwords();
    public final TextRank textRank;
    
    /**
     * Get the unique (singleton) instance of this class. 
     * If the instance doesn't exist, then it is created.
     * @return singleton instance of NLPMain
     * @throws Exception 
     */
    public static NLPMain getNLPMainInstance() throws Exception {
    	if (instance == null) {
    		instance = new NLPMain();
    	}
    	return instance;
    }
    
    /**
     * Private constructor for singleton instance of this class.
     * @throws Exception 
     */
    private NLPMain() throws Exception {
    	String classpath = ResourceUtils.getFile("classpath:").getAbsolutePath();
    	// Check if path ends with separator
    	if (!classpath.endsWith(File.separator)) {
    		classpath += File.separator;
    	}
    	contextPath = classpath;
    	
    	// Load POSTagger
        posTagger = new POSTagger(contextPath + POS_TAGGER_MODEL_PATH, 
        		TAG_SEPARATOR);
    	
        // Load Lemmatizer
        lemmatizer = new Lemmatizer(contextPath + WN_HOME, posTagger);
        
        // Load TextRank
		languageModel = 
	    		LanguageModel.buildLanguage(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
		WordNet.buildDictionary(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
		textRank = new TextRank(stopwords, languageModel);
    }
    
    /**
     * Lemmatize each term in a given string.
     * @param contextPath the absolute root path of the project
     * @param text
     * @return the lemmatized text
     * @throws Exception
     */
    public String lemmatizeText(String text) throws Exception {
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
    public String tagText(String text) throws Exception {
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
    public ArrayList<KeyPhraseSimple> extractKeywords( 
    		String text, String method) throws Exception {

    	RankingMethod rankingMethod = 
    			KeyPhrase.getRankingMethodFromString(method);
    	
    	ArrayList<KeyPhraseSimple> keywords = new ArrayList<>();
    	switch (rankingMethod) {
    		case TEXTRANK:				
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
    			RankUpMain rankUpMain = 
    					RankUpMain.getRankUpMainInstance(textRank, lemmatizer, posTagger, stopwords);
	    		List<KeyPhrase> keyphrases = 
	    				(ArrayList<KeyPhrase>) rankUpMain.extractRankUpKeywords(
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