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

import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.KeywordExtractionMethod;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetLevel;
import com.figueroa.nlp.rankup.PhraseFeatures.Feature;
import com.figueroa.nlp.POSTagger;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.nlp.Stopwords;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.textrank.LanguageModel;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.WordNet;
import com.figueroa.util.Abstract;
import com.figueroa.util.Abstract.Type;
import com.figueroa.util.AbstractManager;
import com.figueroa.util.DatabaseManager;

import com.figueroa.nlp.NLPMain;

/**
 * Main class for running RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class Main_Standalone {

	private static final Logger logger = Logger.getLogger(Main_Standalone.class);
	
    // Runtime options
    // Debugging
    public final static boolean PRINT_RESULTS = false;
    public final static boolean PRINT_SETS = false;
    public final static boolean PRINT_GEPHI_GRAPHS = false;
    // Keyphrase sets to obtain
    public final static boolean GET_TFIDF_KEYPHRASES = false;
    public final static boolean GET_RIDF_KEYPHRASES = false;
    public final static boolean GET_CLUSTEREDNESS_KEYPHRASES = false;
    public final static boolean GET_RAKE_KEYPHRASES = false;
    public final static boolean GET_ORIGINAL_KEYPHRASES = true;
    public final static boolean GET_RANKUP_KEYPHRASES = true;
    // Changes and bug fixes
    public final static boolean PARTIAL_MATCHING = true;
    public final static boolean MINMAX_MID_BUG_FIX = true;
    public final static boolean CORRECT_NEGATIVE_WEIGHTS = true;
    public final static boolean DENORMALIZE_MODIFICATION_VALUE = true;
    public final static boolean USE_DIFFERENTIAL_CONVERGENCE = true;
    
    // DatabaseManager and AbstractManager
    public final static String HOST_IP = "140.114.77.17";
    public final static String DB_CLASS_NAME = "com.mysql.jdbc.Driver";
    public final static String CONNECTION_STRING =
            "jdbc:mysql://" + HOST_IP + "/New_RankUp_Tests";
    public final static String USER = "root";
    public final static String PASSWORD = "bakayarou00";
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

    // RankUp-specific
    private static RankUp rankUp;
    public final static String RANKUP_RESOURCES_PATH = NLPMain.RESOURCES_PATH + 
    		"rankup";
    private final static String RANKUP_PROPERTIES_FILE = RANKUP_RESOURCES_PATH +
    		File.separator + "properties" + File.separator + "default.properties";

    // Stopwords
    private static Stopwords stopwords;

    /**
     * Loads the main components required by RankUp
     * @throws Exception 
     */
    private static void loadComponents(String contextPath)
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
        rake = new Rake();
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

        logger.debug("");
        logger.debug("***********" + method + "************");
        logger.debug("*Size: " + keyPhrases.size() + "*");
        for (KeyPhrase keyPhrase : keyPhrases) {
            String features = keyPhrase.getFeatures() != null ?
                    keyPhrase.getFeatures().toString() : "";
            logger.debug(keyPhrase.toString() + "\t" + features);
        }
        logger.debug("");
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
    private static List<KeyPhrase> getAndInsertFeatureKeyphrases(
            RankUpProperties rankUpProperties,
            final Feature feature,
            List<KeyPhrase> textRankKeyphrases,
            boolean GET_KEYPHRASES) throws Exception {
        
        String featureString = PhraseFeatures.getShortFeatureString(feature);
        
        List<KeyPhrase> keyphrases = new ArrayList<>();
        if (GET_KEYPHRASES) {

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
        }
        return keyphrases;
    }
    
    /**
     * The main function
     * @param args 
     */
    public static void extractRankUpKeywords(String contextPath) {
        
        try {

            logger.info("Starting RankUp...");

            loadComponents(contextPath);
            
            // Load RankUp Properties
            final String propertiesFile = contextPath + RANKUP_PROPERTIES_FILE;
            List<RankUpProperties> rankUpPropertiesList = 
                    loadRankUpProperties(propertiesFile);
            RankUpProperties rankUpProperties = rankUpPropertiesList.get(0);
            
            String abstractSource = rankUpPropertiesList.get(0).abstractSource;
            
            // Retrieve all abstracts
            logger.info("Retrieving abstracts...");
            List<Abstract> allAbstracts =
                    abstractManager.retrieveAbstracts(
                    rankUpPropertiesList.get(0).abstractType,
                    abstractSource, 
                    ABSTRACT_TABLE);
            
            logger.info("***************************************");
            logger.info("");

            rankUp = new RankUp(
                    //textRank,
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
  
                String text = "Compatibility of systems of linear constraints over the set of natural numbers. " +
                    "Criteria of compatibility of a system of linear Diophantine equations, strict inequations, " +
                    "and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and " +
                    "algorithms of construction of minimal generating sets of solutions for all types of systems are given. " +
                    "These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions " +
                    "can be used in solving all the considered types of systems and systems of mixed types.";
            Abstract abs = new Abstract(0, text, Type.TESTING, lemmatizer);

            logger.info("Text: " + abs.getOriginalText() + "\n");
            
            // Determine keyword extraction method
            GraphBasedKeywordExtractor keywordExtractor;
            if (rankUpProperties.keywordExtractionMethod == null ||
                    rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.TEXTRANK) {
                
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
            else if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.RAKE) {
                keywordExtractor = new GraphBasedKeywordExtractor(
                        rankUpProperties,
                        rake);
            }
            else {
                logger.error("Wrong Keyword Extraction Method");
                return;
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
            List<KeyPhrase> tfidfKeyphrases = getAndInsertFeatureKeyphrases(
                    rankUpProperties, Feature.TFIDF_STEMMED,
                    originalKeyphrases, GET_TFIDF_KEYPHRASES);

            // Get RIDF KeyPhrases
            List<KeyPhrase> ridfKeyphrases = getAndInsertFeatureKeyphrases(
                    rankUpProperties, Feature.RIDF_STEMMED,
                    originalKeyphrases, GET_RIDF_KEYPHRASES);


            // Get Clusteredness KeyPhrases
            List<KeyPhrase> clusterednessKeyphrases = getAndInsertFeatureKeyphrases(
                    rankUpProperties, Feature.CLUSTEREDNESS_STEMMED,
                    originalKeyphrases, GET_CLUSTEREDNESS_KEYPHRASES);

            // Get RAKE KeyPhrases
            List<KeyPhrase> rakeKeyphrases= getAndInsertFeatureKeyphrases(
                    rankUpProperties, Feature.RAKE_STEMMED,
                    originalKeyphrases, GET_RAKE_KEYPHRASES);

            // Print and insert TextRank KeyPhrases into database
            if (GET_ORIGINAL_KEYPHRASES) {
                printKeyPhrases(originalKeyphrases, 
                        rankUpProperties.keywordExtractionMethod.toString());
            }

            logger.info("");
            logger.info("RankUp completed for this text!");
            logger.info("");
        }
        catch (FileNotFoundException fnfe) {
            logger.error("Properties file not found! " + fnfe.getMessage());
        }
        catch (IOException ioe) {
            logger.error("IO Exception! " + ioe.getMessage());
        }
        catch (Exception e) {
            logger.error("Exception in RankUp Main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}