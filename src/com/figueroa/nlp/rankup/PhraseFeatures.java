package com.figueroa.nlp.rankup;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import com.figueroa.nlp.rankup.ErrorDetector.ErrorDetectingApproach;
import com.figueroa.nlp.POSTagger;
import com.figueroa.nlp.Phrase;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Lemmatizer;
import com.figueroa.util.Abstract;
import com.figueroa.util.AbstractManager;
import com.figueroa.util.MiscUtils;

/**
 * Node features for HybridRank
 *
 * Gerardo O. Figueroa Calderon
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * April 2011
 *
 */
public class PhraseFeatures {

    public static enum Feature {
        FREQUENCY_UNSTEMMED, FREQUENCY_STEMMED, 
        WORD_COUNT,
        RELATIVE_POSITION_UNSTEMMED, RELATIVE_POSITION_STEMMED, 
        TFIDF_UNSTEMMED, TFIDF_STEMMED, 
        RIDF_UNSTEMMED, RIDF_STEMMED, 
        CLUSTEREDNESS_STEMMED,
        CLUSTEREDNESS_UNSTEMMED, 
        RAKE_STEMMED, RAKE_UNSTEMMED, 
        RANKUP_SCORE, 
        ORIGINAL_SCORE
    };
    
    public static String getShortFeatureString(Feature feature) {
        String featureString = 
                feature.toString().substring(0, feature.toString().lastIndexOf("_"));
        
        // Just in case, make CLUSTEREDNESS lower case
        if (feature == Feature.CLUSTEREDNESS_STEMMED || 
                feature == Feature.CLUSTEREDNESS_UNSTEMMED) {
            featureString = featureString.toLowerCase();
        }
        
        return featureString;
    }

    public final String posTags;
    public final String stemmedPhrase;
    public final double relativePositionUnstemmed;
    public final double relativePositionStemmed;
    public final int frequencyUnstemmed;
    public final int frequencyStemmed;
    public final double tfidfUnstemmed;
    public final double tfidfStemmed;
    public final int keaFrequencyUnstemmed;
    public final int keaFrequencyStemmed;
    public final double ridfUnstemmed;
    public final double ridfStemmed;
    public final double clusterednessStemmed;
    public final double clusterednessUnstemmed;
    public final double rakeStemmed;
    public final double rakeUnstemmed;
    public final int wordCount;
    public final boolean isKeyphrase;

    // Public constructor
    public PhraseFeatures(
            String posTags, 
            String stemmedPhrase,
            double relativePositionUnstemmed, 
            double relativePositionStemmed,
            int frequencyUnstemmed, 
            int frequencyStemmed, 
            double tfidfUnstemmed,
            double tfidfStemmed, 
            int keaFrequencyUnstemmed, 
            int keaFrequencyStemmed,
            double ridfUnstemmed, 
            double ridfStemmed,
            double clusterednessUnstemmed, 
            double clusterednessStemmed,
            double rakeStemmed,
            double rakeUnstemmed,
            boolean isKeyphrase, 
            int wordCount) {
        
        this.posTags = posTags;
        this.stemmedPhrase = stemmedPhrase;
        this.relativePositionUnstemmed = relativePositionUnstemmed;
        this.relativePositionStemmed = relativePositionStemmed;
        this.frequencyUnstemmed = frequencyUnstemmed;
        this.frequencyStemmed = frequencyStemmed;
        this.tfidfUnstemmed = tfidfUnstemmed;
        this.tfidfStemmed = tfidfStemmed;
        this.keaFrequencyUnstemmed = keaFrequencyUnstemmed;
        this.keaFrequencyStemmed = keaFrequencyStemmed;
        this.ridfUnstemmed = ridfUnstemmed;
        this.ridfStemmed = ridfStemmed;
        this.clusterednessUnstemmed = clusterednessUnstemmed;
        this.clusterednessStemmed = clusterednessStemmed;
        this.rakeStemmed = rakeStemmed;
        this.rakeUnstemmed = rakeUnstemmed;
        this.isKeyphrase = isKeyphrase;
        this.wordCount = wordCount;
    }

    // Factory method
    public static PhraseFeatures setPhraseFeatures(
            final Phrase phrase,
            final Abstract abs,
            final Collection<Abstract> trainingAbstracts,
            final AbstractManager abstractManager,
            final POSTagger posTagger,
            final Lemmatizer lemmatizer,
            final HashMap<String, Double> rakeKeyphrases) throws Exception {

        PhraseFeatures features;

        String posTags = "";
        double relativePositionUnstemmed = 0;
        double relativePositionStemmed = 0;
        int frequencyUnstemmed = 0;
        int frequencyStemmed;
        double tfidfUnstemmed = 0;
        double tfidfStemmed;
        int keaFrequencyUnstemmed = 0;
        int keaFrequencyStemmed = 0;
        double ridfUnstemmed = 0;
        double ridfStemmed;
        double clusterednessUnstemmed = 0;
        double clusterednessStemmed;
        double rakeUnstemmed;
        double rakeStemmed;
        int wordCount = 0;
        boolean isKeyphrase;
        
        // Abstract text features
//        String originalText = abs.getOriginalText();
        String stemmedText = abs.getStemmedText();

        // Keyphrase features
        String originalPhrase = phrase.getText();
//        String[] tokenizedOriginalKeyphrase = originalPhrase.split(" ");

        String taggedPhrase = posTagger.tagText(phrase.getText());
        String stemmedPhrase = "";
        try {
            stemmedPhrase = lemmatizer.stemText(taggedPhrase, true);
            if (stemmedPhrase.isEmpty()) {
                stemmedPhrase = phrase.getText();
            }
        }
        catch (Exception e) {
            throw new Exception("Exception in setPhraseFeatures: " + e.getMessage());
        }

//        String[] tokenizedTaggedPhrase = taggedPhrase.split(" ");
//
//        for (String token : tokenizedTaggedPhrase) {
//            posTags = posTags.concat(posTagger.getTag(token) + " ");
//        }
//        posTags = posTags.trim();

//        relativePositionUnstemmed = calculateRelativePosition(originalPhrase, originalText);
//        relativePositionStemmed = calculateRelativePosition(stemmedPhrase, stemmedText);

//        frequencyUnstemmed = calculateFrequency(originalPhrase, originalText);
        frequencyStemmed = calculateFrequency(stemmedPhrase, stemmedText);

        // TFIDF
//        int correctingIntUnstemmed = 0;
//        if (frequencyUnstemmed == 0) {
//            correctingIntUnstemmed = 1;
//        }
        int correctingIntStemmed = 0;
        if (frequencyStemmed == 0) {
            correctingIntStemmed = 1;
        }
//        double tfUnstemmed =
//                (double) (frequencyUnstemmed + correctingIntUnstemmed) / (double) abs.wordCount();
//        double idfUnstemmed =
//                calculateIDF(originalPhrase, trainingAbstracts, abs, true);
//        tfidfUnstemmed =
//                tfUnstemmed * idfUnstemmed; // TFXIDF = (freq(P, D) / size(D)) * -log2(df(P)/N) [Witten et al., 1999]

        double tfStemmed =
                (double) (frequencyStemmed + correctingIntStemmed) / (double) abs.wordCount();
        double idfStemmed =
                calculateIDF(stemmedPhrase, trainingAbstracts, abs, false);
        tfidfStemmed =
                tfStemmed * idfStemmed;     // TFXIDF = (freq(P, D) / size(D)) * -log2(df(P)/N) [Witten et al., 1999];
        
        // RIDF(t) = IDF(t) - PIDF(t) [Timonen, 2013]
        // PIDF(t) = -log(1 - e^-(ctf(t) / size(D))
        // ctf(t) = sum_d(tf(t, d))
//        double pidfUnstemmed = calculatePIDF(originalPhrase, trainingAbstracts, true);
//        ridfUnstemmed = Math.abs(idfUnstemmed - pidfUnstemmed);
        
        double pidfStemmed = calculatePIDF(stemmedPhrase, trainingAbstracts, false);
        ridfStemmed = Math.abs(idfStemmed - pidfStemmed);
        
        // Clusteredness (x^I) [Bookstein, 1974]
        // x^I(t) = ctf(t) - df(t)
//        clusterednessUnstemmed = calculateClusteredness(originalPhrase, trainingAbstracts, true);
        clusterednessStemmed = calculateClusteredness(stemmedPhrase, trainingAbstracts, false);
        
        // RAKE (Use Jython) 
        // In M. W. Berry and J. Kogan (Eds.), Text Mining: Applications and Theory
        rakeUnstemmed = calculateRake(originalPhrase, rakeKeyphrases);
        rakeStemmed = rakeUnstemmed;
        
//        keaFrequencyUnstemmed =
//                calculateKeaFrequency(originalPhrase, abs, abstractManager, true, null);
//        keaFrequencyStemmed =
//                calculateKeaFrequency(stemmedPhrase, abs, abstractManager, false, null);
//
//        wordCount = tokenizedOriginalKeyphrase.length;

//        isKeyphrase = 
//                (abstractManager.isKeyphrase(abs.getAbstractId(), originalPhrase) ||
//                abstractManager.isKeyphrase(abs.getAbstractId(), stemmedPhrase));
        isKeyphrase = false;

        features = 
                new PhraseFeatures(posTags, stemmedPhrase, relativePositionUnstemmed,
                relativePositionStemmed, frequencyUnstemmed, frequencyStemmed, tfidfUnstemmed,
                tfidfStemmed, keaFrequencyUnstemmed, keaFrequencyStemmed, 
                ridfUnstemmed, ridfStemmed, clusterednessUnstemmed, clusterednessStemmed, 
                rakeUnstemmed, rakeStemmed, isKeyphrase, wordCount);
        phrase.setFeatures(features);

//        if (frequencyUnstemmed == 0 && frequencyStemmed == 0) {
//            logger.debug("ERROR IN PhraseFeatures: UNSTEMMED AND STEMMED FREQUENCIES = 0!\nKeyword = "
//                    + phrase.getText() + "\nText = " + abs.getOriginalText(), DebugLevel.ERROR);
//        }

        return features;
    }
    
    public static Feature getFeatureFromErrorDetectingApproach(
            ErrorDetectingApproach errorDetectingApproach) {
        
        if (errorDetectingApproach == null) {
            return null;
        }
        
        switch (errorDetectingApproach) {
            case TFIDF:
                return Feature.TFIDF_STEMMED;
            case RIDF:
                return Feature.RIDF_STEMMED;
            case CLUSTEREDNESS:
                return Feature.CLUSTEREDNESS_STEMMED;
            case RAKE:
                return Feature.RAKE_STEMMED;
            default:
                return null;
        }
            
    }

    // Uses same method as KEA (Witten et al. 1999)
    public static double calculateRelativePosition(String phrase, String text) {

        phrase = phrase.toLowerCase();
        text = text.toLowerCase();
        String[] tokenizedText = text.split(" ");

        int posIndex = text.indexOf(phrase);

        if (posIndex >= 0) {
            String truncatedText = text.substring(0, posIndex);
            String[] tokenizedTruncatedText = truncatedText.split(" ");

            return ((double) tokenizedTruncatedText.length / (double) tokenizedText.length);
        }
        else {
            return -1.0;
        }
    }

    public static int calculateFrequency(String phrase, String text) {
        phrase = phrase.toLowerCase();
        text = text.toLowerCase();

        int count = 0;
        while (text.contains(phrase)) {
            count++;
            text = text.substring(text.indexOf(phrase) + phrase.length());
        }

        return count;
    }

    // IDF = -log2(df(P)/N)
    public static double calculateIDF(String phrase, Collection<Abstract> trainingAbstracts,
            Abstract currAbs, boolean useOriginalTrainingAbstracts) throws Exception {

        phrase = phrase.toLowerCase();
        try {
            double N = trainingAbstracts.size(); // Size of global corpus

            double df = 0.0; // number of documents containing this keyword(keyphrase)

            String abstractText = "";
            for (Abstract abs : trainingAbstracts) {
                
                // Skip current abstract
                if (currAbs.getAbstractId() == abs.getAbstractId()) {
                    continue;
                }
                
                if (useOriginalTrainingAbstracts) {
                    abstractText = abs.getOriginalText().toLowerCase();
                }
                else {
                    abstractText = abs.getStemmedText().toLowerCase();
                }

                if (abstractText.contains(phrase)) {
                    df = df + 1.0;
                }
            }

            if (df == 0.0) {
                df = 1.0;
                N = N + 1.0;
            }

            double idf = -(Math.log(df / N) / Math.log(2.0));
            return idf;
        }
        catch (Exception e) {
            throw new Exception("Exception in calculateIDF: " + e.getMessage());
        }
    }
    
    // PIDF(t) = -log(1 - e^-(ctf(t) / size(D))
    // ctf(t) = sum_d(tf(t, d))
    public static double calculatePIDF(String phrase, Collection<Abstract> trainingAbstracts,
            boolean useOriginalTrainingAbstracts) throws Exception {

        phrase = phrase.toLowerCase();
        try {
            double N = trainingAbstracts.size(); // Size of global corpus

            int ctf = 0; // collection term frequency
            String abstractText = "";
            for (Abstract abs : trainingAbstracts) {
                if (useOriginalTrainingAbstracts) {
                    abstractText = abs.getOriginalText().toLowerCase();
                }
                else {
                    abstractText = abs.getStemmedText().toLowerCase();
                }
                
                ctf += calculateFrequency(phrase, abstractText);
            }
            
            // Set to 1 in case it didn't exist
            if (ctf == 0) {
                ctf = 1;
                N = N + 1.0;
            }
            
            double exp = (ctf / N) * -1;
            exp = Math.exp(exp);
            double logOf = 1 - exp;

            double pidf = (Math.log(logOf) / Math.log(2.0));
            pidf *= -1;
            
            return pidf;
        }
        catch (Exception e) {
            throw new Exception("Exception in calculatePIDF: " + e.getMessage());
        }
    }
    
    // Clusteredness (x^I) [Bookstein, 1974]
    // x^I(t) = ctf(t) - df(t)
    public static double calculateClusteredness(String phrase, Collection<Abstract> trainingAbstracts,
            boolean useOriginalTrainingAbstracts) throws Exception {

        phrase = phrase.toLowerCase();
        try {

            int ctf = 0; // collection term frequency ctf(t)
            int df = 0; // document frequency df(t)
            String abstractText = "";
            for (Abstract abs : trainingAbstracts) {
                if (useOriginalTrainingAbstracts) {
                    abstractText = abs.getOriginalText().toLowerCase();
                }
                else {
                    abstractText = abs.getStemmedText().toLowerCase();
                }
                
                int frequency = calculateFrequency(phrase, abstractText);
                ctf += frequency;
                
                if (frequency > 0) {
                    df++;
                }
            }
            
            // Set to 1 in case it didn't exist
            if (ctf == 0) {
                ctf = 1;
                df += 1;
            }
            
            double clusteredness = ctf - (double) df;
            return clusteredness;
        }
        catch (Exception e) {
            throw new Exception("Exception in calculatePIDF: " + e.getMessage());
        }
    }
    
    // Rose, S., D. Engel, N. Cramer, and W. Cowley (2010). 
    // Automatic keyword extraction from indi-vidual documents. 
    // In M. W. Berry and J. Kogan (Eds.), Text Mining: Applications and Theory.unknown: John Wiley and Sons, Ltd.
    public static double calculateRake(String phrase, 
            HashMap<String, Double> rakeKeyphrases) {
        
        if (rakeKeyphrases == null) {
            return -1.0;
        }
        
        double rakeScore = 0;
        
        // First, check if the RAKE keyphrases contain the exact phrase
        if (rakeKeyphrases.containsKey(phrase)) {
            rakeScore = rakeKeyphrases.get(phrase);
        }
        // Then, check if one of the RAKE keyphrases is contained in the phrase
        else {
            for (String keyphrase : rakeKeyphrases.keySet()) {
                if (phrase.contains(keyphrase)) {
                    rakeScore = rakeKeyphrases.get(keyphrase);
                    break;
                }
            }
        }
        
        return rakeScore;
    }

    public static int calculateKeaFrequency(String phrase, Abstract currAbs,
            AbstractManager abstractManager, boolean useOriginalTrainingAbstracts,
            String abstractsSource) throws Exception {

        phrase = phrase.toLowerCase();

        try {

            String currAbstractText = currAbs.getOriginalText().toLowerCase();
            currAbstractText = currAbstractText.replaceAll("'", "\\\\'");
            currAbstractText = currAbstractText.replaceAll("\"", "\\\\\"");
            phrase = phrase.replaceAll("'", "\\\\'");
            phrase = phrase.replaceAll("\"", "\\\\\"");

            String sourceSelect = "";
            if (abstractsSource != null) {
                sourceSelect = " AND A.Abstract_Source = '" + abstractsSource + "' ";
            }

            String queryString;
            if (useOriginalTrainingAbstracts) {
                queryString = "SELECT COUNT(B.Keyword) "
                        + "FROM Abstract A, Abstract_Real_Keyword B "
                        + "WHERE A.Abstract_Id = B.Abstract_Id AND A.Abstract_Type = 'Testing' "
                        + "AND LOWER(B.Keyword) = '" + phrase + "' " + sourceSelect
                        + "AND LOWER(A.Abstract_Text) != '" + currAbstractText + "'";
            }
            else {
                queryString = "SELECT COUNT(B.Keyword) "
                        + "FROM Abstract A, Abstract_Real_Keyword B "
                        + "WHERE A.Abstract_Id = B.Abstract_Id AND A.Abstract_Type = 'Testing' "
                        + "AND LOWER(B.Stemmed_Keyword) = '" + phrase + "' " + sourceSelect
                        + "AND LOWER(A.Abstract_Text) != '" + currAbstractText + "'";
            }

            ResultSet rs = abstractManager.databaseManager.executeQuery(queryString);
            rs.next();

            return rs.getInt(1);
        }
        catch (Exception e) {
            throw new Exception("Exception in calculateKeaFrequency: " + e.getMessage());
        }
    }

    public double getFeatureValue(Feature feature, KeyPhrase keyPhrase) {
        switch (feature) {
            case FREQUENCY_UNSTEMMED:
                return frequencyUnstemmed;
            case FREQUENCY_STEMMED:
                return frequencyStemmed;
            case WORD_COUNT:
                return wordCount;
            case RELATIVE_POSITION_UNSTEMMED:
                return relativePositionUnstemmed;
            case RELATIVE_POSITION_STEMMED:
                return relativePositionStemmed;
            case TFIDF_UNSTEMMED:
                return tfidfUnstemmed;
            case TFIDF_STEMMED:
                return tfidfStemmed;
            case RIDF_UNSTEMMED:
                return ridfUnstemmed;
            case RIDF_STEMMED:
                return ridfStemmed;
            case CLUSTEREDNESS_UNSTEMMED:
                return clusterednessUnstemmed;
            case CLUSTEREDNESS_STEMMED:
                return clusterednessStemmed;
            case RAKE_UNSTEMMED:
                return rakeUnstemmed;
            case RAKE_STEMMED:
                return rakeStemmed;
            case RANKUP_SCORE:
                return keyPhrase.getScore();
            case ORIGINAL_SCORE:
                return keyPhrase.getOriginalTextRankScore();
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        String output = "";
        output = output.concat(
                //"FREQ_STEM: " + MiscUtils.convertDoubleTo4CharacterString(frequencyStemmed) + 
                "TFIDF_STEM: " + MiscUtils.convertDoubleToFixedCharacterString(tfidfStemmed, 2) +
                ", RIDF_STEM: " + MiscUtils.convertDoubleToFixedCharacterString(ridfStemmed, 2)  +
                ", CLUST_STEM: " + MiscUtils.convertDoubleToFixedCharacterString(clusterednessStemmed, 2) +
                ", RAKE_STEM: " + MiscUtils.convertDoubleToFixedCharacterString(rakeStemmed, 2) +
                ", IS_KEY: (" + MiscUtils.getBooleanCheckString(isKeyphrase) + ")");

        return output;
    }
}
