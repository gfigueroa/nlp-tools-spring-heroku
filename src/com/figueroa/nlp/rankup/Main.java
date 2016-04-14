package com.figueroa.nlp.rankup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.math.util.MathUtils;
import com.figueroa.nlp.rankup.ErrorDetector.ErrorDetectingApproach;
import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.KeywordExtractionMethod;
import com.figueroa.nlp.KeyPhrase.ScoreDirection;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetLevel;
import com.figueroa.nlp.rankup.PhraseFeatures.Feature;
import com.figueroa.nlp.POSTagger;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.nlp.Stopwords;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.textrank.LanguageModel;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.WordNet;
import com.figueroa.util.Abstract;
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
public class Main {

    // Runtime options
    // Debugging
    public static DebugLevel DEBUG_LEVEL = DebugLevel.INFO;
    public final static int ABSTRACTS_TO_PROCESS = 1; // 0 for no limit
    public final static int ABSTRACT_ID_TO_PROCESS = 6825; // 0 to ignore
    public final static int PROPERTIES_FILES_TO_PROCESS = 1; // 0 for no limit
    public final static int PROPERTIES_FILE_NUMBER_TO_PROCESS = 0; // 0 to ignore
    public final static boolean PRINT_RESULTS = true;
    public final static boolean PRINT_SETS = false;
    public final static boolean PRINT_GEPHI_GRAPHS = false;
    public final static boolean INSERT_KEYPHRASES_INTO_DATABASE = false;
    public final static boolean INSERT_DEBUGGING_INFORMATION = false;
    // Keyphrase sets to obtain
    public final static boolean GET_TFIDF_KEYPHRASES = true;
    public final static boolean GET_RIDF_KEYPHRASES = true;
    public final static boolean GET_CLUSTEREDNESS_KEYPHRASES = true;
    public final static boolean GET_RAKE_KEYPHRASES = true;
    public final static boolean GET_ORIGINAL_KEYPHRASES = true;
    public final static boolean GET_RANKUP_KEYPHRASES = true;
    // Changes and bug fixes
    public final static boolean PARTIAL_MATCHING = true;
    public final static boolean MINMAX_MID_BUG_FIX = true;
    public final static boolean CORRECT_NEGATIVE_WEIGHTS = true;
    public final static boolean DENORMALIZE_MODIFICATION_VALUE = false;
    public final static boolean USE_DIFFERENTIAL_CONVERGENCE = false;
    
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
    
    // Abstract Source sample percentages
    public static final Map<String, Double> abstractSourcePercentages;
    static
    {
        abstractSourcePercentages = new HashMap<>();
        abstractSourcePercentages.put("Hulth 2003", 0.1);
        abstractSourcePercentages.put("IEEE Explore", 0.1);
        abstractSourcePercentages.put("Journal of Applied Physics", 0.05);
        abstractSourcePercentages.put("Journal of Psychiatric Practice", 0.4);
        abstractSourcePercentages.put("Kaggle", 0.02);
        abstractSourcePercentages.put("VLDB Journal", 0.4);
        abstractSourcePercentages.put("finalpool_type0_score=3", 0.05);
        abstractSourcePercentages.put("finalpool_type0_score>=2 AND score<3", 0.05);
    }

    /**
     * Allows the program to pause and wait for user to press any key to continue
     */
    protected static void systemPause() {
        System.out.println("Press Any Key To Continue...");
        new java.util.Scanner(System.in).nextLine();
    }

    /**
     * Loads the main components required by RankUp
     * @throws Exception 
     */
    protected static void loadComponents()
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
    protected static List<RankUpProperties> loadRankUpProperties(String propertiesFileString) 
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
    public static void printKeyPhrases(List<KeyPhrase> keyPhrases, String method,
            boolean writeToDataLog) {

        if (!writeToDataLog) {
            exceptionLogger.debug("", DebugLevel.DEBUG);
            exceptionLogger.debug("***********" + method + "************", DebugLevel.DEBUG);
            exceptionLogger.debug("*Size: " + keyPhrases.size() + "*", DebugLevel.DEBUG);
            for (KeyPhrase keyPhrase : keyPhrases) {
                String features = keyPhrase.getFeatures() != null ?
                        keyPhrase.getFeatures().toString() : "";
                exceptionLogger.debug(keyPhrase.toString() + "\t" + features,
                        DebugLevel.DEBUG);
            }
            exceptionLogger.debug("", DebugLevel.DEBUG);
        }
        else {
            dataLogger.writeToLog("");
            dataLogger.writeToLog("***********" + method + "************");
            for (KeyPhrase keyPhrase : keyPhrases) {
                String features = keyPhrase.getFeatures() != null ?
                        keyPhrase.getFeatures().toString() : "";
                dataLogger.writeToLog(keyPhrase.toString() + " - " + features);
            }
            dataLogger.writeToLog("");
        }
    }

    /**
     * Insert the given keyphrases into the database
     * @param abstractId
     * @param keyPhraseList
     * @param properties
     * @param method
     * @param extractedKeywordsTable 
     */
    private static void insertKeyphrasesIntoDatabase(
            int abstractId,
            List<KeyPhrase> keyPhraseList,
            RankUpProperties properties,
            String method,
            String extractedKeywordsTable) {

        String insertString;

        for (KeyPhrase keyphrase : keyPhraseList) {

            try {
                // Clean for SQL Syntax
                // Replace single quotes
                String cleanKeyphrase =
                        keyphrase.getText().replaceAll("'", "\\\\'");
                // Replace double quotes
                cleanKeyphrase = cleanKeyphrase.replaceAll("\"", "\\\\\"");
                
                double score = 0;
                if (method.equalsIgnoreCase("TextRank") ||
                        (method.equalsIgnoreCase("RAKE") && 
                        properties.keywordExtractionMethod == KeywordExtractionMethod.RAKE)) { // OTRS
                    score = (!Double.isNaN(keyphrase.getOriginalTextRankScore()) ? 
                            keyphrase.getOriginalTextRankScore() : 0);
                }
                else if (method.equalsIgnoreCase("RAKE")) { // Any other keyword extraction method besides RAKE
                    score = (!Double.isNaN(keyphrase.getFeatures().rakeStemmed) ? 
                            keyphrase.getFeatures().rakeStemmed : 0);
                }
                else if (method.equalsIgnoreCase("RankUp")) { // FTRS
                    score = (!Double.isNaN(keyphrase.getFinalTextRankScore()) ? 
                            keyphrase.getFinalTextRankScore() : 0);
                }
                else if (method.equalsIgnoreCase("TFIDF")) { // TFIDF_STEM
                    score = (!Double.isNaN(keyphrase.getFeatures().tfidfStemmed) ? 
                            keyphrase.getFeatures().tfidfStemmed : 0);
                }
                else if (method.equalsIgnoreCase("RIDF")) { // RIDF_STEM
                    score = (!Double.isNaN(keyphrase.getFeatures().ridfStemmed) ? 
                            keyphrase.getFeatures().ridfStemmed : 0);
                }
                else if (method.equalsIgnoreCase("CLUSTEREDNESS")) { // CLUST_STEM
                    score = (!Double.isNaN(keyphrase.getFeatures().clusterednessStemmed) ? 
                            keyphrase.getFeatures().clusterednessStemmed : 0);
                }
                else {
                    exceptionLogger.debug("Wrong method '" + method + "' " +
                            "in insertKeyphrasesIntoDatabase()", DEBUG_LEVEL);
                    System.exit(0);
                }

                insertString =
                        "INSERT INTO " + extractedKeywordsTable + "(" +
                        "Abstract_Id, " +
                        "Keyword, " +
                        "Learning_Rate, " +
                        "SE_Threshold, " +
                        "Convergence_Scheme, " +
                        "Score, " +
                        "Method, " +
                        "Use_Whole_TR_Graph, " +
                        "Postprocess, " +
                        "Error_Detecting_Approach, " +
                        "Expected_Score_Value, " +
                        "Feature_Lower_Bound, " +
                        "Feature_Upper_Bound, " +
                        "Set_Assignment_Approach, " +
                        "Convergence_Rule, " +
                        "Revert_Graphs, " +
                        "Keyword_Extraction_Method" +
                        ")\n" +
                        "VALUES(" +
                        abstractId + ", " +
                        "'" + cleanKeyphrase + "', " +
                        properties.learningRate + ", " +
                        properties.standardErrorThreshold + ", " +
                        "'" + properties.convergenceScheme + "', " +
                        score + ", " +
                        "'" + method + "', " +
                        properties.useWholeTextRankGraph + ", " +
                        properties.postprocess + ", " +
                        "'" + properties.errorDetectingApproach + "', " +
                        "'" + properties.expectedScoreValue + "', " +
                        properties.featureLowerBound + ", " +
                        properties.featureUpperBound + ", " +
                        "'" + properties.setAssignmentApproach + "', " +
                        "'" + properties.convergenceRule + "', " +
                        properties.revertGraphs + ", '" +
                        properties.keywordExtractionMethod + "'" +
                        ")";

                exceptionLogger.debug("INSERT SQL: " + insertString, DebugLevel.DETAIL);

                databaseManager.executeUpdate(insertString);
            }
            catch (SQLException e) {
                exceptionLogger.debug("SQL exception in insertKeyphrasesIntoDatabase: " + 
                        e.getMessage(), DebugLevel.WARNING);
            }
        }
    }
    
    /**
     * Insert RankUp debugging information into the database
     * @param abstractId
     * @param rankUp
     * @param debuggingTable
     */
    private static void insertRankUpDebuggingInformationIntoDatabase(
            int abstractId,
            RankUp rankUp,
            String debuggingTable) {

        String insertString;

        try {
            RankUpProperties properties = rankUp.getRankUpProperties();
            
            Double lowSetMinFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.LOW) : -1;
            Double lowSetMaxFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.LOW) : -1;
            Double lowSetMeanFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.LOW) : -1;
            Double lowSetMinScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.LOW) : -1;
            Double lowSetMaxScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.LOW) : -1;
            Double lowSetMeanScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.LOW)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.LOW) : -1;
            
            Double midSetMinFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.MID) : -1;
            Double midSetMaxFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.MID) : -1;
            Double midSetMeanFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.MID) : -1;
            Double midSetMinScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.MID) : -1;
            Double midSetMaxScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.MID) : -1;
            Double midSetMeanScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.MID)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.MID) : -1;
            
            Double highSetMinFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMin(SetLevel.HIGH) : -1;
            Double highSetMaxFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMax(SetLevel.HIGH) : -1;
            Double highSetMeanFeature = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMean(SetLevel.HIGH) : -1;
            Double highSetMinScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMinTextRankScore(SetLevel.HIGH) : -1;
            Double highSetMaxScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMaxTextRankScore(SetLevel.HIGH) : -1;
            Double highSetMeanScore = !Double.isNaN(rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.HIGH)) ?
                    rankUp.getKeyPhraseGraph().getFeatureSetMeanTextRankScore(SetLevel.HIGH) : -1;
            
            insertString =
                    "INSERT INTO " + debuggingTable + "(" +
                    "Abstract_Id, " +
                    "Learning_Rate, " +
                    "SE_Threshold, " +
                    "Convergence_Scheme, " +
//                    "Weight_Updating_Scheme, " +
                    "Use_Whole_TR_Graph, " +
                    "Postprocess, " +
                    "Error_Detecting_Approach, " +
                    "Expected_Score_Value, " +
                    "Feature_Lower_Bound, " +
                    "Feature_Upper_Bound, " +
                    "Set_Assignment_Approach, " +
                    "Convergence_Rule, " +
                    "Revert_Graphs, " +
                    "Final_Score_Correctness, " +
                    "Node_Score_Correctness, " +
                    "RankUp_Iterations, " +
                    "Low_Set_Size, " +
                    "Low_Set_Min_Feature, " +
                    "Low_Set_Max_Feature, " +
                    "Low_Set_Mean_Feature, " +
                    "Low_Set_Min_Score, " +
                    "Low_Set_Max_Score, " +
                    "Low_Set_Mean_Score, " +
                    "Mid_Set_Size, " +
                    "Mid_Set_Min_Feature, " +
                    "Mid_Set_Max_Feature, " +
                    "Mid_Set_Mean_Feature, " +
                    "Mid_Set_Min_Score, " +
                    "Mid_Set_Max_Score, " +
                    "Mid_Set_Mean_Score, " +
                    "High_Set_Size, " +
                    "High_Set_Min_Feature, " +
                    "High_Set_Max_Feature, " +
                    "High_Set_Mean_Feature, " +
                    "High_Set_Min_Score, " +
                    "High_Set_Max_Score, " +
                    "High_Set_Mean_Score" +
                    ")\n" +
                    "VALUES(" +
                    abstractId + ", " +
                    properties.learningRate + ", " +
                    properties.standardErrorThreshold + ", " +
                    "'" + properties.convergenceScheme + "', " +
//                    "'" + properties.weightUpdatingScheme + "', " +
                    properties.useWholeTextRankGraph + ", " +
                    properties.postprocess + ", " +
                    "'" + properties.errorDetectingApproach + "', " +
                    "'" + properties.expectedScoreValue + "', " +
                    properties.featureLowerBound + ", " +
                    properties.featureUpperBound + ", " +
                    "'" + properties.setAssignmentApproach + "', " +
                    "'" + properties.convergenceRule + "', " +
                    properties.revertGraphs + ", " +
                    rankUp.getKeyPhraseGraph().getKeyphraseFinalTextRankScoreCorrectness() + ", " +
                    rankUp.getKeyPhraseGraph().getTextRankNodeScoreCorrectness() + ", " +
                    rankUp.getRankUpIterations() + ", " + 
                    rankUp.getKeyPhraseGraph().getFeatureSet(SetLevel.LOW).size() + ", " +
                    lowSetMinFeature + ", " +
                    lowSetMaxFeature + ", " + 
                    lowSetMeanFeature + ", " +
                    lowSetMinScore + ", " +
                    lowSetMaxScore + ", " +
                    lowSetMeanScore + ", " +
                    rankUp.getKeyPhraseGraph().getFeatureSet(SetLevel.MID).size() + ", " +
                    midSetMinFeature + ", " +
                    midSetMaxFeature + ", " + 
                    midSetMeanFeature + ", " +
                    midSetMinScore + ", " +
                    midSetMaxScore + ", " +
                    midSetMeanScore + ", " +
                    rankUp.getKeyPhraseGraph().getFeatureSet(SetLevel.HIGH).size() + ", " +
                    highSetMinFeature + ", " +
                    highSetMaxFeature + ", " + 
                    highSetMeanFeature + ", " +
                    highSetMinScore + ", " +
                    highSetMaxScore + ", " +
                    highSetMeanScore +
                    ")";

            exceptionLogger.debug("INSERT SQL: " + insertString, DebugLevel.DETAIL);

            databaseManager.executeUpdate(insertString);
        }
        catch (SQLException e) {
            exceptionLogger.debug("SQL exception in insertRankUpDebuggingInformationIntoDatabase: " + 
                    e.getMessage(), DebugLevel.WARNING);
        }
    }
    
    /**
     * Gets a list of KeyPhrases ordered according to the given Feature
     * @param keyphrases
     * @param feature
     * @return
     * @throws Exception 
     */
    protected static List<KeyPhrase> getFeatureKeyphrases(List<KeyPhrase> keyphrases,
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
    protected static List<KeyPhrase> getAndInsertFeatureKeyphrases(
            Abstract abs,
            RankUpProperties textRankProperties,
            RankUpProperties rankUpProperties,
            final Feature feature,
            List<KeyPhrase> textRankKeyphrases,
            int rankUpPropertiesProcessedCount, 
            boolean GET_KEYPHRASES,
            String extractedKeywordsTable,
            RankUp rankUp) throws Exception {
        
        String featureString = PhraseFeatures.getShortFeatureString(feature);
        
        List<KeyPhrase> keyphrases = new ArrayList<>();
        if (rankUpPropertiesProcessedCount < 1 && GET_KEYPHRASES) {

            // Check if features have really been set
            if (!textRankKeyphrases.isEmpty() && textRankKeyphrases.get(0).getFeatures() == null) {
                rankUp.setKeyPhraseFeatures(textRankKeyphrases, abs, true);
            }
            
            keyphrases =
                    getFeatureKeyphrases(textRankKeyphrases, feature);

            printKeyPhrases(keyphrases, featureString, false);

            // Insert into database
            if (INSERT_KEYPHRASES_INTO_DATABASE && 
                    !abstractManager.keywordsExistInDatabase(abs, 
                    textRankProperties, featureString, extractedKeywordsTable)) {

                exceptionLogger.debug("Inserting " + featureString +
                        " KeyPhrases into database...", DebugLevel.INFO);
                insertKeyphrasesIntoDatabase(
                        abs.getAbstractId(),
                        keyphrases,
                        textRankProperties, 
                        featureString,
                        extractedKeywordsTable);
            }

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
            
            final boolean useSampledData = Boolean.parseBoolean(args[1]);
            final double samplePercentage;
            if (useSampledData) {
                samplePercentage = abstractSourcePercentages.get(abstractSource);
            }
            else {
                // Use 0.1 for Kaggle
                if (abstractSource.equalsIgnoreCase("Kaggle")) {
                    samplePercentage = 0.1;
                }
                else {
                    samplePercentage = 1;
                }
            }

            final String temporaryExtractedKeywordsTable = args[2];
            
            final String debuggingTable = args[3];
            
            if (args.length > 4) {
                Main.DEBUG_LEVEL = ExceptionLogger.getDebugLevelFromString(args[4]);
            }

            exceptionLogger.debug("Starting RankUp...", DebugLevel.INFO);
            exceptionLogger.debug("Properties file: " + propertiesFile, 
                    DebugLevel.INFO);
            exceptionLogger.debug("Sample percentage: " + samplePercentage, 
                    DebugLevel.INFO);
            exceptionLogger.debug("Extracted keywords table: " + temporaryExtractedKeywordsTable, 
                    DebugLevel.INFO);

            loadComponents();

            // Retrieve all abstracts
            exceptionLogger.debug("Retrieving abstracts...", DebugLevel.INFO);
            List<Abstract> allAbstracts =
                    abstractManager.retrieveAbstracts(
                    rankUpPropertiesList.get(0).abstractType,
                    abstractSource, 
                    ABSTRACT_TABLE);
            
            ArrayList<Abstract> testingAbstracts = new ArrayList<>();
            // Select sample from abstracts or only one abstract
            if (ABSTRACTS_TO_PROCESS == 1 && ABSTRACT_ID_TO_PROCESS > 0) {
                Abstract abs = abstractManager.retrieveAbstract(ABSTRACT_ID_TO_PROCESS);
                testingAbstracts.add(abs);
            }
            else {
                int sampleSize = (int) (allAbstracts.size() * samplePercentage);
                int skip = allAbstracts.size() / sampleSize;
                for (int i = 0; i < sampleSize; i++) {
                    testingAbstracts.add(allAbstracts.get(i * skip));
                }
            }
            
            int rankUpPropertiesCount = 0;
            int rankUpPropertiesProcessedCount = 0;
            for (RankUpProperties rankUpProperties : rankUpPropertiesList) {
                rankUpPropertiesCount++;
                
                // Check if there is a specific properties file to process
                if (PROPERTIES_FILES_TO_PROCESS == 1 && 
                        PROPERTIES_FILE_NUMBER_TO_PROCESS > 0) {
                    if (rankUpPropertiesCount != PROPERTIES_FILE_NUMBER_TO_PROCESS) {
                        continue;
                    }
                }
                
                // Save RAKE keywords to another table
                String extractedKeywordsTable;
                if (temporaryExtractedKeywordsTable.equals("RankUp_Extracted_Keyword_Final2") && 
                        rankUpProperties.errorDetectingApproach == ErrorDetectingApproach.RAKE) {
                    extractedKeywordsTable = "RankUp_Extracted_Keyword_Final2_Rake";
                }
                else {
                    extractedKeywordsTable = temporaryExtractedKeywordsTable;
                }
                
                // Load TextRank Properties
                RankUpProperties nonRankUpProperties = new RankUpProperties(
                        "TextRank properties",
                        rankUpProperties.abstractSource,
                        rankUpProperties.abstractType,
                        rankUpProperties.useWholeTextRankGraph,
                        false,
                        null,
                        null,
                        -1,
                        -1,
                        null,
                        -1,
                        -1,
                        null,
//                        null,
                        null,
                        false,
                        rankUpProperties.keywordExtractionMethod);
                
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

                int overallKeyphraseFinalTextRankScoreCorrectnessCount = 0;
                int overallTextRankNodeScoreCorrectnessCount = 0;
                double overallKeyphraseFinalTextRankScoreCorrectness = 0;
                double overallTextRankNodeScoreCorrectness = 0;
                int abstractsProcessed = 0;
                ArrayList<Integer> goodExamples = new ArrayList<>();
                for (int i = 0; i < testingAbstracts.size() && (i < ABSTRACTS_TO_PROCESS ||
                        ABSTRACTS_TO_PROCESS == 0); i++) {
                    
                    Abstract abs = testingAbstracts.get(i);
                    int abstractId = abs.getAbstractId();
                    
                    // Check that this abstract hasn't been processed with these properties
                    if (INSERT_KEYPHRASES_INTO_DATABASE &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                            rankUpProperties, "RankUp", extractedKeywordsTable) &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                                nonRankUpProperties, 
                                rankUpProperties.keywordExtractionMethod.toString(), 
                                extractedKeywordsTable) ||
                                !GET_ORIGINAL_KEYPHRASES) &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                                nonRankUpProperties, "TFIDF", extractedKeywordsTable) ||
                                !GET_TFIDF_KEYPHRASES) &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                                nonRankUpProperties, "RIDF", extractedKeywordsTable) ||
                                !GET_RIDF_KEYPHRASES) &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                                nonRankUpProperties, "clusteredness", extractedKeywordsTable) ||
                                !GET_CLUSTEREDNESS_KEYPHRASES) &&
                            (abstractManager.keywordsExistInDatabase(abs, 
                                nonRankUpProperties, "RAKE", extractedKeywordsTable) ||
                                !GET_RAKE_KEYPHRASES)
                            )) {
                        continue;
                    }

                    exceptionLogger.debug("Current Abstract: " + (i + 1) + "/"
                            + testingAbstracts.size() + "(ID " + abstractId
                            + ")), Properties File: " + rankUpPropertiesCount + "/" +
                            rankUpPropertiesList.size(), DebugLevel.INFO);
                    exceptionLogger.debug("Text: " + abs.getOriginalText() + "\n", 
                            DebugLevel.INFO);

                    // Determine keyword extraction method
                    GraphBasedKeywordExtractor keywordExtractor;
                    if (rankUpProperties.keywordExtractionMethod == null ||
                            rankUpProperties.keywordExtractionMethod == 
                                    KeywordExtractionMethod.TEXTRANK) {

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
                    else if (rankUpProperties.keywordExtractionMethod == 
                            KeywordExtractionMethod.RAKE) {
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
                            abs, nonRankUpProperties, rankUpProperties, Feature.TFIDF_STEMMED,
                            originalKeyphrases, rankUpPropertiesProcessedCount, 
                            GET_TFIDF_KEYPHRASES, extractedKeywordsTable, rankUp);
                    
                    // Get RIDF KeyPhrases
                    List<KeyPhrase> ridfKeyphrases = getAndInsertFeatureKeyphrases(
                            abs, nonRankUpProperties, rankUpProperties, Feature.RIDF_STEMMED,
                            originalKeyphrases, rankUpPropertiesProcessedCount, 
                            GET_RIDF_KEYPHRASES, extractedKeywordsTable, rankUp);
                    
                    
                    // Get Clusteredness KeyPhrases
                    List<KeyPhrase> clusterednessKeyphrases = getAndInsertFeatureKeyphrases(
                            abs, nonRankUpProperties, rankUpProperties, Feature.CLUSTEREDNESS_STEMMED,
                            originalKeyphrases, rankUpPropertiesProcessedCount, 
                            GET_CLUSTEREDNESS_KEYPHRASES, extractedKeywordsTable, rankUp);
                    
                    // Get RAKE KeyPhrases
                    List<KeyPhrase> rakeKeyphrases= getAndInsertFeatureKeyphrases(
                            abs, nonRankUpProperties, rankUpProperties, Feature.RAKE_STEMMED,
                            originalKeyphrases, rankUpPropertiesProcessedCount, 
                            GET_RAKE_KEYPHRASES, extractedKeywordsTable, rankUp);
                    
                    // Print and insert Original KeyPhrases into database
                    if (rankUpPropertiesProcessedCount < 1 && GET_ORIGINAL_KEYPHRASES) {
                        
                        printKeyPhrases(originalKeyphrases, 
                                rankUpProperties.keywordExtractionMethod.toString(), false);
                        
                        if (INSERT_KEYPHRASES_INTO_DATABASE &&
                                !abstractManager.keywordsExistInDatabase(
                                        abs, 
                                        nonRankUpProperties, 
                                        rankUpProperties.keywordExtractionMethod.toString(), 
                                        extractedKeywordsTable)) {
                            
                            exceptionLogger.debug("Inserting " + 
                                    rankUpProperties.keywordExtractionMethod.toString() + 
                                    " KeyPhrases into database...", 
                                    DebugLevel.INFO);
                            insertKeyphrasesIntoDatabase(
                                    abs.getAbstractId(),
                                    originalKeyphrases,
                                    nonRankUpProperties, 
                                    rankUpProperties.keywordExtractionMethod.toString(),
                                    extractedKeywordsTable);
                        }
                    }
                    
                    // Print and insert RankUp KeyPhrases into database
                    if (GET_RANKUP_KEYPHRASES) {
                        
                        printKeyPhrases(rankUpKeyphrases, "RankUp", false);
                        
                        if (INSERT_KEYPHRASES_INTO_DATABASE &&
                                !abstractManager.keywordsExistInDatabase(abs, 
                                rankUpProperties, "RankUp", extractedKeywordsTable)) {

                            exceptionLogger.debug("Inserting RankUp KeyPhrases into database...", 
                                    DebugLevel.INFO);
                            insertKeyphrasesIntoDatabase(
                                    abs.getAbstractId(),
                                    rankUpKeyphrases,
                                    rankUpProperties, 
                                    "RankUp",
                                    extractedKeywordsTable);
                        }
                    }
                    
                    // Get TextRank KeyPhraseGraph
                    if (PRINT_SETS) {
                        KeyPhraseGraph keyphraseGraph = new KeyPhraseGraph(
                                exceptionLogger, 
                                originalKeyphrases,
                                rankUpProperties.setAssignmentApproach,
                                rankUpProperties.featureLowerBound, 
                                rankUpProperties.featureUpperBound,
                                Feature.ORIGINAL_SCORE);
                        exceptionLogger.debug("**** " + Feature.ORIGINAL_SCORE + " Sets ****", 
                                DebugLevel.DEBUG);
                        keyphraseGraph.printFeatureSet(SetLevel.LOW, DebugLevel.DEBUG);
                        keyphraseGraph.printFeatureSet(SetLevel.MID, DebugLevel.DEBUG);
                        keyphraseGraph.printFeatureSet(SetLevel.HIGH, DebugLevel.DEBUG);
                    }
                    
                    // Get RankUp KeyPhraseGraph
                    if (PRINT_SETS) {
                        KeyPhraseGraph rankUpGraph = new KeyPhraseGraph(
                                exceptionLogger, 
                                rankUpKeyphrases,
                                rankUpProperties.setAssignmentApproach,
                                rankUpProperties.featureLowerBound, 
                                rankUpProperties.featureUpperBound,
                                Feature.RANKUP_SCORE);
                        exceptionLogger.debug("**** " + Feature.RANKUP_SCORE + " Sets ****", 
                                DebugLevel.DEBUG);
                        rankUpGraph.printFeatureSet(SetLevel.LOW, DebugLevel.DEBUG);
                        rankUpGraph.printFeatureSet(SetLevel.MID, DebugLevel.DEBUG);
                        rankUpGraph.printFeatureSet(SetLevel.HIGH, DebugLevel.DEBUG);
                    }

                    // Print the real rankUpKeyphrases
                    abstractManager.printRealKeywords(abs);

                    // Call results
                    boolean rankUpIsBest = false;
                    if (PRINT_RESULTS) {
                        rankUpIsBest = PrecisionAnalyzer.results(
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
                    if (keyphraseFinalTextRankScoreCorrectness != null) {
                        overallKeyphraseFinalTextRankScoreCorrectness += 
                                keyphraseFinalTextRankScoreCorrectness;
                        overallKeyphraseFinalTextRankScoreCorrectnessCount++;
                    }
                    if (textRankNodeScoreCorrectness != null) {
                        overallTextRankNodeScoreCorrectness += textRankNodeScoreCorrectness;
                        overallTextRankNodeScoreCorrectnessCount++;
                    }
                    exceptionLogger.debug("", DebugLevel.DEBUG);
                    exceptionLogger.debug("Keyphrase final TextRank score correctness (this abstract): " + 
                            (keyphraseFinalTextRankScoreCorrectness != null ?
                                    MathUtils.round(keyphraseFinalTextRankScoreCorrectness, 2) :
                                    ""), DebugLevel.DEBUG);
                    exceptionLogger.debug("TextRank node score correctness (this abstract): " + 
                            (textRankNodeScoreCorrectness != null ?
                                    MathUtils.round(textRankNodeScoreCorrectness, 2) :
                                    ""), DebugLevel.DEBUG);
                    
                    //////////////////// Used for choosing good examples ////////////////////////
                    // Check if RankUp performs best always
                    if (rankUpIsBest) {
                        if (keyphraseFinalTextRankScoreCorrectness != null &&
                                keyphraseFinalTextRankScoreCorrectness == 1.0) {

                            // Expected scores must correspond to at least 50% of keyphrases
                            int changedKeyphrases = 0;
                            int scoreIncreases = 0, scoreDecreases = 0;
                            for (KeyPhrase keyphrase : rankUpKeyphrases) {
                                if (keyphrase.getExpectedScore() > 0) {
                                    changedKeyphrases++;
                                }
                                if (keyphrase.getExpectedScoreDirection() == ScoreDirection.INCREASE) {
                                    scoreIncreases++;
                                }
                                if (keyphrase.getExpectedScoreDirection() == ScoreDirection.DECREASE) {
                                    scoreDecreases++;
                                }

                            }
                            if ((double) changedKeyphrases / rankUpKeyphrases.size() >= 0.5) {

                                // At least one keyphrase score goes up and one goes down
                                if (scoreIncreases >= 1 && scoreDecreases >= 1) {
                                    
                                    // Add this abstract to list
                                    goodExamples.add(i + 1);
                                }
                            }
                        }
                    }

                    // Insert debug information into database
                    exceptionLogger.debug("Iterations: " + rankUp.getRankUpIterations(), 
                            DebugLevel.DETAIL);
                    if (INSERT_DEBUGGING_INFORMATION) {
                        exceptionLogger.debug("Inserting debug information into database...", 
                                DebugLevel.INFO);
                        insertRankUpDebuggingInformationIntoDatabase(
                                abstractId,
                                rankUp,
                                debuggingTable);
                    }
                    
                    exceptionLogger.debug("", DebugLevel.INFO);
                    exceptionLogger.debug("RankUp completed for this text!", 
                            DebugLevel.INFO);
                    exceptionLogger.debug("", DebugLevel.INFO);
                    
                    abstractsProcessed++;
                }
                
                // RankUp correctness for these properties
                overallKeyphraseFinalTextRankScoreCorrectness = 
                        overallKeyphraseFinalTextRankScoreCorrectness / 
                        overallKeyphraseFinalTextRankScoreCorrectnessCount;
                overallTextRankNodeScoreCorrectness = 
                        overallTextRankNodeScoreCorrectness / 
                        overallTextRankNodeScoreCorrectnessCount;
                exceptionLogger.debug("Overall keyphrase final TextRank score correctness (these properties): " + 
                        MathUtils.round(overallKeyphraseFinalTextRankScoreCorrectness, 2), 
                        DebugLevel.DEBUG);
                exceptionLogger.debug("Overall TextRank node score correctness (these properties): " + 
                        MathUtils.round(overallTextRankNodeScoreCorrectness, 2), 
                        DebugLevel.DEBUG);
                exceptionLogger.debug("", DebugLevel.DEBUG);
                
                // Print good examples
                exceptionLogger.debug("Good examples:", DebugLevel.DETAIL);
                for (int i : goodExamples) {
                    exceptionLogger.debug(String.valueOf(i), DebugLevel.DETAIL);
                }
                
                // Only increase rankUpPropertiesProcessedCount if an abstract was processed
                if (abstractsProcessed > 0) {
                    rankUpPropertiesProcessedCount++;
                }
                
                if (rankUpPropertiesProcessedCount >= PROPERTIES_FILES_TO_PROCESS &&
                        PROPERTIES_FILES_TO_PROCESS != 0) {
                    break;
                }
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