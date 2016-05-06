package com.figueroa.nlp.rankup;

import java.util.Properties;
import com.figueroa.nlp.rankup.ErrorCorrector.ConvergenceRule;
import com.figueroa.nlp.rankup.ErrorCorrector.ConvergenceScheme;
import com.figueroa.nlp.rankup.ErrorDetector.ErrorDetectingApproach;
import com.figueroa.nlp.rankup.ErrorDetector.ExpectedScoreValue;
import com.figueroa.nlp.rankup.GraphBasedKeywordExtractor.GraphBasedKeywordExtractionMethod;
import com.figueroa.nlp.rankup.KeyPhraseGraph.SetAssignmentApproach;
import com.figueroa.util.Abstract;
import com.figueroa.util.Abstract.Type;

/**
 * Properties for RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * June 2013
 */
public class RankUpProperties {

    public final String propertiesFileName;
    public final String abstractSource;
    public final Type abstractType;
    public final boolean useWholeTextRankGraph;
    public final boolean postprocess;
    public final ErrorDetectingApproach errorDetectingApproach;
    public final SetAssignmentApproach setAssignmentApproach;
    public final double featureLowerBound;
    public final double featureUpperBound;
    public final ExpectedScoreValue expectedScoreValue;
    public final double learningRate;
    public final double standardErrorThreshold;
    public final ConvergenceScheme convergenceScheme;
//    public final WeightUpdatingScheme weightUpdatingScheme;
    public final ConvergenceRule convergenceRule;
    public final boolean revertGraphs;
    public final GraphBasedKeywordExtractionMethod keywordExtractionMethod;

    public RankUpProperties(String propertiesFileName, Properties props) 
            throws Exception {

        this.propertiesFileName = propertiesFileName;
        
        try {
            this.abstractSource = props.getProperty("abstract_source");
            this.abstractType =
                    Abstract.getTypeFromString(props.getProperty("abstract_type"));
            this.useWholeTextRankGraph =
                    Boolean.parseBoolean(props.getProperty("use_whole_textrank_graph"));
            this.postprocess = Boolean.parseBoolean(props.getProperty("postprocess"));
            this.errorDetectingApproach =
                    ErrorDetector.getErrorDetectingApproachFromString(
                            props.getProperty("error_detecting_approach"));
            this.setAssignmentApproach =
                    KeyPhraseGraph.getSetAssignmentApproachFromString(
                            props.getProperty("set_assignment_approach"));
            this.featureLowerBound =
                    Double.parseDouble(props.getProperty("feature_lower_bound"));
             this.featureUpperBound =
                    Double.parseDouble(props.getProperty("feature_upper_bound"));
            this.expectedScoreValue =
                    ErrorDetector.getExpectedScoreValueFromString(
                            props.getProperty("expected_score_value"));
            this.learningRate = Double.parseDouble(props.getProperty("learning_rate"));
            this.standardErrorThreshold =
                    Double.parseDouble(props.getProperty("standard_error_threshold"));
            this.convergenceScheme =
                    ErrorCorrector.getConvergenceSchemeFromString(
                            props.getProperty("convergence_scheme"));
//            this.weightUpdatingScheme =
//                    TextRankErrorCorrector.getWeightUpdatingSchemeFromString(
//                    props.getProperty("weight_updating_scheme"));
            this.convergenceRule = 
                    ErrorCorrector.getConvergenceRuleFromString(
                            props.getProperty("convergence_rule"));
            this.revertGraphs = 
                    Boolean.parseBoolean(props.getProperty("revert_graphs"));
            this.keywordExtractionMethod =
                    GraphBasedKeywordExtractor.getGraphBasedKeywordExtractionMethodFromString(
                            props.getProperty("keyword_extraction_method"));
        }
        catch (NumberFormatException e) {
            throw new Exception("Error parsing properties file!");
        }
    }

    public RankUpProperties(
            String propertiesFileName,
            String abstractSource,
            Type abstractType,
            boolean useWholeTextRankGraph,
            boolean postprocess,
            ErrorDetectingApproach errorDetectingApproach,
            SetAssignmentApproach setAssignmentApproach,
            double featureLowerBound,
            double featureUpperBound,
            ExpectedScoreValue expectedScoreValue,
            double learningRate,
            double standardErrorThreshold,
            ConvergenceScheme convergenceScheme,
//            WeightUpdatingScheme weightUpdatingScheme,
            ConvergenceRule convergenceRule,
            boolean revertGraphs,
            GraphBasedKeywordExtractionMethod keywordExtractionMethod) {

            this.propertiesFileName = propertiesFileName;
            this.abstractSource = abstractSource;
            this.abstractType = abstractType;
            this.useWholeTextRankGraph = useWholeTextRankGraph;
            this.postprocess = postprocess;
            this.errorDetectingApproach = errorDetectingApproach;
            this.setAssignmentApproach = setAssignmentApproach;
            this.featureLowerBound = featureLowerBound;
            this.featureUpperBound = featureUpperBound;
            this.expectedScoreValue = expectedScoreValue;
            this.learningRate = learningRate;
            this.standardErrorThreshold = standardErrorThreshold;
            this.convergenceScheme = convergenceScheme;
//            this.weightUpdatingScheme = weightUpdatingScheme;
            this.convergenceRule = convergenceRule;
            this.revertGraphs = revertGraphs;
            this.keywordExtractionMethod = keywordExtractionMethod;
    }

    @Override
    public String toString() {
        String string = "";

        string += "ABSTRACT_SOURCE = " + abstractSource + "\n";
        string += "ABSTRACT_TYPE = " + abstractType + "\n";
        string += "USE_WHOLE_TEXTRANK_GRAPH: " + useWholeTextRankGraph + "\n";
        string += "POSTPROCESS = " + postprocess + "\n";
        string += "ERROR_DETECTING_APPROACH = " + errorDetectingApproach + "\n";
        string += "SET_ASSIGNMENT_APPROACH = " + setAssignmentApproach + "\n";
        string += "FEATURE_LOWER_BOUND = " + featureLowerBound + "\n";
        string += "FEATURE_UPPER_BOUND = " + featureUpperBound + "\n";
        string += "EXPECTED_SCORE_VALUE = " + expectedScoreValue + "\n";
        string += "LEARNING_RATE = " + learningRate + "\n";
        string += "STANDARD_ERROR_THRESHOLD = " + standardErrorThreshold + "\n";
        string += "CONVERGENCE_SCHEME = " + convergenceScheme + "\n";
//        string += "WEIGHT_UPDATING_SCHEME = " + weightUpdatingScheme + "\n";
        string += "CONVERGENCE_RULE = " + convergenceRule + "\n";
        string += "REVERT_GRAPHS = " + revertGraphs + "\n";
        string += "KEYWORD_EXTRACTION_METHOD = " + keywordExtractionMethod;

        return string;
    }

}
