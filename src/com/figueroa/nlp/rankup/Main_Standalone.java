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
import org.apache.commons.math.util.MathUtils;
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
    public final static int PROPERTIES_FILES_TO_PROCESS = 1; // 0 for no limit
    public final static int PROPERTIES_FILE_NUMBER_TO_PROCESS = 0; // 0 to ignore
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
    public final static String POS_TAGGER_MODEL =
            "./pos_models/left3words-wsj-0-18.tagger";
    public final static String POS_TAGGER_CONFIG =
            "./pos_models/left3words-wsj-0-18.tagger2.props";
    public final static String TAG_SEPARATOR = "_";
    public final static String WN_HOME = "./lib/WordNet-3.0";
    private static POSTagger posTagger;
    private static Lemmatizer lemmatizer;

    // TextRank tools
    public final static String log4j_conf = "./res/log4j.properties";
    public final static String res_path = "./res";
    public final static String lang_code = "en";
    public static LanguageModel languageModel;
    
    // Python and RAKE tools
    private static Rake rake;
    
    private static RankUp rankUp;

    // Stopwords
    private static Stopwords stopwords;

    /**
     * Allows the program to pause and wait for user to press any key to continue
     */
    private static void systemPause() {
        System.out.println("Press Any Key To Continue...");
        new java.util.Scanner(System.in).nextLine();
    }

    /**
     * Loads the main components required by RankUp
     * @throws Exception 
     */
    private static void loadComponents()
            throws Exception {
        
        logger.info("Loading components...");
        
        // Load Stopwords
        stopwords = new Stopwords();
        
        // Load TextRank components
        languageModel = LanguageModel.buildLanguage(res_path, lang_code);
        WordNet.buildDictionary(res_path, lang_code);
        //textRank = new TextRank(logger, stopwords, languageModel);
        
        // Load DatabaseManager
        databaseManager =
            new DatabaseManager(DB_CLASS_NAME, CONNECTION_STRING, USER, PASSWORD);
        
        // Load AbstractManager
        abstractManager = new AbstractManager(databaseManager);
        
        // Load POSTagger
        posTagger =
            new POSTagger(POS_TAGGER_MODEL, TAG_SEPARATOR);
        
        // Load Lemmatizer
        lemmatizer = new Lemmatizer(WN_HOME, posTagger);
        
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
            int rankUpPropertiesProcessedCount, 
            boolean GET_KEYPHRASES) throws Exception {
        
        String featureString = PhraseFeatures.getShortFeatureString(feature);
        
        List<KeyPhrase> keyphrases = new ArrayList<>();
        if (rankUpPropertiesProcessedCount < 1 && GET_KEYPHRASES) {

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
    public static void main(String[] args) {
        /*
         * Arg 0: Properties file
         * Arg 1: Whether to use a sample of the data or not
         * Arg 2: Extracted Keywords table
         * Arg 3: Debugging Information table
         * Arg 4: Override DebugLevel
         */
        
        if (args.length < 4) {
            System.err.println("Incorrect argument list.");
            System.err.println("Usage:   RankUp PROPERTIES_FILE SAMPLE_OR_NOT EXTRACTED_KEYWORDS_TABLE DEBUGGING_INFORMATION_TABLE [OVERRIDE_DEBUG_LEVEL]");
            System.err.println("Example: RankUp ./properties/ieee.properties true RankUp_Extracted_Keyword RankUp_Debug_Sampled [INFO]");
            return;
        }
        
        try {
            // Load RankUp Properties
            final String propertiesFile = args[0];
            List<RankUpProperties> rankUpPropertiesList = 
                    loadRankUpProperties(propertiesFile);
            
            String abstractSource = rankUpPropertiesList.get(0).abstractSource;

            logger.info("Starting RankUp...");
            logger.info("Properties file: " + propertiesFile);

            loadComponents();
            
            // Retrieve all abstracts
            logger.info("Retrieving abstracts...");
            List<Abstract> allAbstracts =
                    abstractManager.retrieveAbstracts(
                    rankUpPropertiesList.get(0).abstractType,
                    abstractSource, 
                    ABSTRACT_TABLE);
            
            int rankUpPropertiesCount = 0;
            int rankUpPropertiesProcessedCount = 0;
            for (RankUpProperties rankUpProperties : rankUpPropertiesList) {
                rankUpPropertiesCount++;
                
                if (rankUpPropertiesCount > PROPERTIES_FILES_TO_PROCESS) {
                    break;
                }
                
                // Check if there is a specific properties file to process
                if (PROPERTIES_FILES_TO_PROCESS == 1 && 
                        PROPERTIES_FILE_NUMBER_TO_PROCESS > 0) {
                    if (rankUpPropertiesCount != PROPERTIES_FILE_NUMBER_TO_PROCESS) {
                        continue;
                    }
                }
                
                logger.info("***************************************");
                logger.info("RankUp Properties (" + rankUpPropertiesCount +
                        "/" + rankUpPropertiesList.size() + "): " + 
                        rankUpProperties.propertiesFileName + "\n" +
                        rankUpProperties.toString());
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

                logger.info("Properties File: " + rankUpPropertiesCount + "/" +
                        rankUpPropertiesList.size());
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
                List<KeyPhrase> rankUpKeyphrases;
                List<KeyPhrase> originalKeyphrases;
                try {
                    rankUpKeyphrases = 
                            rankUp.runRankUp(abs, PRINT_GEPHI_GRAPHS, keywordExtractor);
                    originalKeyphrases = rankUp.getOriginalKeyphraseSet();
                }
                catch (Exception exception) {
                    logger.error("Exception in runRankUp: " + 
                            exception.getMessage());
                    exception.printStackTrace();
                    continue;
                }

                // Get TFIDF KeyPhrases
                List<KeyPhrase> tfidfKeyphrases = getAndInsertFeatureKeyphrases(
                        rankUpProperties, Feature.TFIDF_STEMMED,
                        originalKeyphrases, rankUpPropertiesProcessedCount, 
                        GET_TFIDF_KEYPHRASES);

                // Get RIDF KeyPhrases
                List<KeyPhrase> ridfKeyphrases = getAndInsertFeatureKeyphrases(
                        rankUpProperties, Feature.RIDF_STEMMED,
                        originalKeyphrases, rankUpPropertiesProcessedCount, 
                        GET_RIDF_KEYPHRASES);


                // Get Clusteredness KeyPhrases
                List<KeyPhrase> clusterednessKeyphrases = getAndInsertFeatureKeyphrases(
                        rankUpProperties, Feature.CLUSTEREDNESS_STEMMED,
                        originalKeyphrases, rankUpPropertiesProcessedCount, 
                        GET_CLUSTEREDNESS_KEYPHRASES);

                // Get RAKE KeyPhrases
                List<KeyPhrase> rakeKeyphrases= getAndInsertFeatureKeyphrases(
                        rankUpProperties, Feature.RAKE_STEMMED,
                        originalKeyphrases, rankUpPropertiesProcessedCount, 
                        GET_RAKE_KEYPHRASES);

                // Print and insert TextRank KeyPhrases into database
                if (rankUpPropertiesProcessedCount < 1 && GET_ORIGINAL_KEYPHRASES) {
                    printKeyPhrases(originalKeyphrases, 
                            rankUpProperties.keywordExtractionMethod.toString());
                }

                // RankUp correctness for this abstract
                Double keyphraseFinalTextRankScoreCorrectness = 
                        rankUp.getKeyPhraseGraph().getKeyphraseFinalTextRankScoreCorrectness();
                Double textRankNodeScoreCorrectness = 
                        rankUp.getKeyPhraseGraph().getTextRankNodeScoreCorrectness();

                logger.debug("");
                logger.debug("Keyphrase final TextRank score correctness (this abstract): " + 
                        (keyphraseFinalTextRankScoreCorrectness != null ?
                                MathUtils.round(keyphraseFinalTextRankScoreCorrectness, 2) :
                                ""));
                logger.debug("TextRank node score correctness (this abstract): " + 
                        (textRankNodeScoreCorrectness != null ?
                                MathUtils.round(textRankNodeScoreCorrectness, 2) :
                                ""));

                // Insert debug information into database
                logger.trace("Iterations: " + rankUp.getRankUpIterations());

                logger.info("");
                logger.info("RankUp completed for this text!");
                logger.info("");
            }
        }
        catch (NumberFormatException nfe) {
            logger.error("Sample percentage must be decimal number "
                        + "between 0 and 1 (e.g. 0.25)!");
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