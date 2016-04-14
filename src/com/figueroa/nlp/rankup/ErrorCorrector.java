package com.figueroa.nlp.rankup;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import com.figueroa.util.ExceptionLogger;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Node;

/**
 * Static class.
 * The ErrorCorrector is the core of RankUp. It corrects errors detected
 * in original keyphrase scores. This super class contains common functions and
 * variables shared by subclasses.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * March 2013
 */
public abstract class ErrorCorrector {

    protected static final String loggerDir =
            "." + File.separator + "logs" + File.separator + "exception_log" +
            System.currentTimeMillis() + ".log";
    protected static final ExceptionLogger logger =
            new ExceptionLogger(loggerDir, Main.DEBUG_LEVEL);

    // Property
    public static enum ConvergenceScheme {
        MIN, MAX, AVERAGE, SSE, TEXTRANK
    }
    
    // Property
    public static enum ConvergenceRule {
        NO_INCREASE, NO_INCREASE_2X
    }

    // Property
    public static enum WeightUpdatingScheme {
        SUM, MIN, MAX, AVERAGE
    }
    public static final WeightUpdatingScheme WEIGHT_UPDATING_SCHEME = 
            WeightUpdatingScheme.AVERAGE; // Fixed

    protected static class EdgeVector {
        Double weight1;
        Double weight2;

        public EdgeVector() {
            weight1 = null;
            weight2 = null;
        }
    }

    /**
     * Get a ConvergenceScheme enum type from a String
     * @param schemeString
     * @return a ConvergenceScheme
     */
    public static ConvergenceScheme getConvergenceSchemeFromString(
            String schemeString) {

        if (schemeString.equalsIgnoreCase("MIN")) {
            return ConvergenceScheme.MIN;
        }
        else if (schemeString.equalsIgnoreCase("MAX")) {
            return ConvergenceScheme.MAX;
        }
        else if (schemeString.equalsIgnoreCase("AVERAGE")) {
            return ConvergenceScheme.AVERAGE;
        }
        else if (schemeString.equalsIgnoreCase("SSE")) {
            return ConvergenceScheme.SSE;
        }
        else if (schemeString.equalsIgnoreCase("TEXTRANK")) {
            return ConvergenceScheme.TEXTRANK;
        }
        else {
            return null;
        }
    }
    
    /**
     * Get a ConvergenceRule enum type from a String
     * @param ruleString
     * @return a ConvergenceRule
     */
    public static ConvergenceRule getConvergenceRuleFromString(
            String ruleString) {
        
        if (ruleString.equalsIgnoreCase("NO_INCREASE")) {
            return ConvergenceRule.NO_INCREASE;
        }
        else if (ruleString.equalsIgnoreCase("NO_INCREASE_2X")) {
            return ConvergenceRule.NO_INCREASE_2X;
        }
        else {
            return null;
        }
    }

    /**
     * Get a WeightUpdatingScheme enum type from a String
     * @param schemeString
     * @return a WeightUpdatingScheme
     */
    public static WeightUpdatingScheme getWeightUpdatingSchemeFromString(
            String schemeString) {

        if (schemeString.equalsIgnoreCase("SUM")) {
            return WeightUpdatingScheme.SUM;
        }
        else if(schemeString.equalsIgnoreCase("MIN")) {
            return WeightUpdatingScheme.MIN;
        }
        else if (schemeString.equalsIgnoreCase("MAX")) {
            return WeightUpdatingScheme.MAX;
        }
        else if (schemeString.equalsIgnoreCase("AVERAGE")) {
            return WeightUpdatingScheme.AVERAGE;
        }
        else {
            return null;
        }
    }

    /**
     * Update the graph's edge weights in 1 of 4 possible Weight Updating Schemes:
     * SUM, MIN, MAX or AVERAGE. The scheme is set to a constant value of AVERAGE
     * for practicality purposes.
     * The edge weights need to be consolidated because the graph is, in theory,
     * undirected. In the code, however, it has been implemented as a directed graph.
     * @param graph: the original graph
     * @param graphEdges: the new edges of the graph to which the graph will be updated
     * @param correctNegativeWeights: bug fix
     * @throws Exception 
     */
    protected static void updateEdgeWeights(Map<String, ? extends Node> graph,
            HashMap<String, EdgeVector> graphEdges, boolean correctNegativeWeights) 
            throws Exception {

        String lastKey = null; // For debugging
        try {
            for (String key : graphEdges.keySet()) {

                lastKey = key; // For debugging
                EdgeVector edgeVector = graphEdges.get(key);
                String[] nodeKeys = key.split("\\|");
                String node1Key = nodeKeys[0];
                String node2Key = nodeKeys[1];
                Node node1 = graph.get(node1Key);
                Node node2 = graph.get(node2Key);

                // Set weights to 0 if negative
                if (correctNegativeWeights) {
                    edgeVector.weight1 = edgeVector.weight1 < 0 ? 0 : edgeVector.weight1;
                    edgeVector.weight2 = edgeVector.weight2 < 0 ? 0 : edgeVector.weight2;
                }
                
                double newWeight = 0;
                switch (WEIGHT_UPDATING_SCHEME) {
                    case SUM:
                        newWeight = edgeVector.weight1 + edgeVector.weight2;
                        break;
                    case MIN:
                        newWeight = Math.min(edgeVector.weight1, edgeVector.weight2);
                        break;
                    case MAX:
                        newWeight = Math.max(edgeVector.weight1, edgeVector.weight2);
                        break;
                    case AVERAGE:
                        newWeight = (edgeVector.weight1 + edgeVector.weight2) / 2;
                        break;
                }
                
                // Set newWeight to 0 if it is negative
                //newWeight = newWeight < 0 ? 0 : newWeight;
                
                node1.setEdgeWeight(node2, newWeight);
                node2.setEdgeWeight(node1, newWeight);
            }
        }
        catch (Exception e) {
            throw new Exception("Exception in updateEdgeWeights (" + e.getMessage() +
                    "), key: " + lastKey);
        }
    }

    /**
     * Get the standard error of the iteration in 1 of 4 possible Convergence Schemes
     * @param statistics: the set of error values (d_j) in the current iteration
     * @param convergenceScheme: the way in which the set of error values is summarized.
     * In the RankUp paper, the SSE scheme is used.
     * @return 
     */
    protected static double getStandardError(SummaryStatistics statistics,
            ConvergenceScheme convergenceScheme) {
        
        // Return really high value if statistics are empty
        if (statistics.getN() == 0) {
            return Double.MAX_VALUE;
        }

        double standardError = 0;
        
        switch (convergenceScheme) {
            case MAX:
                standardError = statistics.getMax();
                break;
            case AVERAGE:
                standardError = statistics.getMean();
                break;
            case SSE:
                standardError = statistics.getSumsq() * 0.5;
                break;
            case TEXTRANK:
                standardError = statistics.getStandardDeviation() / Math.sqrt((double) statistics.getN());
                break;
        }

        return standardError;
    }
    
    /**
     * Checks whether convergence has been reached in the RankUp algorithm, using
     * 1 of 2 possible Convergence Rules. If convergence is reached, the iterations are
     * is stopped.
     * @param standardErrorThreshold: a value greater than 0.
     * @param currentStandardError
     * @param previousStandardError
     * @param convergenceRule: overrides the error threshold by checking if the 
     * currentStandardError is greater than or equal to the 
     * previousStandardError (NO_INCREASE) or greater than or equal to twice of the
     * previousStandardError (NO_INCREASE_2X). The latter allows for potentially 
     * more iterations.
     * @param keyPhraseGraph
     * @param graphNodes
     * @param revertGraphs: whether or not to revert the graphs after convergence
     * to their previous state (due to possible bug)
     * @param useDifferentialConvergence: whether or not to use differential convergence.
     * With differential convergence, the previousStandardError is first subtracted from
     * the currentStandardError, and the resulting value is compared to the threshold.
     * Without differential convergence, the currentStandardError is directly compared
     * to the threshold.
     * @return 
     */
    protected static boolean verifyConvergence(
            double standardErrorThreshold, 
            double currentStandardError, 
            double previousStandardError,
            ConvergenceRule convergenceRule, 
            KeyPhraseGraph keyPhraseGraph, 
            Collection<? extends Node> graphNodes, 
            boolean revertGraphs,
            boolean useDifferentialConvergence) {
            
            // First check threshold
            if (useDifferentialConvergence) {
                if (Math.abs(currentStandardError - previousStandardError) < standardErrorThreshold) {
                    logger.debug("Break iteration (Standard error - Previous error < threshold) ", 
                            ExceptionLogger.DebugLevel.DEBUG);
                    return true;
                }
            }
            else {
                if (currentStandardError <= standardErrorThreshold) {
                    logger.debug("Break iteration (Standard error <= Standard error threshold) ", 
                            ExceptionLogger.DebugLevel.DEBUG);
                    return true;
                }
            }
            
            // Check if currentStandardError is a number
            if (Double.isNaN(currentStandardError)) {
                logger.debug("Break iteration (Standard error is NaN) ", 
                        ExceptionLogger.DebugLevel.DEBUG);
                // Always revert graphs
                revertGraphs(keyPhraseGraph, graphNodes);
                return true;
            }
            
            // Convergence Rule
            switch (convergenceRule) {
                case NO_INCREASE:
                    if (currentStandardError >= previousStandardError) {
                        logger.debug("Break iteration (Current standard error > Previous standard error) ", 
                                ExceptionLogger.DebugLevel.DEBUG);
                        
                        // Revert the graphs
                        if (revertGraphs) {
                            revertGraphs(keyPhraseGraph, graphNodes);
                        }
                        return true;
                    }
                case NO_INCREASE_2X:
                    if (currentStandardError >= (previousStandardError * 2)) {
                        logger.debug("Break iteration (Current standard error >= Previous standard error x 2) ", 
                                ExceptionLogger.DebugLevel.DEBUG);
                        
                        // Revert the graphs
                        if (revertGraphs) {
                            revertGraphs(keyPhraseGraph, graphNodes);
                        }
                        return true;
                    }
            }
            
            return false;
    }
    
    /**
     * Reverts the keyphrases and nodes to their previous states.
     * @param keyPhraseGraph
     * @param textRankGraph 
     */
    private static void revertGraphs(
            KeyPhraseGraph keyPhraseGraph, 
            Collection<? extends Node> graphNodes) {
        
        logger.debug("Reverting graphs...", ExceptionLogger.DebugLevel.DEBUG);
        for (Node node : graphNodes) {
            KeyPhrase keyphrase = keyPhraseGraph.get(node.key);
            if (keyphrase != null) {
                keyphrase.revertKeyPhrase();
            }
            node.revertNode();
        }
    }

}
