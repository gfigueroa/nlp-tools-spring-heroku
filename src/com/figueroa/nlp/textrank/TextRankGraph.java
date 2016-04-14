/*
Copyright (c) 2009, ShareThis, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided
with the distribution.

 * Neither the name of the ShareThis, Inc., nor the names of its
contributors may be used to endorse or promote products derived
from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.figueroa.nlp.textrank;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.util.MathUtils;
import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.rankup.KeyPhraseGraph;
import com.figueroa.nlp.Node;
import com.figueroa.util.ExceptionLogger;
import com.figueroa.util.ExceptionLogger.DebugLevel;

/**
 * An abstraction for handling the graph as a data object.
 *
 * @author paco@sharethis.com
 */
public class TextRankGraph extends TreeMap<String, TextRankNode> {

    /**
     * Public definitions.
     */
    public final static double INCLUSIVE_COEFF = 0.25D;
    //public final static double KEYWORD_REDUCTION_FACTOR = 0.8D;
    public final static double KEYWORD_REDUCTION_FACTOR = 1.0D; // Return all possible phrases
    public final static double TEXTRANK_DAMPING_FACTOR = 0.85D;
    public final static double STANDARD_ERROR_THRESHOLD = 0.005D;
    public final static String GEPHI_LOGGER_DIR =
            "." + File.separator + "graphs" + File.separator;
    
    /**
     * Public members.
     */
    public SummaryStatistics dist_stats = new SummaryStatistics();
    /**
     * Protected members.
     */
    protected TextRankNode[] node_list = null;

    /**
     * Run through N iterations of the TreeRank algorithm, or until
     * the standard error converges below a given threshold.
     * @param logger
     */
    public void runTextRank(ExceptionLogger logger) {
        final int max_iterations = this.size();
        node_list = new TextRankNode[this.size()];

        // load the node list

        int j = 0;

        for (TextRankNode n1 : this.values()) {
            node_list[j++] = n1;
        }

        // iterate, then sort and mark the top results

        iterateGraph(max_iterations, true, logger);
    }

    // For outside use
    public void runTextRank(boolean debug, ExceptionLogger logger) {
        final int max_iterations = this.size();
        node_list = new TextRankNode[this.size()];

        // load the node list

        int j = 0;

        for (TextRankNode n1 : this.values()) {
            node_list[j++] = n1;
        }

        // iterate, then sort and mark the top results

        iterateGraph(max_iterations, debug, logger);
    }
    
    /**
     * Iterate through the graph, calculating rank.
     * @param max_iterations
     * @param debug
     * @param logger
     */
    protected void iterateGraph(
            final int max_iterations, 
            boolean debug,
            ExceptionLogger logger
            ) {

        final double[] rank_list = new double[node_list.length];
        final double[] previous_rank_list = new double[node_list.length];
        for (int i = 0; i < node_list.length; i++) {
            previous_rank_list[i] = node_list[i].getRank();
        }

        // either run through N iterations, or until the standard
        // error converges below a threshold

        for (int k = 0; k < max_iterations; k++) {
            dist_stats.clear();

            // calculate the next rank for each node

            for (int i = 0; i < node_list.length; i++) {
                final TextRankNode n1 = node_list[i];
                double rank = 0.0D;

                /**
                 * Original unweighted formula
                 */
//		for (TextRankNode n2 : n1.edges.keySet()) {
//		    rank += n2.rank / (double) n2.edges.size();
//		}

                /**
                 * New weighted formula
                 */
                for (Node n2 : n1.getEdges().keySet()) {
                    double denominator = 0.0D;
                    for (Double d : n2.getEdges().values()) {
                        denominator += d;
                    }

                    double normalizedWeight = n2.getEdges().get(n1) / denominator;
                    //LOG.info("NORMALIZED WEIGHT: " + normalizedWeight);

                    rank += normalizedWeight * n2.getRank();
                    //rank += (n2.edges.get(n1) * n2.rank) / denominator;
                }

                rank *= TEXTRANK_DAMPING_FACTOR;
                rank += 1.0D - TEXTRANK_DAMPING_FACTOR;

                rank_list[i] = rank;
                dist_stats.addValue(Math.abs(n1.getRank() - rank));
            }

            final double standard_error =
                    dist_stats.getStandardDeviation() / Math.sqrt((double) dist_stats.getN());

            if (debug) {
                logger.debug("iteration: " + k + " error: " + standard_error,
                        DebugLevel.DETAIL);
            }

            // swap in new rank values
            for (int i = 0; i < node_list.length; i++) {
                node_list[i].setRank(rank_list[i]);
            }

            if (standard_error < STANDARD_ERROR_THRESHOLD) {
                break;
            }
        }
        
        // swap in previous ranks
        for (int i = 0; i < node_list.length; i++) {
            node_list[i].setPreviousRank(previous_rank_list[i]);
        }
    }

    /**
     * Sort results to identify potential keywords.
     * @param max_results
     */
    public void sortResults(final long max_results) {
        Arrays.sort(node_list,
                new Comparator<TextRankNode>() {

                    public int compare(TextRankNode n1, TextRankNode n2) {
                        if (n1.getRank() > n2.getRank()) {
                            return -1;
                        }
                        else if (n1.getRank() < n2.getRank()) {
                            return 1;
                        }
                        else {
                            return 0;
                        }
                    }
                });

        // mark the top-ranked nodes

        dist_stats.clear();

        for (int i = 0; i < node_list.length; i++) {
            final TextRankNode n1 = node_list[i];

            if (i <= max_results) {
                n1.marked = true;
                dist_stats.addValue(n1.getRank());
            }
        }
    }

    /**
     * Calculate a threshold for the ranked results.
     * @return 
     */
    public double getRankThreshold() {
        return dist_stats.getMean()
                + (dist_stats.getStandardDeviation() * INCLUSIVE_COEFF);
    }
    
    /**
     * Print graph in text mode
     * @param keyphraseGraph
     * @param logger 
     */
    public void printGraph(KeyPhraseGraph keyphraseGraph, ExceptionLogger logger) {
        logger.debug("", DebugLevel.DETAIL);
        logger.debug("*** TextRank Graph ***", DebugLevel.DETAIL);
        for (TextRankNode node : this.values()) {
            
            // Skip SynsetLink nodes
            if (node.value instanceof SynsetLink) {
                continue;
            }
            
            KeyPhrase keyphrase = keyphraseGraph.get(node.key);
            String nodeString = node.toString(keyphrase);
            
            HashMap<Node, Double> previousEdges = node.getPreviousEdges();
            HashMap<Node, Double> edges = node.getEdges();

            logger.debug(nodeString, DebugLevel.DETAIL);
            logger.debug("\tEdges: ", DebugLevel.DETAIL);
            for (Node edgeNode : edges.keySet()) {
                KeyPhrase edgeKeyphrase = keyphraseGraph.get(edgeNode.key);
                // Skip SynsetLink nodes
//                if (edgeNode.value instanceof SynsetLink) {
//                    continue;
//                }
                
                String edgeText = edgeNode.toString(edgeKeyphrase);
                double previousEdgeWeight = previousEdges.get(edgeNode) != null ?
                        previousEdges.get(edgeNode) : -1.0;
                double edgeWeight = edges.get(edgeNode);
                
                String edgeInformation = "\t\t" + edgeText +
                        " - w: " + MathUtils.round(edgeWeight, 2);
                edgeInformation += " (P: " + MathUtils.round(previousEdgeWeight, 2) +
                        ")";
                logger.debug(edgeInformation , DebugLevel.DETAIL);
            }
        }
        
        Double keyphraseFinalTextRankScoreCorrectness = 
                keyphraseGraph.getKeyphraseFinalTextRankScoreCorrectness();
        Double textRankNodeScoreCorrectness = keyphraseGraph.getTextRankNodeScoreCorrectness();
        logger.debug("", DebugLevel.DETAIL);
        logger.debug("Keyphrase final TextRank score correctness: " + 
                (keyphraseFinalTextRankScoreCorrectness != null ?
                        MathUtils.round(keyphraseFinalTextRankScoreCorrectness, 2) :
                        ""), DebugLevel.DETAIL);
        logger.debug("TextRank node score correctness: " + 
                (textRankNodeScoreCorrectness != null ?
                        MathUtils.round(textRankNodeScoreCorrectness, 2) :
                        ""), DebugLevel.DETAIL);
        logger.debug("", DebugLevel.DETAIL);
        logger.debug("**********************", DebugLevel.DETAIL);
    }
    
    /**
     * Print graph in text mode
     * @param logger 
     */
    public void printGraph(ExceptionLogger logger) {
        logger.debug("", DebugLevel.DETAIL);
        logger.debug("*** TextRank Graph ***", DebugLevel.DETAIL);
        for (Node node : this.values()) {
            String nodeString = node.toString();
            
            HashMap<Node, Double> previousEdges = node.getPreviousEdges();
            HashMap<Node, Double> edges = node.getEdges();

            logger.debug(nodeString, DebugLevel.DETAIL);
            logger.debug("\tEdges: ", DebugLevel.DETAIL);
            for (Node edgeNode : edges.keySet()) {
                
                String edgeText = edgeNode.toString();
                double previousEdgeWeight = previousEdges.get(edgeNode) != null ?
                        previousEdges.get(edgeNode) : -1.0;
                double edgeWeight = edges.get(edgeNode);
                
                String edgeInformation = "\t\t" + edgeText +
                        " - w: " + MathUtils.round(edgeWeight, 2);
                edgeInformation += " (P: " + MathUtils.round(previousEdgeWeight, 2) +
                        ")";
                logger.debug(edgeInformation , DebugLevel.DETAIL);
            }
        }
        logger.debug("**********************", DebugLevel.DETAIL);
    }
    
    /**
     * Print graph in Gephi-readable format.
     * Gephi format:
     *  ;A;B;C;D;E
     *  A;0;1;0;1;0
     *  B;1;0;0;0;0
     *  C;0;0;1;0;0
     *  D;0;1;0;1;0
     *  E;0;0;0;0;0
     * @param abstractId
     * @param run
     */
    public void printGephiGraph(int abstractId, int run) {
        
        try {
            BufferedWriter writer = 
                    new BufferedWriter(new FileWriter(
                            GEPHI_LOGGER_DIR + 
                            "abs_" + abstractId + "-it_" + run + ".csv", true));
        
            System.out.println("*** TextRank Graph ***");

            // First, print top header (node names)
            for (TextRankNode node: this.values()) {
                // Skip SynsetLink nodes
                if (node.value instanceof SynsetLink) {
                    continue;
                }

                String nodeString = ";\"" + node.value.text;
                nodeString += " [" + MathUtils.round(node.getRank(), 2) + "]";
                System.out.print(nodeString);
                writer.write(nodeString);
            }
            System.out.println(";");
            writer.write(";");
            writer.newLine();

            // Then, print each row
            for (TextRankNode node : this.values()) {
                // Skip SynsetLink nodes
                if (node.value instanceof SynsetLink) {
                    continue;
                }

                // Print row header (node name)
                String nodeString = "\"" + node.value.text;
                nodeString += " [" + MathUtils.round(node.getRank(), 2) + "];";
                System.out.print(nodeString);
                writer.write(nodeString);

                // Print edge weights
                HashMap<Node, Double> nodeEdges = node.getEdges();
                String edgeText = "";
                for (TextRankNode node2 : this.values()) {
                    // Skip SynsetLink nodes
                    if (node2.value instanceof SynsetLink) {
                        continue;
                    }

                    Double edgeWeight = nodeEdges.get(node2);
                    edgeText += 
                            edgeWeight != null ? 
                            String.valueOf(MathUtils.round(edgeWeight, 2)) : "0";
                    edgeText += ";";
                }
                // Remove last semicolon
                edgeText = edgeText.substring(0, edgeText.length() - 1);
                System.out.println(edgeText);
                writer.write(edgeText);
                writer.newLine();
            }
            System.out.println();
            writer.close();
        }
        catch (Exception e) {
            System.err.println("Exception in printGephiGraph: " + e.getMessage());
        }

    }
}
