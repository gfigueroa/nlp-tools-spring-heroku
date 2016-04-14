package com.figueroa.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import com.figueroa.nlp.rankup.RankUpProperties;
import com.figueroa.util.ExceptionLogger.DebugLevel;

/**
 *
 * Manages abstract access to and from the database
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * January 2013
 */
public class AbstractManager {

    public DatabaseManager databaseManager;
    private ExceptionLogger logger;

    public AbstractManager(DatabaseManager dbm, ExceptionLogger log) {
        databaseManager = dbm;
        logger = log;
    }

    /**
     * Retrieve all abstract IDs of the given type
     * @param abstractType
     * @param source
     * @param table
     * @return 
     */
    public ArrayList<Integer> retrieveAbstractIds(Abstract.Type abstractType,
            String source, String table) {

        /*abstractType:
         * 1 - training
         * 2 - testing
         * 3 - validation
         * 4 - all
         */
        String selectString = "";

        switch (abstractType) {
            case ALL:
                selectString = "SELECT Abstract_Id FROM " + table + " WHERE 1 = 1 ";
                break;
            case TRAINING:
                selectString = "SELECT Abstract_Id FROM " + table +
                        " WHERE Abstract_Type = 'Training' ";
                break;
            case TESTING:
                selectString = "SELECT Abstract_Id FROM " + table +
                        " WHERE Abstract_Type = 'Testing' ";
                break;
            case VALIDATION:
                selectString = "SELECT Abstract_Id FROM " + table +
                        " WHERE Abstract_Type = 'Validation' ";
                ;
                break;
            default:
                logger.debug("Error in retrieveAbstractIds: incorrect abstractType!",
                        DebugLevel.ERROR);
                return null;
        }

        if (source != null) {
            selectString = selectString.concat("AND Abstract_Source = '" + source + "' ");
        }

        selectString = selectString.concat("ORDER BY Abstract_Id");

        ArrayList<Integer> abstractIds = new ArrayList<Integer>();

        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            while (rs.next()) {
                abstractIds.add(rs.getInt("Abstract_Id"));
            }

            return abstractIds;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractIds: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve all abstract texts of the given type.
     * @param abstractType
     * @param source
     * @param table
     * @return 
     */
    public ArrayList<String> retrieveAbstractTexts(Abstract.Type abstractType,
            String source, String table) {

        /*abstractType:
         * 1 - training
         * 2 - testing
         * 3 - validation
         * 4 - all
         */
        String selectString = "";

        switch (abstractType) {
            case ALL:
                selectString = "SELECT Abstract_Text FROM " + table + " WHERE 1 = 1 ";
                break;
            case TRAINING:
                selectString = "SELECT Abstract_Text FROM " + table +
                        " WHERE Abstract_Type = 'Training' ";
                break;
            case TESTING:
                selectString = "SELECT Abstract_Text FROM " + table +
                        " WHERE Abstract_Type = 'Testing' ";
                break;
            case VALIDATION:
                selectString = "SELECT Abstract_Text FROM " + table +
                        " WHERE Abstract_Type = 'Validation' ";
                ;
                break;
            default:
                logger.debug("Error in retrieveAbstractTexts: incorrect abstractType!",
                        DebugLevel.ERROR);
                return null;
        }

        if (source != null) {
            selectString = selectString.concat("AND Abstract_Source = '" + source + "' ");
        }

        selectString = selectString.concat("ORDER BY Abstract_Id");

        ArrayList<String> abstractTexts = new ArrayList<String>();

        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            while (rs.next()) {
                abstractTexts.add(rs.getString("Abstract_Text"));
            }

            return abstractTexts;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractTexts: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve all Abstracts of the given type
     * @param abstractType
     * @param source
     * @param table
     * @return 
     */
    public ArrayList<Abstract> retrieveAbstracts(Abstract.Type abstractType,
            String source, String table) {

        /*abstractType:
         * 1 - training
         * 2 - testing
         * 3 - validation
         * 4 - all
         */
        String selectString = "";

        switch (abstractType) {
            case ALL:
                selectString = "SELECT * FROM " + table + " WHERE 1 = 1 ";
                break;
            case TRAINING:
                selectString = "SELECT * FROM " + table +
                        " WHERE Abstract_Type = 'Training' ";
                break;
            case TESTING:
                selectString = "SELECT * FROM " + table +
                        " WHERE Abstract_Type = 'Testing' ";
                break;
            case VALIDATION:
                selectString = "SELECT * FROM " + table +
                        " WHERE Abstract_Type = 'Validation' ";
                break;
            default:
                logger.debug("Error in retrieveAbstracts: incorrect abstractType!",
                        DebugLevel.ERROR);
                return null;
        }

        if (source != null) {
            selectString = selectString.concat("AND Abstract_Source = '" + source + "' ");
        }

        selectString = selectString.concat("ORDER BY Abstract_Id");

        ArrayList<Abstract> abstracts = new ArrayList<Abstract>();

        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            while (rs.next()) {
                int abstractId = rs.getInt("Abstract_Id");
                String originalText = rs.getString("Abstract_Text");
                String stemmedText = rs.getString("Abstract_Stemmed_Text");

                Abstract abs =
                        new Abstract(abstractId, originalText, stemmedText, abstractType);

                abstracts.add(abs);
            }

            return abstracts;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractTexts: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
        catch (Exception ex) {
            logger.debug("Exception in retrieveAbstractTexts: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve a specific Abstract given its ID
     * @param abstractId
     * @return 
     */
    public Abstract retrieveAbstract(int abstractId) {
        String selectString = "SELECT Abstract_Type, Abstract_Text, Abstract_Stemmed_Text " +
                "FROM Abstract WHERE Abstract_Id = " + abstractId;
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();

            Abstract.Type abstractType = Abstract.getTypeFromString(rs.getString("Abstract_Type"));
            String originalText = rs.getString("Abstract_Text");
            String stemmedText = rs.getString("Abstract_Stemmed_Text");

            Abstract abs =
                    new Abstract(abstractId, originalText, stemmedText, abstractType);

            return abs;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractText: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        } 
        catch (Exception ex) {
            logger.debug("Exception in retrieveAbstractText: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }
    
    /**
     * Retrieve a specific abstract text given the Abstract's ID
     * @param abstractId
     * @return 
     */
    public String retrieveAbstractText(int abstractId) {
        String selectString = "SELECT Abstract_Text FROM Abstract WHERE Abstract_Id = " + abstractId;
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();
            String abstractText = rs.getString("Abstract_Text");

            return abstractText;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractText: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve a specific Abstract ID given the abstract text
     * @param abstractText
     * @return 
     */
    public int retrieveAbstractId(String abstractText) {
        String selectString = "SELECT Abstract_Id FROM Abstract WHERE Abstract_Text LIKE '%"
                + abstractText + "%'";
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();

            if (rs.isLast()) {
                int abstractId = rs.getInt("Abstract_Id");

                return abstractId;
            }
            else {
                return 0;
            }
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractText: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return 0;
        }
    }

    /**
     * Retrieve the stemmed text of a given Abstract (for efficiency)
     * @param abstractId
     * @return 
     */
    public String retrieveAbstractStemmedText(int abstractId) {
        String selectString = "SELECT Abstract_Stemmed_Text FROM Abstract WHERE Abstract_Id = " + abstractId;
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();
            String abstractText = rs.getString("Abstract_Stemmed_Text");

            return abstractText;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractStemmedText: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve the real keywords of a given Abstract
     * @param abstractId
     * @return 
     */
    public ArrayList<String> retrieveRealKeywords(int abstractId) {

        String selectString;
        selectString = "SELECT keyword FROM Abstract_Real_Keyword WHERE Abstract_Id = " + abstractId;

        ArrayList<String> realKeywords = new ArrayList<String>();
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);
            while (rs.next()) {
                realKeywords.add(rs.getString("Keyword").toLowerCase());
            }

            return realKeywords;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveRealKeywords: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieves only the real keywords that exist in the abstract text
     * @param abstractId
     * @return 
     */
    public ArrayList<String> retrieveRealKeywordsExistingInText(int abstractId) {

        String selectString;
        selectString = "SELECT keyword FROM Abstract_Real_Keyword WHERE Abstract_Id = " + abstractId + " AND (Frequency_Stemmed > 0 OR Frequency_Unstemmed > 0)";

        ArrayList<String> realKeywords = new ArrayList<String>();
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);
            while (rs.next()) {
                realKeywords.add(rs.getString("Keyword").toLowerCase());
            }

            return realKeywords;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveRealKeywords: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Prints only the real keywords of a given abstract that also exist in the text
     * @param abs 
     */
    public void printRealKeywordsExistingInText(Abstract abs) {
            logger.debug("***************REAL KEYPHRASES THAT EXIST IN ABSTRACT TEXT*****************",
                    DebugLevel.DEBUG);
            ArrayList<String> realExistingKeyphrases =
                    retrieveRealKeywordsExistingInText(abs.getAbstractId());
            for (String phrase : realExistingKeyphrases) {
                logger.debug(phrase, DebugLevel.DEBUG);
            }
            logger.debug("LIST SIZE: " + realExistingKeyphrases.size(), DebugLevel.DEBUG);
            logger.debug("", DebugLevel.DEBUG);
    }

    /**
     * Prints the real keywords of a given abstract
     * @param abs 
     */
    public void printRealKeywords(Abstract abs) {
            logger.debug("***************REAL KEYPHRASES*****************",
                    DebugLevel.DEBUG);
            ArrayList<String> realKeyphrases =
                    retrieveRealStemmedKeywords(abs.getAbstractId());
            for (String phrase : realKeyphrases) {
                logger.debug(phrase, DebugLevel.DEBUG);
            }
            logger.debug("LIST SIZE: " + realKeyphrases.size(), DebugLevel.DEBUG);
            logger.debug("", DebugLevel.DEBUG);
    }

    /**
     * Retrieves the stemmed version of the given abstract's real keywords
     * @param abstractId
     * @return 
     */
    public ArrayList<String> retrieveRealStemmedKeywords(int abstractId) {

        String selectString;
        selectString = "SELECT stemmed_keyword FROM Abstract_Real_Keyword WHERE Abstract_Id = " + abstractId;

        ArrayList<String> realKeywords = new ArrayList<String>();
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);
            while (rs.next()) {
                realKeywords.add(rs.getString("stemmed_keyword").toLowerCase());
            }

            return realKeywords;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveRealStemmedKeywords: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve the keywords that have been extracted for the given abstract
     * @param abstractId
     * @param keywordsUsed: the number of keywords to retrieve (top K)
     * @param slop: the maximum cooccurence value used in the TextRank algorithm
     * @param method: the method with which the keyphrases were extracted
     * @return 
     */
    public ArrayList<String> retrieveExtractedKeywords(
            int abstractId, int keywordsUsed, int slop, String method) {
        String selectString = 
                "SELECT keyword FROM Abstract_Extracted_Keyword WHERE Abstract_Id = " 
                + abstractId + " AND Slop = " + slop + " AND Method = '" + method + "' "
                + " ORDER BY score DESC";
        ArrayList<String> extractedKeywords = new ArrayList<String>();

        try {

            //If keywordsUsed = 0, then the number of keywords to be returned is equal to 5% 
            // of the total number of words in the abstract
            if (keywordsUsed == 0) {
                String selectString2 = "SELECT word_count FROM Abstract "
                        + "WHERE Abstract_Id = " + abstractId;

                ResultSet rs2 = databaseManager.executeQuery(selectString2);
                rs2.next();
                double percentage = 0.05;
                double wordCount = rs2.getInt("word_count");
                keywordsUsed = (int) Math.round(percentage * wordCount);

                if (keywordsUsed == 0) {
                    keywordsUsed = 1;
                }
            }

            ResultSet rs = databaseManager.executeQuery(selectString);

            int i = 1;
            while (rs.next() && i <= keywordsUsed) {
                extractedKeywords.add(rs.getString("Keyword"));
                i++;
            }

            return extractedKeywords;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveExtractedKeywords: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Retrieve the abstract type from an Abstract
     * @param abstractId
     * @return 
     */
    public String retrieveAbstractType(int abstractId) {
        String selectString = 
                "SELECT Abstract_Type FROM Abstract WHERE Abstract_Id = " + abstractId;
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();
            String abstractType = rs.getString("Abstract_Type");

            return abstractType;
        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in retrieveAbstractType: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return null;
        }
    }

    /**
     * Determines whether this abstract id belongs to this abstract source
     * @param abstractId
     * @param abstractSource
     * @return 
     */
    public boolean isFromThisSource(int abstractId, String abstractSource) {
        String selectString = "SELECT Abstract_Source FROM Abstract WHERE Abstract_Id = " + abstractId;
        try {
            ResultSet rs = databaseManager.executeQuery(selectString);

            rs.next();
            String thisAbstractsSource = rs.getString("Abstract_Source");

            if (thisAbstractsSource.equals(abstractSource)) {
                return true;
            }
            else {
                return false;
            }

        }
        catch (SQLException ex) {
            logger.debug("SQL Exception in isFromThisSource: " + ex.getMessage(),
                    DebugLevel.ERROR);
            return false;
        }
    }

    /**
     * Determines whether or not an input phrase is a keyphrase for the specified abstract
     * @param abstractId
     * @param phrase
     * @return 
     */
    public boolean isKeyphrase(int abstractId, String phrase) {
        
        // Simplification for leniency
        phrase = phrase.replaceAll("-", " ");
        phrase = phrase.replaceAll(" +", " ");
        phrase = phrase.trim();
        
        ArrayList<String> originalKeyphrases = 
                this.retrieveRealKeywords(abstractId);
        ArrayList<String> stemmedKeyphrases = 
                this.retrieveRealStemmedKeywords(abstractId);
        for (String originalKeyword : originalKeyphrases) {
            
            // Simplification for leniency
            originalKeyword = originalKeyword.replaceAll("-", " ");
            originalKeyword = originalKeyword.replaceAll(" +", " ");
            originalKeyword = originalKeyword.trim();
            
            if (phrase.toLowerCase().equals(originalKeyword)) {
                return true;
            }
        }
        for (String stemmedKeyword : stemmedKeyphrases) {
            
            // Simplification for leniency
            stemmedKeyword = stemmedKeyword.replaceAll("-", " ");
            stemmedKeyword = stemmedKeyword.replaceAll(" +", " ");
            stemmedKeyword = stemmedKeyword.trim();
            
            if (phrase.toLowerCase().equals(stemmedKeyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the given keywords
     * @param abs
     * @param rankUpProperties
     * @param method
     * @param extractedKeywordsTable
     * @return 
     */
    public boolean keywordsExistInDatabase(Abstract abs,
            RankUpProperties rankUpProperties, String method, 
            String extractedKeywordsTable) {

        try {
            String query =
                    "SELECT Abstract_Id\n" +
                    "FROM " + extractedKeywordsTable + "\n" +
                    "WHERE " +
                    "Abstract_Id = " + abs.getAbstractId() + " AND " +
                    "Method = '" + method + "' AND " +
                    "Use_Whole_TR_Graph = " + rankUpProperties.useWholeTextRankGraph + " AND " +
                    "Postprocess = " + rankUpProperties.postprocess + " AND " +
                    "Error_Detecting_Approach = '" + rankUpProperties.errorDetectingApproach + "' AND " +
                    "Set_Assignment_Approach = '" + rankUpProperties.setAssignmentApproach + "' AND " +
                    "Feature_Lower_Bound = " + rankUpProperties.featureLowerBound + " AND " +
                    "Feature_Upper_Bound = " + rankUpProperties.featureUpperBound + " AND " +
                    "Expected_Score_Value = '" + rankUpProperties.expectedScoreValue + "' AND " +
                    "Learning_Rate = " + rankUpProperties.learningRate + " AND " +
                    "SE_Threshold = " + rankUpProperties.standardErrorThreshold + " AND " +
                    "Convergence_Scheme = '" + rankUpProperties.convergenceScheme + "' AND " +
                    "Convergence_Rule = '" + rankUpProperties.convergenceRule + "' AND " +
                    "Revert_Graphs = " + rankUpProperties.revertGraphs + " AND " +
                    "Keyword_Extraction_Method = '" + rankUpProperties.keywordExtractionMethod + "'";

            ResultSet rs = databaseManager.executeQuery(query);
            return rs.next();
        }
        catch (SQLException e) {
            logger.debug("Error in existsInDatabase: " + e.getMessage(), DebugLevel.ERROR);
            return true;
        }
    }
}
