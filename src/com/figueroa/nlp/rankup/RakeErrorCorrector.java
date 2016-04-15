package com.figueroa.nlp.rankup;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.Node;
import com.figueroa.nlp.rake.Rake;
import com.figueroa.nlp.rake.RakeNode;
import com.figueroa.nlp.rake.RakeNode.RakeNodeType;
import com.figueroa.util.Abstract;

/**
 * Static class.
 * The RakeErrorCorrector is the core of RankUp. It corrects errors detected
 * in original keyphrase scores.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2015
 */
public class RakeErrorCorrector extends ErrorCorrector {

	private static final Logger logger = Logger.getLogger(RakeErrorCorrector.class);
	
    // Calculate d_j for every node in the graph (Step 1)
    private static void calculateDifferentials(List<RakeNode> wordNodes, 
            SummaryStatistics statistics, ConvergenceScheme convergenceScheme) {
        
        // For every node in the TextRank graph
        for (Node graphNode : wordNodes) {
            
            double previous_d_j = graphNode.get_d_j();
            double d_j; // equation 29
            double A_j = graphNode.getRank(); // Original score
            double T_j = graphNode.getExpectedScore(); // Expected score

            // If the node is in the expected score list
            if (T_j >= 0) {
                d_j = T_j - A_j;
            }
            // If the node is not in the expected score list
            else {
                d_j = 0;
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
    private static void modifyEdgeWeights(
            List<RakeNode> wordNodes, HashMap<String, RakeNode> rakeGraph,
            double learningRate, boolean correctNegativeWeights) 
            throws Exception {

        HashMap<String, EdgeVector> graphEdges = new HashMap<>();

        for (Node graphNode : wordNodes) {

            double d_i = graphNode.get_d_j(); // graph node differential
            double w_ii = graphNode.getEdges().get(graphNode); // frequency
            
            for (Node edgeNode : graphNode.getEdges().keySet()) {
                
                if (graphNode.equals(edgeNode)) {
                    continue;
                }
                
                // Only do it for Word nodes
                RakeNode rakeEdgeNode = (RakeNode) edgeNode;
                if (rakeEdgeNode.type != RakeNodeType.WORD) {
                    continue;
                }
                
                // New formula for RAKE
                double delta_w_ij = (learningRate * d_i) / w_ii;

                double previousWeight = graphNode.getEdges().get(edgeNode);
                double newWeight = previousWeight + delta_w_ij;

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

        updateEdgeWeights(rakeGraph, graphEdges, correctNegativeWeights);
    }

    public static int performErrorFeedback(
            Abstract abs,
            List<KeyPhrase> keyphraseList,
            KeyPhraseGraph keyPhraseGraph, 
            Rake rake, 
            double learningRate, 
            double standardErrorThreshold,
            ConvergenceScheme convergenceScheme, 
            ConvergenceRule convergenceRule,
            boolean revertGraphs,
            boolean printGephiGraphs,
            boolean correctNegativeWeights) 
            throws Exception {
        
        HashMap<String, RakeNode> rakeGraph = rake.rakeFullGraph;
        List<RakeNode> rakeWordNodes = rake.getWordNodes();

        // Modify Edge Weights
        // Either run through N iterations, until the standard
        // error converges below a threshold, or until the standard error increases
        // compared to the previous iteration
        int iteration = 0;
        int maxIterations = rakeWordNodes.size();
        SummaryStatistics statistics = new SummaryStatistics();
        double previousStandardError;
        for (; iteration < maxIterations * 10; iteration++) {

            // Print keyphrase graph on every round (DETAIL)
            if (printGephiGraphs) {
                rake.printRakeGraph();
                keyPhraseGraph.printGraph(false);
            }
            
            previousStandardError = getStandardError(statistics, convergenceScheme);
            statistics.clear();

            // Step 1: Calculate d_j for every node
            calculateDifferentials(rakeWordNodes, statistics, convergenceScheme);

            // Step 1.5: Verify convergence
            double currentStandardError = getStandardError(statistics, convergenceScheme);
            logger.debug("Iteration: " + iteration + "\tStandard error: " +
                    currentStandardError);
            if (verifyConvergence(standardErrorThreshold, currentStandardError, 
                    previousStandardError, convergenceRule, keyPhraseGraph,
                    rakeWordNodes, revertGraphs, true)) {  // Always use differential convergence
                
                // If iteration 0, RankUp scores will be same as Rake scores (instead of -1)
                if (iteration == 0) {
                    for (KeyPhrase keyphrase : keyPhraseGraph.values()) {
                        keyphrase.setFinalTextRankScore(keyphrase.getOriginalTextRankScore());
                    }
                }
                
                break;
            }

            // Step 2: Modify edge weights
            modifyEdgeWeights(rakeWordNodes, rakeGraph, learningRate, correctNegativeWeights);

            // Step 3: Rerun Rake
            rake.rerunRake();

            // Step 4: Reassign scores
            for (KeyPhrase keyphrase: keyphraseList) {
                String key = keyphrase.getNode().key;
                RakeNode keywordNode = rakeGraph.get(key);

                keyphrase.setFinalTextRankScore(keywordNode.rank);
                keyphrase.setScore(keywordNode.rank);
            }
        }

        return iteration;
    }

}
