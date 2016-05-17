package com.figueroa.nlp.rankup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.KeyPhrase.RankingMethod;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.nlp.NLPMain;
import com.figueroa.nlp.POSTagger;
import com.figueroa.nlp.Stopwords;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.GraphBasedKeywordExtractionMethod;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetLevel;
import com.figueroa.nlp.rankup.PhraseFeatures.Feature;
import com.figueroa.nlp.textrank.LanguageModel;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.WordNet;
import com.figueroa.util.Abstract;
import com.figueroa.util.Abstract.Type;
import com.figueroa.util.AbstractManager;
import com.figueroa.util.DatabaseManager;

/**
 * Main class for running RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class RankUpMain {

	private static final Logger logger = Logger.getLogger(RankUpMain.class);
	
	// Singleton instance
	private static RankUpMain instance = null;
	
	// Class path for resources
	public final String contextPath;
	
    // Runtime options
    // Debugging
    public final static boolean PRINT_SETS = false;
    public final static boolean PRINT_GEPHI_GRAPHS = false;
    
    // Changes and bug fixes
    public final static boolean MINMAX_MID_BUG_FIX = true;
    public final static boolean CORRECT_NEGATIVE_WEIGHTS = true;
    public final static boolean DENORMALIZE_MODIFICATION_VALUE = true;
    public final static boolean USE_DIFFERENTIAL_CONVERGENCE = true;
    
    // DatabaseManager and AbstractManager
    public final static String LOCAL_HOST = "localhost";
    
    public final static String MYSQL_CLASS = "com.mysql.jdbc.Driver";
    public final static String MYSQL_CONNECTION_PREFIX = "jdbc:mysql";
    public final static String MYSQL_DB_NAME = "New_RankUp_Tests";
    public final static String MYSQL_USER = "root";
    public final static String MYSQL_PASSWORD = "bakayarou00";
    
    public final static String REMOTE_HOST = "ec2-54-227-248-123.compute-1.amazonaws.com";
    
    public final static String POSTGRESQL_CLASS = "org.postgresql.Driver";
    public final static String POSTGRESQL_CONNECTION_PREFIX = "jdbc:postgresql";
    //public final static String POSTGRESQL_DB_NAME = "postgres";
    public final static String POSTGRESQL_DB_NAME = "d27phf5prvh7cm";
    //public final static String POSTGRESQL_USER = "postgres";
    public final static String POSTGRESQL_USER = "cccdunxdiqtlwe";
    //public final static String MYSQL_PASSWORD = "bakayarou00";
    public final static String POSTGRESQL_PASSWORD = "mBCvdlqfcJUj4T3LVy4s9uTPjP";
    public final static boolean REQUIRE_SSL = true;
    
    public final static String DB_CLASS_NAME = POSTGRESQL_CLASS;
    public final static String CONNECTION_STRING =
            POSTGRESQL_CONNECTION_PREFIX + "://" + REMOTE_HOST + "/" + 
            POSTGRESQL_DB_NAME + (REQUIRE_SSL ? "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory" : "");
    public final static String USER = POSTGRESQL_USER;
    public final static String PASSWORD = POSTGRESQL_PASSWORD;
    private static DatabaseManager databaseManager;
    public final static String ABSTRACT_TABLE = "Abstract";
    
    // AbstractManager
    private static AbstractManager abstractManager;
    
    // POSTagger and Lemmatizer
    private static POSTagger posTagger;
    private static Lemmatizer lemmatizer;

    // TextRank tools
    public static LanguageModel languageModel;
    
    // Python and RAKE tools
    private static Rake rake;
    public final static String PYTHON_RESOURCES_PATH = 
    		NLPMain.RESOURCES_PATH + "python";

    // RankUp-specific
    private static RankUp rankUp;
    public final static String RANKUP_RESOURCES_PATH = NLPMain.RESOURCES_PATH + 
    		"rankup";
    private final static String RANKUP_PROPERTIES_FILE = RANKUP_RESOURCES_PATH +
    		File.separator + "properties" + File.separator + "default.properties";
    public final List<Abstract> allAbstracts;
    private final RankUpProperties rankUpProperties;

    // Stopwords
    private static Stopwords stopwords;
    
    /**
     * Get the unique (singleton) instance of this class. 
     * If the instance doesn't exist, then it is created.
     * @return singleton instance of RankUpMain
     * @throws Exception
     */
    public static RankUpMain getRankUpMainInstance() throws Exception {
    	if (instance == null) {
    		instance = new RankUpMain();
    	}
    	return instance;
    }
    
    /**
     * Private constructor for singleton instance of this class.
     * @throws Exception
     */
    private RankUpMain() throws Exception {
    	logger.info("Starting RankUp...");
    	
    	String classpath = ResourceUtils.getFile("classpath:").getAbsolutePath();
    	// Check if path ends with separator
    	if (!classpath.endsWith(File.separator)) {
    		classpath += File.separator;
    	}
    	contextPath = classpath;
    	
        loadComponents();
        
        // Load RankUp Properties
        final String propertiesFile = contextPath + RANKUP_PROPERTIES_FILE;
        List<RankUpProperties> rankUpPropertiesList = 
                loadRankUpProperties(propertiesFile);
        rankUpProperties = rankUpPropertiesList.get(0);
        
        String abstractSource = rankUpPropertiesList.get(0).abstractSource;
        
        // Retrieve all abstracts
        logger.info("Retrieving abstracts...");
        allAbstracts =
                abstractManager.retrieveAbstracts(
                rankUpPropertiesList.get(0).abstractType,
                abstractSource, 
                ABSTRACT_TABLE);
        
        logger.info("***************************************");
        logger.info("");

        rankUp = new RankUp(
                    abstractManager, 
                    posTagger, 
                    lemmatizer, 
                    stopwords,
                    rake,
                    rankUpProperties, 
                    allAbstracts, 
                    MINMAX_MID_BUG_FIX,
                    CORRECT_NEGATIVE_WEIGHTS,
                    DENORMALIZE_MODIFICATION_VALUE,
                    USE_DIFFERENTIAL_CONVERGENCE);
    }
    
    /**
     * Loads the main components required by RankUp
     * @throws Exception 
     */
    private void loadComponents()
            throws Exception {
        
        logger.info("Loading components...");
        
        // Load Stopwords
        stopwords = new Stopwords();
        
        // Load TextRank components
        languageModel = 
        		LanguageModel.buildLanguage(contextPath + NLPMain.TEXTRANK_RESOURCES_PATH, 
        				NLPMain.LANG_CODE);
        WordNet.buildDictionary(contextPath + NLPMain.TEXTRANK_RESOURCES_PATH, 
        		NLPMain.LANG_CODE);
        //textRank = new TextRank(logger, stopwords, languageModel);
        
        // Load DatabaseManager
        databaseManager =
            new DatabaseManager(DB_CLASS_NAME, CONNECTION_STRING, USER, PASSWORD);
        
        // Load AbstractManager
        abstractManager = new AbstractManager(databaseManager);
        
        // Load POSTagger
        posTagger =
            new POSTagger(contextPath + NLPMain.POS_TAGGER_MODEL_PATH, NLPMain.TAG_SEPARATOR);
        
        // Load Lemmatizer
        lemmatizer = new Lemmatizer(contextPath + NLPMain.WN_HOME, posTagger);
        
        // Load and set up RAKE
        rake = new Rake(contextPath + PYTHON_RESOURCES_PATH);
    }

    /**
     * Loads and returns the RankUp properties file(s)
     * @param propertiesFileString
     * @return A list of RankUpProperties files if the given propertiesFileString
     * is a directory. The list will only contain 1 item if the propertiesFileString
     * is a single file.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception 
     */
    private static List<RankUpProperties> loadRankUpProperties(String propertiesFileString) 
            throws FileNotFoundException, IOException, Exception {
        
        ArrayList<RankUpProperties> propertiesList = new ArrayList<>();
        File propertiesFile = new File(propertiesFileString);
        
        // First, check if propertiesFile is a file or a directory
        ArrayList<File> fileList = new ArrayList<>();
        if (propertiesFile.isDirectory()) {
            for (File fileEntry : propertiesFile.listFiles()) {
                // Check if there is one more directory level
                if (fileEntry.isDirectory()) {
                    fileList.addAll(Arrays.asList(fileEntry.listFiles()));
                }
                else {
                    fileList.add(fileEntry);
                }
            }
        }
        // Else just add one file to list
        else {
            fileList.add(propertiesFile);
        }
        
        for (File fileEntry : fileList) {
            try (FileInputStream fis = new FileInputStream(fileEntry)) {
                Properties props = new Properties();
                props.load(fis);
                RankUpProperties rankUpProperties =
                        new RankUpProperties(fileEntry.getName(), props);
                propertiesList.add(rankUpProperties);
            }
            catch (FileNotFoundException e) {
                throw e;
            }
        }
        
        return propertiesList;
    }

    /**
     * Prints the given KeyPhrase list with a header
     * @param keyPhrases
     * @param method
     * @param writeToDataLog 
     */
    public static void printKeyPhrases(List<KeyPhrase> keyPhrases, String method) {

        logger.info("");
        logger.info("***********" + method + "************");
        logger.info("*Size: " + keyPhrases.size() + "*");
        for (KeyPhrase keyPhrase : keyPhrases) {
            String features = keyPhrase.getFeatures() != null ?
                    keyPhrase.getFeatures().toString() : "";
            logger.info(keyPhrase.toString() + "\t" + features);
        }
        logger.info("");
    }
    
    /**
     * Gets a list of KeyPhrases ordered according to the given Feature
     * @param rankUp
     * @param useWholeTextRankGraph
     * @param abs
     * @param feature
     * @return
     * @throws Exception 
     */
    private static List<KeyPhrase> getFeatureKeyphrases(List<KeyPhrase> keyphrases,
            final Feature feature) throws Exception {
        
        Comparator comparator =
                new Comparator<KeyPhrase>() {
                    @Override
                    public int compare(KeyPhrase a, KeyPhrase b) {
                        Double featureA = a.getFeatures().getFeatureValue(feature, a);
                        Double featureB = b.getFeatures().getFeatureValue(feature, b);
                        return featureB.compareTo(featureA);
                    }
                };
        
        List<KeyPhrase> newKeyphrases = new ArrayList<>(keyphrases);
        Collections.sort(newKeyphrases, comparator);
        
        HashMap<String, KeyPhrase> uniqueNewKeyphrases = new HashMap<>();
        for (int i = newKeyphrases.size() - 1; i >= 0; i--) {
            uniqueNewKeyphrases.put(newKeyphrases.get(i).text, newKeyphrases.get(i));
        }

        ArrayList<KeyPhrase> sortedUniqueNewKeyphrases = 
                new ArrayList<>(uniqueNewKeyphrases.values());
        Collections.sort(sortedUniqueNewKeyphrases, comparator);
        
        return sortedUniqueNewKeyphrases;
    }
    
    /**
     * Get, insert and print a list of KeyPhrases according to the given Feature
     * @param abs
     * @param textRankProperties
     * @param rankUpProperties
     * @param feature
     * @param textRankKeyphrases
     * @param rankUpPropertiesProcessedCount
     * @param GET_KEYPHRASES
     * @param extractedKeywordsTable
     * @return
     * @throws Exception 
     */
    private static List<KeyPhrase> getFeatureKeyphrases(
            RankUpProperties rankUpProperties,
            final Feature feature,
            List<KeyPhrase> textRankKeyphrases) throws Exception {
        
        String featureString = PhraseFeatures.getShortFeatureString(feature);
        
        List<KeyPhrase> keyphrases = new ArrayList<>();

        keyphrases =
                getFeatureKeyphrases(textRankKeyphrases, feature);

        printKeyPhrases(keyphrases, featureString);

        // Get KeyPhraseGraph
        if (PRINT_SETS) {
            KeyPhraseGraph graph = new KeyPhraseGraph(
                    keyphrases,
                    rankUpProperties.setAssignmentApproach,
                    rankUpProperties.featureLowerBound, 
                    rankUpProperties.featureUpperBound,
                    feature);
            logger.debug("**** " + featureString + " Sets ****");
            graph.printFeatureSet(SetLevel.LOW);
            graph.printFeatureSet(SetLevel.MID);
            graph.printFeatureSet(SetLevel.HIGH);
        }

        return keyphrases;
    }
    
    /**
     * Run RankUp with the corresponding properties and configuration.
     * @param contextPath
     * @param text
     */
    public List<KeyPhrase> extractRankUpKeywords(
    		String text, RankingMethod rankingMethod) {
        
        try {

            Abstract abs = new Abstract(0, text, Type.TESTING, lemmatizer);

            logger.info("Text: " + abs.getOriginalText() + "\n");
            
            // Determine keyword extraction method
            GraphBasedKeywordExtractor keywordExtractor;
            if (rankUpProperties.keywordExtractionMethod == null ||
                    rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.TEXTRANK) {
                
                TextRank textRank;
                // Check if this abstract already contains TextRank
                if (abs.getTextRank() != null) {
                    textRank = abs.getTextRank();
                }
                else {
                    textRank = new TextRank(stopwords, languageModel);
                    abs.setTextRank(textRank);
                }
                 
                keywordExtractor = new GraphBasedKeywordExtractor(
                        rankUpProperties,
                        textRank);
            }
            else if (rankUpProperties.keywordExtractionMethod == GraphBasedKeywordExtractionMethod.RAKE) {
                keywordExtractor = new GraphBasedKeywordExtractor(
                        rankUpProperties,
                        rake);
            }
            else {
                logger.error("Wrong Keyword Extraction Method");
                return null;
            }

            // Run RankUp
            List<KeyPhrase> rankUpKeyphrases = null;
            List<KeyPhrase> originalKeyphrases = null;
            try {
                rankUpKeyphrases = 
                        rankUp.runRankUp(abs, PRINT_GEPHI_GRAPHS, keywordExtractor);
                originalKeyphrases = rankUp.getOriginalKeyphraseSet();
            }
            catch (Exception exception) {
                logger.error("Exception in runRankUp: " + 
                        exception.getMessage());
                exception.printStackTrace();
            }

            // Get TFIDF KeyPhrases
            List<KeyPhrase> tfidfKeyphrases = getFeatureKeyphrases(
                    rankUpProperties, Feature.TFIDF_STEMMED,
                    originalKeyphrases);

            // Get RIDF KeyPhrases
            List<KeyPhrase> ridfKeyphrases = getFeatureKeyphrases(
                    rankUpProperties, Feature.RIDF_STEMMED,
                    originalKeyphrases);


            // Get Clusteredness KeyPhrases
            List<KeyPhrase> clusterednessKeyphrases = getFeatureKeyphrases(
                    rankUpProperties, Feature.CLUSTEREDNESS_STEMMED,
                    originalKeyphrases);

            // Get RAKE KeyPhrases
            List<KeyPhrase> rakeKeyphrases= getFeatureKeyphrases(
                    rankUpProperties, Feature.RAKE_STEMMED,
                    originalKeyphrases);

            // Print TextRank KeyPhrases
            printKeyPhrases(originalKeyphrases, 
                    rankUpProperties.keywordExtractionMethod.toString());
            
            // Print RankUp KeyPhrases into database
            printKeyPhrases(rankUpKeyphrases, "RankUp");

            logger.info("");
            logger.info("RankUp completed for this text!");
            logger.info("");
            
            switch (rankingMethod) {
	            case RANKUP:
	            	//return rankUp.getSortedKeyphrases(rankUpKeyphrases);
	            	return rankUpKeyphrases;
	            case TEXTRANK:
	            	return originalKeyphrases;
	            case RAKE:
	            	return rakeKeyphrases;
	            case TFIDF:
	            	return tfidfKeyphrases;
	            case RIDF:
	            	return ridfKeyphrases;
	            case CLUSTEREDNESS:
	            	return clusterednessKeyphrases;
	            default:
	            	return null;
            }
        }
        catch (FileNotFoundException fnfe) {
            logger.error("Properties file not found! " + fnfe.getMessage());
            return null;
        }
        catch (IOException ioe) {
            logger.error("IO Exception! " + ioe.getMessage());
            return null;
        }
        catch (Exception e) {
            logger.error("Exception in RankUp Main: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}