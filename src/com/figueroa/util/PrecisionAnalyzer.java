package com.figueroa.util;

import java.util.ArrayList;
import java.util.List;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.rankup.Main;
import com.figueroa.nlp.Lemmatizer;

/**
 * Used for calculating different precision metrics (precision, recall, f-score)
 * from the results of different keyphrase extraction methods.
 * @author gfigueroa
 */
public class PrecisionAnalyzer {
    
    /**
     * Calculates and prints results for TFIDF, TextRank and RankUp keyphrases.
     * Returns true if RankUp performs better than the other two methods.
     * @param abs
     * @param tfidfKeyphrases
     * @param textRankKeyphrases
     * @param rankUpKeyphrases
     * @param partialMatching
     * @param exceptionLogger
     * @param dataLogger
     * @param abstractManager
     * @param lemmatizer
     * @return true if RankUp performed the best (Precision), false otherwise
     * @throws Exception 
     */
    public static boolean results(
            Abstract abs, 
            List<KeyPhrase> tfidfKeyphrases,
            List<KeyPhrase> textRankKeyphrases,
            List<KeyPhrase> rankUpKeyphrases,
            boolean partialMatching, ExceptionLogger exceptionLogger, 
            ExceptionLogger dataLogger, AbstractManager abstractManager, Lemmatizer lemmatizer)
            throws Exception {

        int abstractId = abs.getAbstractId();

        // Results
        if (exceptionLogger.getDebugLevel().compareTo(ExceptionLogger.DebugLevel.DEBUG) <= 0) {
            
            ArrayList<Double> tfidfPrecisions;
            exceptionLogger.debug("***TFIDF Results***", ExceptionLogger.DebugLevel.DEBUG);
            tfidfPrecisions = printResults(tfidfKeyphrases,
                    abstractManager.retrieveRealKeywords(abstractId),
                    abstractManager.retrieveRealStemmedKeywords(abstractId), 
                    partialMatching, false, lemmatizer, dataLogger);
            exceptionLogger.debug("", ExceptionLogger.DebugLevel.DEBUG);
            
            ArrayList<Double> textRankPrecisions;
            exceptionLogger.debug("***TextRank Results***", ExceptionLogger.DebugLevel.DEBUG);
            textRankPrecisions = printResults(textRankKeyphrases,
                    abstractManager.retrieveRealKeywords(abstractId),
                    abstractManager.retrieveRealStemmedKeywords(abstractId), 
                    partialMatching, false, lemmatizer, dataLogger);
            exceptionLogger.debug("", ExceptionLogger.DebugLevel.DEBUG);

            ArrayList<Double> rankUpPrecisions;
            exceptionLogger.debug("***RankUp Results***", ExceptionLogger.DebugLevel.DEBUG);
            rankUpPrecisions = printResults(rankUpKeyphrases,
                    abstractManager.retrieveRealKeywords(abstractId),
                    abstractManager.retrieveRealStemmedKeywords(abstractId),
                    partialMatching, false, lemmatizer, dataLogger);

            // Check higher or lower precision
            int totalKeywords = tfidfPrecisions.size();
            totalKeywords = textRankPrecisions.size() < totalKeywords ? 
                    textRankPrecisions.size() : totalKeywords;
            totalKeywords = rankUpPrecisions.size() < totalKeywords ? 
                    rankUpPrecisions.size() : totalKeywords;
            for (int keywordsUsed = 0; keywordsUsed < totalKeywords; keywordsUsed++) {
                double tfidfPrecision = tfidfPrecisions.get(keywordsUsed);
                double textRankPrecision = textRankPrecisions.get(keywordsUsed);
//                if (keywordsUsed >= rankUpPrecisions.size()) {
//                    continue;
//                }
                double rankUpPrecision = rankUpPrecisions.get(keywordsUsed);
                boolean interesting = false;
                if (textRankPrecision > rankUpPrecision) {
                    dataLogger.writeToLog("*******************************************************************************************");
                    dataLogger.writeToLog("TextRank precision higher than RankUp precision using " +
                            (keywordsUsed + 1) + " keywords.");
                    interesting = true;
                }
                if (tfidfPrecision > rankUpPrecision) {
                    dataLogger.writeToLog("*****************************************************************************************************");
                    dataLogger.writeToLog("TFIDF precision higher than RankUp precision using " +
                            (keywordsUsed + 1) + " keywords.");
                    interesting = true;
                }

                if (interesting) {
                    dataLogger.writeToLog("Text: " + abs.getOriginalText() + "\n");
                    Main.printKeyPhrases(tfidfKeyphrases,
                            "***********TFIDF KEYPHRASES************", true);
                    Main.printKeyPhrases(textRankKeyphrases,
                            "***********TEXTRANK KEYPHRASES************", true);
                    Main.printKeyPhrases(rankUpKeyphrases,
                            "***********RANKUP KEYPHRASES************", true);
                    dataLogger.writeToLog("***********REAL KEYPHRASES************");
                    for (String realKeyword : abstractManager.retrieveRealKeywords(abstractId)) {
                        dataLogger.writeToLog(realKeyword);
                    }
                    tfidfPrecisions = printResults(tfidfKeyphrases,
                            abstractManager.retrieveRealKeywords(abstractId),
                            abstractManager.retrieveRealStemmedKeywords(abstractId),
                            partialMatching, true, lemmatizer, dataLogger);
                    textRankPrecisions = printResults(textRankKeyphrases,
                            abstractManager.retrieveRealKeywords(abstractId),
                            abstractManager.retrieveRealStemmedKeywords(abstractId),
                            partialMatching, true, lemmatizer, dataLogger);
                    rankUpPrecisions = printResults(rankUpKeyphrases,
                            abstractManager.retrieveRealKeywords(abstractId),
                            abstractManager.retrieveRealStemmedKeywords(abstractId),
                            partialMatching, true, lemmatizer, dataLogger);
                    dataLogger.writeToLog("*****************************************************************************************************");
                }
            }
            
            boolean rankUpIsBest = true;
            int chances = 0;
            for (int i = 0; i < rankUpPrecisions.size(); i++) {
                try {
                    double rankUpPrecision = rankUpPrecisions.get(i);
                    double tfidfPrecision = tfidfPrecisions.get(i);
                    double textRankPrecision = textRankPrecisions.get(i);
                    
                    if (rankUpPrecision <= tfidfPrecision ||
                            rankUpPrecision <= textRankPrecision) {
                        chances++;
                    }
                    
                    // Give RankUp 2 chances
                    if (chances > 2) {
                        rankUpIsBest = false;
                        break;
                    }
                    
                }
                catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
            return rankUpIsBest;
            
        }
        else {
            return false;
        }
    }

    /**
     * Prints a results table.
     * @param extractedKeyphrases
     * @param realUnstemmedKeywords
     * @param realStemmedKeywords
     * @param partialMatching
     * @param writeToDataLog
     * @param lemmatizer
     * @param dataLogger
     * @return
     * @throws Exception 
     */
    private static ArrayList<Double> printResults(List<KeyPhrase> extractedKeyphrases,
            List<String> realUnstemmedKeywords, List<String> realStemmedKeywords, 
            boolean partialMatching, boolean writeToDataLog, Lemmatizer lemmatizer,
            ExceptionLogger dataLogger)
            throws Exception {

        ArrayList<Double> finalPrecisions = new ArrayList<>();

        ArrayList<Integer> unstemmedCorrectList = new ArrayList<>();
        ArrayList<Integer> stemmedCorrectList = new ArrayList<>();
        ArrayList<Double> unstemmedPrecisionList = new ArrayList<>();
        ArrayList<Double> stemmedPrecisionList = new ArrayList<>();
        ArrayList<Double> unstemmedRecallList = new ArrayList<>();
        ArrayList<Double> stemmedRecallList = new ArrayList<>();

        int maxKeywordsUsed = 0;
        for (int keywordsUsed = 1; keywordsUsed <= 5 && 
                keywordsUsed <= extractedKeyphrases.size(); keywordsUsed++) {
            maxKeywordsUsed++;

            ArrayList<String> extractedUnstemmedKeywords = new ArrayList<>();
            for (int i = 0; i < keywordsUsed; i++) {
                KeyPhrase keyphrase = extractedKeyphrases.get(i);
                extractedUnstemmedKeywords.add(keyphrase.getText());
            }

            ArrayList<String> extractedStemmedKeywords = new ArrayList<>();
            for (String currWord : extractedUnstemmedKeywords) {
                String stemmedWord = lemmatizer.stemText(currWord, false);
                extractedStemmedKeywords.add(stemmedWord);
            }

            int unstemmedCorrect =
                    calculateNumberOfCorrectKeywords(
                    realUnstemmedKeywords, extractedUnstemmedKeywords,
                            partialMatching);
            unstemmedCorrectList.add(unstemmedCorrect);
            int stemmedCorrect =
                    calculateNumberOfCorrectKeywords(
                    realStemmedKeywords, extractedStemmedKeywords, partialMatching);
            stemmedCorrectList.add(stemmedCorrect);

            double unstemmedPrecision =
                    PrecisionAnalyzer.calculatePrecision(
                    realUnstemmedKeywords, extractedUnstemmedKeywords,
                            partialMatching);
            unstemmedPrecisionList.add(unstemmedPrecision);
            double stemmedPrecision =
                    PrecisionAnalyzer.calculatePrecision(
                    realStemmedKeywords, extractedStemmedKeywords,
                            partialMatching);
            stemmedPrecisionList.add(stemmedPrecision);

            // Final precision
            if (stemmedPrecision >= unstemmedPrecision) {
                finalPrecisions.add(stemmedPrecision);
            }
            else {
                finalPrecisions.add(unstemmedPrecision);
            }

            double unstemmedRecall =
                    PrecisionAnalyzer.calculateRecall(
                    realUnstemmedKeywords, extractedUnstemmedKeywords,
                            partialMatching);
            unstemmedRecallList.add(unstemmedRecall);
            double stemmedRecall =
                    PrecisionAnalyzer.calculateRecall(
                    realStemmedKeywords, extractedStemmedKeywords,
                            partialMatching);
            stemmedRecallList.add(stemmedRecall);
        }

        String header = "";
        String correctText = "";
        String precisionText = "";
        String recallText = "";
        for (int i = 0; i < maxKeywordsUsed; i++) {
            header += (i + 1) + " keywords used                                ";
            correctText +=
                    "Correct (unstemmed):   " + unstemmedCorrectList.get(i) +
                    ", (stemmed):     " + stemmedCorrectList.get(i) + "     ";
            precisionText += 
                    "Precision (unstemmed): " + MiscUtils.convertDoubleToFixedCharacterString(unstemmedPrecisionList.get(i), 2) +
                    ", (stemmed):  " + MiscUtils.convertDoubleToFixedCharacterString(stemmedPrecisionList.get(i), 2) + "  ";
            recallText += 
                    "Recall (unstemmed):    " + MiscUtils.convertDoubleToFixedCharacterString(unstemmedRecallList.get(i), 2) +
                    ", (stemmed):  " + MiscUtils.convertDoubleToFixedCharacterString(stemmedRecallList.get(i), 2) + "  ";
        }

        if (!writeToDataLog) {
            System.out.println(header + "\n" + correctText + "\n" + precisionText + "\n" +
                    recallText);
        }
        else {
            dataLogger.writeToLog(header + "\n" + correctText + "\n" + precisionText + "\n" +
                    recallText);
        }

        return finalPrecisions;
    }
    
    /**
     * Counts the number of correct keyphrases in a list of extracted keyphrases
     * @param realKeywords
     * @param extractedKeywords
     * @param partialMatching: whether or not to use partial matching (i.e. a
     * keyphrase is classified as correct if the extracted keyphrase contains
     * a real keyphase.
     * @return 
     */
    public static int calculateNumberOfCorrectKeywords(List<String> realKeywords,
            List<String> extractedKeywords, boolean partialMatching) {
        
        int correctKeywords = 0;

        //Check which keywords were correctly extracted
        for (int i = 0; i < extractedKeywords.size(); i++) {
            for (int j = 0; j < realKeywords.size(); j++) {
                
                // Simplification for leniency
                String realKeyword = realKeywords.get(j).toLowerCase();
                String extractedKeyword = extractedKeywords.get(i).toLowerCase();
                realKeyword = realKeyword.replaceAll("-", " ");
                extractedKeyword = extractedKeyword.replaceAll("-", " ");
                realKeyword = realKeyword.replaceAll(" +", " ");
                extractedKeyword = extractedKeyword.replaceAll(" +", " ");
                realKeyword = realKeyword.trim();
                extractedKeyword = extractedKeyword.trim();
                
                if (partialMatching) {
                    if (extractedKeyword.contains(realKeyword)) {
                        correctKeywords++;
                        break;
                    }
                    else if (extractedKeyword.equals(realKeyword)) {
                        correctKeywords++;
                        break;
                    }
                }
                else {
                    if (realKeyword.equals(extractedKeyword)) {
                        correctKeywords++;
                        break;
                    }
                }

                String[] realKeywordTokens = realKeyword.split(" ");
                String[] extractedKeywordTokens = extractedKeyword.split(" ");
                if (realKeywordTokens.length != extractedKeywordTokens.length) {
                    continue;
                }
                else {
                    boolean areEqual = true;
                    for (int k = 0; k < realKeywordTokens.length; k++) {
                        String realToken = realKeywordTokens[k];
                        String extractedToken = extractedKeywordTokens[k];
                        if (!(realToken.contains(extractedToken) || 
                                extractedToken.contains(realToken))) {
                            areEqual = false;
                            break;
                        }
                    }
                    if (areEqual) {
                        correctKeywords++;
                        break;
                    }
                }
            }
        }

        return correctKeywords;
    }

    /**
     * Calculates the recall of the extracted keyphrases.
     * @param realKeywords
     * @param extractedKeywords
     * @param partialMatching: whether or not to use partial matching (i.e. a
     * keyphrase is classified as correct if the extracted keyphrase contains
     * a real keyphase.
     * @return 
     */
    public static double calculatePrecision(List<String> realKeywords, 
            List<String> extractedKeywords, boolean partialMatching) {

        int correctKeywords = calculateNumberOfCorrectKeywords(realKeywords, 
                extractedKeywords, partialMatching);

        double precision = 
                Double.valueOf(correctKeywords) / Double.valueOf(extractedKeywords.size());

        return precision;
    }

    /**
     * Calculates the recall of the extracted keyphrases.
     * @param realKeywords
     * @param extractedKeywords
     * @param partialMatching: whether or not to use partial matching (i.e. a
     * keyphrase is classified as correct if the extracted keyphrase contains
     * a real keyphase.
     * @return 
     */
    public static double calculateRecall(List<String> realKeywords, 
            List<String> extractedKeywords, boolean partialMatching) {

        int correctKeywords = calculateNumberOfCorrectKeywords(realKeywords, 
                extractedKeywords, partialMatching);

        double recall = 
                Double.valueOf(correctKeywords) / Double.valueOf(realKeywords.size());
        if (recall >= 1) {
            recall = 1;
        }

        return recall;
    }

}
