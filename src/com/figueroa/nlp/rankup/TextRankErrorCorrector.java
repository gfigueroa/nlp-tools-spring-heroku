package com.figueroa.nlp.rankup;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Node;
import com.figueroa.nlp.textrank.MetricVector;
import com.figueroa.nlp.textrank.NGram;
import com.figueroa.nlp.textrank.TextRank;
import com.figueroa.nlp.textrank.TextRankGraph;
import com.figueroa.util.Abstract;
import com.figueroa.util.ExceptionLogger.DebugLevel;

/**
 * Static class.
 * The TextRankErrorCorrector is the core of RankUp. It corrects errors detected
 * in original keyphrase scores.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * March 2013
 */
public class TextRankErrorCorrector extends ErrorCorrector {

    // Calculate d_j for every node in the graph (Step 1)
    // New version (using whole TR graph)
    private static void calculateDifferentials(TextRankGraph textRankGraph, 
            SummaryStatistics statistics, ConvergenceScheme convergenceScheme) {
        
        // For every node in the TextRank graph
        for (Node graphNode : textRankGraph.values()) {

            double previous_d_j = graphNode.get_d_j();
            double d_j = 0; // equation 29
            double A_j = graphNode.getRank(); // Original TextRank score
            double T_j = graphNode.getExpectedScore(); // Expected score

            // If the node is in the expected score list
            if (T_j >= 0) {
                d_j = T_j - A_j;
            }
            // If the node is not in the expected score list
            else {
                for (Node edgeNode : graphNode.getEdges().keySet()) {

                    double d_k = edgeNode.get_d_j();
                    double w_jk = graphNode.getEdges().get(edgeNode);

                    // Calculate normalized w_jk
                    double denominator = 0.0;
                    for (double weight : edgeNode.getEdges().values()) {
                        denominator += weight;
                    }
                    double normalized_w_jk = edgeNode.getEdges().get(graphNode) /
                            denominator;

                    d_j += d_k * normalized_w_jk;
                    //d_j += d_k * w_jk;
                }
                d_j *= TextRankGraph.TEXTRANK_DAMPING_FACTOR;
            }
            graphNode.set_d_j(d_j);

            switch (convergenceScheme) {
                case MAX:
                    statistics.addValue(Math.abs(d_j));
                    break;
                case AVERAGE:
                    statistics.addValue(Math.abs(previous_d_j - d_j));
                    break;
                case SSE:
                    statistics.addValue(d_j);
                    break;
                case TEXTRANK:
                    statistics.addValue(d_j);
                    break;
            }
        }
    }
    
    // Calculate d_j for every node in the graph (Step 1)
    private static void calculateDifferentials(KeyPhraseGraph keyPhraseGraph,
            TextRankGraph textRankGraph, SummaryStatistics statistics, 
            ConvergenceScheme convergenceScheme) {

        // For every node in the TextRank graph
        for (Node graphNode : textRankGraph.values()) {

            KeyPhrase keyPhraseNode = keyPhraseGraph.get(graphNode.key);

            double previous_d_j = graphNode.get_d_j();
            double d_j = 0; // equation 29
            double A_j = keyPhraseNode != null ? keyPhraseNode.getScore()
                    : graphNode.getRank(); // Original TextRank score
            double T_j = graphNode.getExpectedScore(); // Expected score

            // If the node is in the expected score list
            if (T_j >= 0) {
                d_j = T_j - A_j;
            }
            // If the node is not in the expected score list
            else {
                for (Node edgeNode : graphNode.getEdges().keySet()) {

                    double d_k = edgeNode.get_d_j();
                    double w_jk = graphNode.getEdges().get(edgeNode);

                    // Calculate normalized w_jk
                    double denominator = 0.0;
                    for (double weight : edgeNode.getEdges().values()) {
                        denominator += weight;
                    }
                    double normalized_w_jk = edgeNode.getEdges().get(graphNode) /
                            denominator;

                    d_j += d_k * normalized_w_jk;
                    //d_j += d_k * w_jk;
                }
                d_j *= TextRankGraph.TEXTRANK_DAMPING_FACTOR;
            }
            graphNode.set_d_j(d_j);

            switch (convergenceScheme) {
                case MAX:
                    statistics.addValue(Math.abs(d_j));
                    break;
                case AVERAGE:
                    statistics.addValue(Math.abs(previous_d_j - d_j));
                    break;
                case SSE:
                    statistics.addValue(d_j);
                    break;
                case TEXTRANK:
                    statistics.addValue(d_j);
                    break;
            }
        }
    }

    // Modify each edge weight in the graph (Step 2)
    // New version (using whole TR graph)
    private static void modifyEdgeWeights(
            TextRankGraph textRankGraph, double learningRate, boolean correctNegativeWeights,
            boolean denormalizeModificationValue) 
            throws Exception {

        HashMap<String, EdgeVector> graphEdges = new HashMap<>();

        for (Node graphNode : textRankGraph.values()) {

            // Calculate denormalization denominator
            double denormalizationDenominator = 0;
            if (denormalizeModificationValue) {
                for (Node edgeNode : graphNode.getEdges().keySet()) {
                    denormalizationDenominator += graphNode.getEdges().get(edgeNode);
                }
            }
            else {
                denormalizationDenominator = 1;
            }
            
            for (Node edgeNode : graphNode.getEdges().keySet()) {

                // Check that edgeNode is still in TextRank TextRankGraph
                if (textRankGraph.get(edgeNode.key) == null) {
                    continue;
                }

                double d_j = edgeNode.get_d_j(); // edge node differential
                double A_i = graphNode.getRank(); // graph node textrank score

                double delta_normalized_w_ij =
                        learningRate *
                        d_j *
                        TextRankGraph.TEXTRANK_DAMPING_FACTOR *
                        A_i;
                
                // Denormalize (if required)
                delta_normalized_w_ij *= denormalizationDenominator;

                double previousWeight = graphNode.getEdges().get(edgeNode);
                double newWeight = previousWeight + delta_normalized_w_ij;

                // Construct graphEdge
                String graphNodeEdgeNodeKey = graphNode.key + "|" + edgeNode.key;
                String edgeNodeGraphNodeKey = edgeNode.key + "|" + graphNode.key;
                String finalKey;
                EdgeVector edgeVector;
                // First time
                if (graphEdges.get(graphNodeEdgeNodeKey) == null &&
                        graphEdges.get(edgeNodeGraphNodeKey) == null) {
                    finalKey = graphNodeEdgeNodeKey;
                    edgeVector = new EdgeVector();
                    edgeVector.weight1 = newWeight;
                    edgeVector.weight2 = newWeight; // For self-connected nodes
                }
                // Second time
                else {
                    finalKey = edgeNodeGraphNodeKey;
                    edgeVector = graphEdges.get(edgeNodeGraphNodeKey);
                    edgeVector.weight2 = newWeight;
                }
                graphEdges.put(finalKey, edgeVector);
            }
        }

        updateEdgeWeights(textRankGraph, graphEdges, correctNegativeWeights);
    }
    
    // Modify each edge weight in the graph (Step 2)
    private static void modifyEdgeWeights(KeyPhraseGraph keyPhraseGraph,
            TextRankGraph textRankGraph, double learningRate, boolean correctNegativeWeights) 
            throws Exception {

        HashMap<String, EdgeVector> graphEdges = new HashMap<>();

        for (Node graphNode : textRankGraph.values()) {

            KeyPhrase keyPhraseNode = keyPhraseGraph.get(graphNode.key);

            for (Node edgeNode : graphNode.getEdges().keySet()) {

                // Check that edgeNode is still in TextRank TextRankGraph
                if (textRankGraph.get(edgeNode.key) == null) {
                    continue;
                }

                double d_j = edgeNode.get_d_j(); // edge node differential
                double A_i = keyPhraseNode != null ? keyPhraseNode.getScore() :
                    graphNode.getRank(); // graph node textrank score
//                double A_i = keyPhraseNode != null ? keyPhraseNode.getNode().rank :
//                    graphNode.rank; // graph node textrank score

                double delta_normalized_w_ij =
                        learningRate *
                        d_j *
                        TextRankGraph.TEXTRANK_DAMPING_FACTOR *
                        A_i;

                double previousWeight = graphNode.getEdges().get(edgeNode);
                double newWeight = previousWeight + delta_normalized_w_ij;

                // Construct graphEdge
                String graphNodeEdgeNodeKey = graphNode.key + "|" + edgeNode.key;
                String edgeNodeGraphNodeKey = edgeNode.key + "|" + graphNode.key;
                String finalKey;
                EdgeVector edgeVector;
                // First time
                if (graphEdges.get(graphNodeEdgeNodeKey) == null &&
                        graphEdges.get(edgeNodeGraphNodeKey) == null) {
                    finalKey = graphNodeEdgeNodeKey;
                    edgeVector = new EdgeVector();
                    edgeVector.weight1 = newWeight;
                    edgeVector.weight2 = newWeight; // For self-connected nodes
                }
                // Second time
                else {
                    finalKey = edgeNodeGraphNodeKey;
                    edgeVector = graphEdges.get(edgeNodeGraphNodeKey);
                    edgeVector.weight2 = newWeight;
                }
                graphEdges.put(finalKey, edgeVector);
            }
        }

        updateEdgeWeights(textRankGraph, graphEdges, correctNegativeWeights);
    }

    public static int performErrorFeedback(
            Abstract abs,
            KeyPhraseGraph keyPhraseGraph, 
            TextRank textRank, 
            boolean useWholeTextRankGraph,
            double learningRate, 
            double standardErrorThreshold,
            ConvergenceScheme convergenceScheme, 
            ConvergenceRule convergenceRule,
//            WeightUpdatingScheme weightUpdatingScheme, 
            boolean revertGraphs,
            boolean printGephiGraphs,
            boolean correctNegativeWeights,
            boolean denormalizeModificationValue,
            boolean useDifferentialConvergence) 
            throws Exception {

        HashMap<NGram, MetricVector> metric_space;
        TextRankGraph textRankGraph = textRank.getGraph();

        // Modify Edge Weights
        // Either run through N iterations, until the standard
        // error converges below a threshold, or until the standard error increases
        // compared to the previous iteration
        int iteration = 0;
        int maxIterations = textRankGraph.size();
        SummaryStatistics statistics = new SummaryStatistics();
        double previousStandardError;
        for (; iteration < maxIterations; iteration++) {
            
            // Print keyphrase graph on every round (DETAIL)
//            textRankGraph.printGraph(keyPhraseGraph, logger); 
//            keyPhraseGraph.printGraph(false);
//            Main.printKeyPhrases(keyPhraseGraph.getSortedKeyphrases(
//                            useWholeTextRankGraph,
//                            true), "RankUp", false);
            if (printGephiGraphs) {
                textRankGraph.printGephiGraph(abs.getAbstractId(), iteration);
            }

            previousStandardError = getStandardError(statistics, convergenceScheme);
            statistics.clear();

            // Step 1: Calculate d_j for every node
            if (useWholeTextRankGraph) {
                calculateDifferentials(textRankGraph, statistics, convergenceScheme);
            }
            else {
                calculateDifferentials(keyPhraseGraph, textRankGraph, statistics, convergenceScheme);
            }

            // Step 1.5: Verify convergence
            double currentStandardError = getStandardError(statistics, convergenceScheme);
            logger.debug("Iteration: " + iteration + "\tStandard error: " +
                    currentStandardError, DebugLevel.DEBUG);
            if (verifyConvergence(standardErrorThreshold, currentStandardError, 
                    previousStandardError, convergenceRule, keyPhraseGraph,
                    textRankGraph.values(), revertGraphs, useDifferentialConvergence)) {
                
                // If iteration 0, RankUp scores will be same as TextRank scores (instead of -1)
                if (iteration == 0) {
                    for (KeyPhrase keyphrase : keyPhraseGraph.values()) {
                        keyphrase.setFinalTextRankScore(keyphrase.getOriginalTextRankScore());
                    }
                }
                
                break;
            }

            // Step 2: Modify edge weights
            if (useWholeTextRankGraph) {
                modifyEdgeWeights(textRankGraph, learningRate, correctNegativeWeights,
                        denormalizeModificationValue);
            }
            else {
                modifyEdgeWeights(keyPhraseGraph, textRankGraph, learningRate, correctNegativeWeights);
            }

            // Step 3: Rerun TextRank
            textRankGraph.runTextRank(false, logger);

            // Step 4: Recalculate Metrics
            metric_space = textRank.calculateMetrics();
            
            // Step 5: Map metric vectors with RankUp keyphrase nodes
            Collection<MetricVector> metricVectorCollection = metric_space.values();
            TreeSet<MetricVector> metricSpace = new TreeSet<>(metricVectorCollection);

            // Step 6: Reassign scores
            for (MetricVector node : metricSpace) {
                KeyPhrase keyphrase = keyPhraseGraph.get(node.value.getParentNode().key);
                if (keyphrase == null) {
                    continue;
                }
                keyphrase.setFinalTextRankScore(node.metric);

                if (!useWholeTextRankGraph) {
                    keyphrase.setScore(node.metric);
                }
            }
            // Reassign node ranks
            if (useWholeTextRankGraph) {
                for (KeyPhrase keyphrase : keyPhraseGraph.values()) {
                    keyphrase.setScore(keyphrase.getNode().getRank());
                }
            }
        }

        return iteration;
    }

}
