package com.figueroa.nlp.rake;

import java.util.HashMap;
import com.figueroa.nlp.Node;
import com.figueroa.util.MiscUtils;

/**
 * Implements a node in the RAKE graph, denoting a word
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2015
 */
public class RakeNode extends Node {
    
    /**
     * Public members.
     */
    public static enum RakeNodeType {
        KEYWORD, WORD
    }
    public final RakeNodeType type;
    
    /**
     * Private members.
     */
    private final HashMap<RakeNode, Double> originalEdges = new HashMap<>();

    /**
     * Private constructor.
     */
    private RakeNode(final String key, final String text, final RakeNodeType type) { 
        super(key, text);
        this.type = type;
    }

    /**
     * Factory method.
     * @param graph
     * @param text
     * @param type
     * @return 
     * @throws java.lang.Exception 
     */
    public static RakeNode buildNode(
            final HashMap<String, RakeNode> graph, 
            final String text,
            final RakeNodeType type) 
            throws Exception {
        
        String key = type.toString() + "_" + text;
        RakeNode n = graph.get(key);

        if (n == null) {
            n = new RakeNode(key, text, type);
            graph.put(key, n);
        }

        return n;
    }
    
    public static String buildRakeNodeKey(String text, RakeNodeType type) {
        String key = type.toString() + "_" + text;
        return key;
    }
    
    /**
     * Connect two nodes with a bi-directional arc in the graph.
     * @param that
     * @param weight
     */
    @Override
    public void connect(final Node that, Double weight) {
        super.connect(that, weight);
        RakeNode thisRakeNode = this;
        RakeNode thatRakeNode = (RakeNode) that;
        this.originalEdges.put(thatRakeNode, weight);
        thatRakeNode.originalEdges.put(thisRakeNode, weight);
    }
    
    public HashMap<RakeNode, Double> getOriginalEdges() {
        return originalEdges;
    }
    
    /**
     * Reset node scores for reuse
     */
    @Override
    public void resetNodeScoresAndWeights() {
                
        // Reset node
        this.rank = originalRank;
        this.d_j = 0;
        this.expectedScore = -1.0;
        this.previousRank = 1.0D;
        this.previous_d_j = 0;
        
        // Reset edge weights
        for (Node edgeNode : this.edges.keySet()) {
            RakeNode rakeEdgeNode = (RakeNode) edgeNode;
            double originalWeight = this.originalEdges.get(rakeEdgeNode);
            this.setEdgeWeight(edgeNode, originalWeight);
        }
    }
    
    @Override
    public String toString() {
        String output = "";

        String adjustedText = this.text;
        adjustedText += " (" + this.type + ")";
        while (adjustedText.length() < 45) {
            adjustedText = adjustedText.concat(" ");
        }
        
        String previousRankString = 
                MiscUtils.convertDoubleToFixedCharacterString(previousRank, 2);
        String rankString = MiscUtils.convertDoubleToFixedCharacterString(rank, 2);
        
        String previous_d_jString =
                MiscUtils.convertDoubleToFixedCharacterString(previous_d_j, 2);
        String d_jString =
                MiscUtils.convertDoubleToFixedCharacterString(d_j, 2);
        
        output += adjustedText;
        output += "r: " + rankString;
        output += " (P: " + previousRankString + ")";
        output += ", d: " + d_jString;
        output += " (P: " + previous_d_jString + ")";
            
        return output;
    }

}
