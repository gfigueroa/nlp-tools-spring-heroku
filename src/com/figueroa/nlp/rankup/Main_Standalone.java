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
import com.figueroa.util.ExceptionLogger;
import com.figueroa.util.ExceptionLogger.DebugLevel;
import com.figueroa.util.PrecisionAnalyzer;

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

    // Runtime options
    // Debugging
    public static DebugLevel DEBUG_LEVEL = DebugLevel.INFO;
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
    
    // Loggers
    public final static String EXCEPTION_LOGGER_DIR =
            "." + File.separator + "logs" + File.separator
            + "exception_log_" + ExceptionLogger.now() + ".log";
    public final static ExceptionLogger exceptionLogger = 
            new ExceptionLogger(EXCEPTION_LOGGER_DIR, DEBUG_LEVEL);
    
    public final static String DATA_LOGGER_DIR =
            "." + File.separator + "logs" + File.separator
            + "data_log_" + ExceptionLogger.now() + ".log";
    public final static ExceptionLogger dataLogger = 
            new ExceptionLogger(DATA_LOGGER_DIR, DEBUG_LEVEL);
    
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
        
        exceptionLogger.debug("Loading components...", DebugLevel.INFO);
        
        // Load Stopwords
        stopwords = new Stopwords();
        
        // Load TextRank components
        languageModel = LanguageModel.buildLanguage(res_path, lang_code);
        WordNet.buildDictionary(res_path, lang_code);
        //textRank = new TextRank(exceptionLogger, stopwords, languageModel);
        
        // Load DatabaseManager
        databaseManager =
            new DatabaseManager(DB_CLASS_NAME, CONNECTION_STRING, USER, PASSWORD);
        
        // Load AbstractManager
        abstractManager = new AbstractManager(databaseManager, exceptionLogger);
        
        // Load POSTagger
        posTagger =
            new POSTagger(POS_TAGGER_MODEL, TAG_SEPARATOR);
        
        // Load Lemmatizer
        lemmatizer = new Lemmatizer(WN_HOME, posTagger);
        
        // Load and set up RAKE
        rake = new Rake(exceptionLogger);
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

            Main.printKeyPhrases(keyphrases, featureString, false);

            // Get KeyPhraseGraph
            if (PRINT_SETS) {
                KeyPhraseGraph graph = new KeyPhraseGraph(
                        exceptionLogger, 
                        keyphrases,
                        rankUpProperties.setAssignmentApproach,
                        rankUpProperties.featureLowerBound, 
                        rankUpProperties.featureUpperBound,
                        feature);
                exceptionLogger.debug("**** " + featureString + " Sets ****", 
                        DebugLevel.DEBUG);
                graph.printFeatureSet(SetLevel.LOW, DebugLevel.DEBUG);
                graph.printFeatureSet(SetLevel.MID, DebugLevel.DEBUG);
                graph.printFeatureSet(SetLevel.HIGH, DebugLevel.DEBUG);
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

            exceptionLogger.debug("Starting RankUp...", DebugLevel.INFO);
            exceptionLogger.debug("Properties file: " + propertiesFile, 
                    DebugLevel.INFO);

            loadComponents();
            
            // Retrieve all abstracts
            exceptionLogger.debug("Retrieving abstracts...", DebugLevel.INFO);
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
                
                exceptionLogger.debug("***************************************", 
                        DebugLevel.INFO);
                exceptionLogger.debug("RankUp Properties (" + rankUpPropertiesCount +
                        "/" + rankUpPropertiesList.size() + "): " + 
                        rankUpProperties.propertiesFileName + "\n" +
                        rankUpProperties.toString(),
                        DebugLevel.INFO);
                exceptionLogger.debug("", DebugLevel.INFO);

                rankUp = new RankUp(exceptionLogger, 
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

                exceptionLogger.debug("Properties File: " + rankUpPropertiesCount + "/" +
                        rankUpPropertiesList.size(), DebugLevel.INFO);
                exceptionLogger.debug("Text: " + abs.getOriginalText() + "\n", 
                        DebugLevel.INFO);

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
                        textRank = new TextRank(exceptionLogger, stopwords, languageModel);
                        abs.setTextRank(textRank);
                    }
                     
                    keywordExtractor = new GraphBasedKeywordExtractor(
                            exceptionLogger, 
                            rankUpProperties,
                            textRank);
                }
                else if (rankUpProperties.keywordExtractionMethod == KeywordExtractionMethod.RAKE) {
                    keywordExtractor = new GraphBasedKeywordExtractor(
                            exceptionLogger,
                            rankUpProperties,
                            rake);
                }
                else {
                    exceptionLogger.debug("Wrong Keyword Extraction Method", DebugLevel.ERROR);
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
                    exceptionLogger.debug("Exception in runRankUp: " + 
                            exception.getMessage(), DebugLevel.ERROR);
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
                    Main.printKeyPhrases(originalKeyphrases, 
                            rankUpProperties.keywordExtractionMethod.toString(), 
                            false);
                }

                // Print and insert RankUp KeyPhrases into database
                if (GET_RANKUP_KEYPHRASES) {
                    Main.printKeyPhrases(rankUpKeyphrases, "RankUp", false);
                }

                // Print the real rankUpKeyphrases
                abstractManager.printRealKeywords(abs);

                // Call results
                if (PRINT_RESULTS) {
                    PrecisionAnalyzer.results(
                            abs,
                            tfidfKeyphrases,
                            originalKeyphrases,
                            rankUpKeyphrases, 
                            PARTIAL_MATCHING, 
                            exceptionLogger, dataLogger, abstractManager, lemmatizer);
                }

                // RankUp correctness for this abstract
                Double keyphraseFinalTextRankScoreCorrectness = 
                        rankUp.getKeyPhraseGraph().getKeyphraseFinalTextRankScoreCorrectness();
                Double textRankNodeScoreCorrectness = 
                        rankUp.getKeyPhraseGraph().getTextRankNodeScoreCorrectness();

                exceptionLogger.debug("", DebugLevel.DEBUG);
                exceptionLogger.debug("Keyphrase final TextRank score correctness (this abstract): " + 
                        (keyphraseFinalTextRankScoreCorrectness != null ?
                                MathUtils.round(keyphraseFinalTextRankScoreCorrectness, 2) :
                                ""), DebugLevel.DEBUG);
                exceptionLogger.debug("TextRank node score correctness (this abstract): " + 
                        (textRankNodeScoreCorrectness != null ?
                                MathUtils.round(textRankNodeScoreCorrectness, 2) :
                                ""), DebugLevel.DEBUG);

                // Insert debug information into database
                exceptionLogger.debug("Iterations: " + rankUp.getRankUpIterations(), 
                        DebugLevel.DETAIL);

                exceptionLogger.debug("", DebugLevel.INFO);
                exceptionLogger.debug("RankUp completed for this text!", 
                        DebugLevel.INFO);
                exceptionLogger.debug("", DebugLevel.INFO);
            }
        }
        catch (NumberFormatException nfe) {
            exceptionLogger.debug("Sample percentage must be decimal number "
                        + "between 0 and 1 (e.g. 0.25)!", DebugLevel.ERROR);
        }
        catch (FileNotFoundException fnfe) {
            exceptionLogger.debug("Properties file not found! " + fnfe.getMessage(),
                    DebugLevel.ERROR);
        }
        catch (IOException ioe) {
            exceptionLogger.debug("IO Exception! " + ioe.getMessage(),
                    DebugLevel.ERROR);
        }
        catch (Exception e) {
            exceptionLogger.debug("Exception in RankUp Main: " + e.getMessage(),
                    DebugLevel.ERROR);
            e.printStackTrace();
        }
    }
}