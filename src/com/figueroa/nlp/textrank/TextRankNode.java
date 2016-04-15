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

import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.util.MathUtils;
import com.figueroa.nlp.Node;
import com.figueroa.util.MiscUtils;

/**
 * Implements a node in the TextRank graph, denoting some noun or
 * adjective morpheme.
 *
 * @author paco@sharethis.com
 */
public class TextRankNode extends Node {
    
    private final static Log log_ = LogFactory.getLog(TextRankNode.class.getName());
    
    /**
     * Public members.
     */
    public NodeValue value = null;

    /**
     * Private constructor.
     */
    private TextRankNode(final String key, final NodeValue value) {
        super(key, value.text);
        this.value = value;
    }

    /**
     * Factory method.
     * @param graph
     * @param key
     * @param value
     * @return 
     * @throws java.lang.Exception 
     */
    public static TextRankNode buildNode(
            final TextRankGraph graph, 
            final String key, 
            final NodeValue value) 
            throws Exception {
        TextRankNode n = graph.get(key);

        if (n == null) {
            n = new TextRankNode(key, value);
            graph.put(key, n);
        }

        if (log_.isDebugEnabled()) {
            log_.debug(n.key);
        }

        return n;
    }

    /**
     * Search nearest neighbors in WordNet subgraph to find the
     * maximum rank of any adjacent SYNONYM synset.
     * @param min
     * @param coeff
     * @param logger
     * @return 
     */
    public double maxNeighbor(final double min, final double coeff) {
        
        double adjusted_rank = 0.0D;

        if (log_.isDebugEnabled()) {
            log_.debug("neighbor: " + value.text + " " + value);
            log_.debug("  edges:");

            for (Node n : edges.keySet()) {
                TextRankNode node = (TextRankNode) n;
                log_.debug(node.value);
            }
        }

        if (edges.size() > 1) {
            // consider the immediately adjacent synsets

            double max_rank = 0.0D;

            for (Node n : edges.keySet()) {
                TextRankNode node = (TextRankNode) n;
                if (node.value instanceof SynsetLink) {
                    max_rank = Math.max(max_rank, node.rank);
                }
            }

            if (max_rank > 0.0D) {
                // adjust it for scale [0.0, 1.0]
                adjusted_rank = (max_rank - min) / coeff;
            }
        }
        else {
            // consider the synsets of the one component keyword
            for (Node n : edges.keySet()) {
                TextRankNode node = (TextRankNode) n;
                if (node.value instanceof KeyWord) {
                    // there will only be one
                    adjusted_rank = node.maxNeighbor(min, coeff);
                }
            }
        }

        if (log_.isDebugEnabled()) {
            log_.debug(adjusted_rank);
        }

        return adjusted_rank;
    }

    /**
     * Traverse the graph, serializing out the nodes and edges.
     * @param entries
     */
    public void serializeGraph(final Set<String> entries) {
        StringBuilder sb = new StringBuilder();

        // emit text and ranks vector

        marked = true;

        sb.append("node").append('\t');
        sb.append(getId()).append('\t');
        sb.append(value.getDescription()).append('\t');
        sb.append(MathUtils.round(rank, 3));
        entries.add(sb.toString());

        // emit edges

        for (Node n : edges.keySet()) {
            TextRankNode node = (TextRankNode) n;
            sb = new StringBuilder();
            sb.append("edge").append('\t');
            sb.append(getId()).append('\t');
            sb.append(n.getId());
            entries.add(sb.toString());

            if (!n.marked) {
                // tail recursion on child
                node.serializeGraph(entries);
            }
        }
    }

    @Override
    public String toString() {
        String output = "";

        String adjustedText = this.value.text;
        adjustedText += " (" + this.value.getClass().getSimpleName() + ") ";
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
        
        if (this.value instanceof NGram) {
            NGram gram = (NGram) this.value;
            output += " - Ngram_count: " + gram.getCount() + ", ";
            output += " - Ngram_length: " + gram.length;
        }
            
        return output;
    }
}
