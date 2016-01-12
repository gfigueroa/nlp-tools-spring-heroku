package com.figueroa.nlp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.figueroa.nlp.textrank.LanguageModel;
import com.figueroa.nlp.textrank.MetricVector;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.WordNet;
import com.figueroa.util.ExceptionLogger;
import com.figueroa.util.ExceptionLogger.DebugLevel;

/**
 *
 * @author gfigueroa
 */
public class NLPMain {

	// Debugging
    public static DebugLevel DEBUG_LEVEL = DebugLevel.INFO;
    
    // Loggers
    public final static String EXCEPTION_LOGGER_DIR =
            "." + File.separator + "logs" + File.separator
            + "exception_log_" + ExceptionLogger.now() + ".log";
    public final static ExceptionLogger exceptionLogger = 
            new ExceptionLogger(EXCEPTION_LOGGER_DIR, DEBUG_LEVEL);
	
	// Resources
	public final static String RESOURCES_PATH = 
			"WEB-INF" + File.separator + "resources" + File.separator;
	
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
        		contextPath + POS_TAGGER_CONFIG_PATH, TAG_SEPARATOR);
    	// Load Lemmatizer
        Lemmatizer lemmatizer = new Lemmatizer(contextPath + WN_HOME, posTagger);
        
        String lemmatizedText = lemmatizer.stemText(text, false);
        return lemmatizedText;
    }
    
    /**
     * Assign POS tags to each term in the given string. 
     * Uses the Penn Treebank tagset.
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
        		contextPath + POS_TAGGER_CONFIG_PATH, TAG_SEPARATOR);
        
        String taggedText = posTagger.tagText(text);
        return taggedText;
    }
    
    public static ArrayList<String> extractKeywords(String contextPath, String text) throws Exception {
    	// Check if path ends with separator
    	if (!contextPath.endsWith(File.separator)) {
    		contextPath += File.separator;
    	}
    	
		languageModel = 
	    		LanguageModel.buildLanguage(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
		WordNet.buildDictionary(contextPath + TEXTRANK_RESOURCES_PATH, LANG_CODE);
		TextRank textRank = new TextRank(exceptionLogger, stopwords, languageModel);
		
		Collection<MetricVector> metricVectorCollection = textRank.run(text);
		ArrayList<String> keywords = new ArrayList<>();
		for (MetricVector metricVector : metricVectorCollection) {
			String keyword = metricVector.value.text;
			double score = metricVector.metric;
			keywords.add(keyword + " " + score);
		}
		
		return keywords;
    }
}