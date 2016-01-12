package com.figueroa.nlp;

import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.figueroa.util.MiscUtils;

/**
 * Implements a node in the RankUp graph, denoting some word or keyword
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2015
 */
public abstract class Node implements Comparable<Node> {
    
    private final static Log log_ = LogFactory.getLog(Node.class.getName());
    public final static Double DEFAULT_EDGE_WEIGHT = 1.0;
    
    /**
     * Public members.
     */
    //public HashSet<Node> edges = new HashSet<Node>();
    public String key = null;
    public boolean marked = false;
    protected double expectedScore;
    
    /**
     * Protected members.
     */
    protected final String text;
    protected double rank = 0.0D;
    protected double previousRank;
    protected double originalRank;
    protected double d_j; // Differential value used in RankUp (equations 3.25 and 3.27)
    protected double previous_d_j;
    protected HashMap<Node, Double> edges = new HashMap<>();
    protected final HashMap<Node, Double> previousEdges = new HashMap<>();

    /**
     * Private constructor.
     * @param key
     * @param text
     */
    protected Node(final String key, String text) {
        this.text = text;
        this.rank = 1.0D;
        this.key = key;
        this.d_j = 0;
        this.expectedScore = -1.0;
        this.previousRank = 1.0D;
        this.originalRank = 1.0D;
        this.previous_d_j = 0;
    }

    /**
     * Compare method for sort ordering.
     * @param that
     */
    @Override
    public int compareTo(final Node that) {
        if (this.rank > that.rank) {
            return -1;
        }
        else if (this.rank < that.rank) {
            return 1;
        }
        else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node) {
            Node node = (Node) o;
            return node.key.equals(this.key);
        }
        else {
            return false;
        }
    }

    public HashMap<Node, Double> getEdges() {
        return edges;
    }
    
    public HashMap<Node, Double> getPreviousEdges() {
        return previousEdges;
    }
    
    /**
     * Sets an edge's weight.
     * @param edgeNode
     * @param weight 
     */
    public void setEdgeWeight(Node edgeNode, double weight) {
        // First, assign previous edge
        double previousWeight = edges.get(edgeNode);
        previousEdges.put(edgeNode, previousWeight);
        
        edges.put(edgeNode, weight);
    }
    
    /**
     * Connect two nodes with a bi-directional arc in the graph.
     * @param that
     * @param weight
     */
    public void connect(final Node that, Double weight) {
        this.edges.put(that, weight);
        that.edges.put(this, weight);
    }

    /**
     * Disconnect two nodes removing their bi-directional arc in the
     * graph.
     * @param that
     */
    public void disconnect(final Node that) {
        this.edges.remove(that);
        that.edges.remove(this);
    }

    /**
     * Create a unique identifier for this node, returned as a hex
     * string.
     * @return 
     */
    public String getId() {
        return Integer.toString(hashCode(), 16);
    }
    
    /**
     * Rever the node values to their previous states
     */
    public void revertNode() {
        rank = previousRank >= 0 ? previousRank : originalRank;
//        d_j = previous_d_j;
        edges = previousEdges;
    }
    
    public String getText() {
        return text;
    }
    
    public double getRank() {
        return rank;
    }
    
    public void setRank(double rank) {
        //this.previousRank = this.rank;
        this.rank = rank;
    }
    
    public void setPreviousRank(double rank) {
        this.previousRank = rank;
    }
    
    public void setOriginalRank(double originalRank) {
        this.originalRank = originalRank;
    }
    
    public double get_d_j() {
        return d_j;
    }

    public void set_d_j(double d_j) {
        this.previous_d_j = this.d_j;
        this.d_j = d_j;
    }

    public double getExpectedScore() {
        return expectedScore;
    }

    public void setExpectedScore(double expectedScore) {
        this.expectedScore = expectedScore;
    }
    
    public double getPreviousRank() {
        return previousRank;
    }
    
    public double getOriginalRank() {
        return originalRank;
    }
    
    public double getPreviousD_j() {
        return previous_d_j;
    }
    
    /**
     * Reset node scores for reuse
     */
    public void resetNodeScoresAndWeights() {
                
        // Reset node
        this.rank = originalRank;
        this.d_j = 0;
        this.expectedScore = -1.0;
        this.previousRank = 1.0D;
        this.previous_d_j = 0;
        
        // Reset edge weights
        for (Node edgeNode : this.edges.keySet()) {
            this.setEdgeWeight(edgeNode, DEFAULT_EDGE_WEIGHT);
        }
    }

    @Override
    public String toString() {
        String output = "";

        String adjustedText = this.text;
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
