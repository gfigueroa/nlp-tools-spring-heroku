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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.figueroa.nlp.Node;
import com.figueroa.nlp.Stopwords;

/**
 * @author paco@sharethis.com
 */
public class Sentence {
    // logging

    private final static Log LOG = LogFactory.getLog(Sentence.class.getName());
    /**
     * Public members.
     */
    public String text = null;
    public String[] token_list = null;
    public TextRankNode[] node_list = null;
    public String md5_hash = null;

    /**
     * Constructor.
     */
    public Sentence(final String text) {
        this.text = text;
    }

    /**
     * Return a byte array formatted as hexadecimal text.
     */
    public static String hexFormat(final byte[] b) {
        final StringBuilder sb = new StringBuilder(b.length * 2);

        for (int i = 0; i < b.length; i++) {
            String h = Integer.toHexString(b[i]);

            if (h.length() == 1) {
                sb.append("0");
            }
            else if (h.length() == 8) {
                h = h.substring(6);
            }

            sb.append(h);
        }

        return sb.toString().toUpperCase();
    }

    /**
     * Tokenize the sentence.
     */
    public String[] tokenize(final LanguageModel lang) {
        token_list = lang.tokenizeSentence(text);

        return token_list;
    }

    /**
     * Accessor for token list.
     */
    public String[] getTokenList() {
        return token_list;
    }

    // New method for cleaning words before adding to graph
    private boolean wordIsSignificant(Stopwords stopwords, String word) {
        if (stopwords.isStopWord(word)) {
            return false;
        }
        if (word.length() < 2) {
            return false;
        }
        String reducedWord = word.replaceAll("[a-zA-Z]", ""); // Remove all alphabetic characters
        if (reducedWord.length() == word.length()) {
            return false;   // If the two strings have the same length, it means the text didn't contain alphabetic characters
        }
        return true;
    }

    /**
     * Main processing per sentence. Modified for HybridRank
     */
    public void mapTokens(final LanguageModel lang, final TextRankGraph graph, final Stopwords stopwords) throws Exception {
        // scan each token to determine part-of-speech

        final String[] tag_list = lang.tagTokens(token_list);

        // create nodes for the graph

        TextRankNode last_node = null;
        node_list = new TextRankNode[token_list.length];

        for (int i = 0; i < token_list.length; i++) {
            final String pos = tag_list[i];

            if (LOG.isDebugEnabled()) {
                LOG.debug("token: " + token_list[i] + " pos tag: " + pos);
            }

            //if (lang.isRelevant(pos)) {
            if (lang.isRelevant(pos) && wordIsSignificant(stopwords, token_list[i])) { // New implementation for cleaning some words

                if (token_list[i].endsWith("-")) {
                    token_list[i] = token_list[i].substring(0, token_list[i].length() - 1);
                }

                final String key = lang.getNodeKey(token_list[i], pos);
                final KeyWord value = new KeyWord(token_list[i], pos);
                final TextRankNode n = TextRankNode.buildNode(graph, key, value);
                value.setParentNode(n); // SET PARENT NODE

                // emit nodes to construct the graph

                if (last_node != null) {
                    n.connect(last_node, Node.DEFAULT_EDGE_WEIGHT);
                }

                last_node = n;
                node_list[i] = n;
            }
        }
    }

    /**
     * Main processing per sentence. Original
     */
    public void mapTokens(final LanguageModel lang, final TextRankGraph graph) throws Exception {
        // scan each token to determine part-of-speech

        final String[] tag_list = lang.tagTokens(token_list);

        // create nodes for the graph

        TextRankNode last_node = null;
        node_list = new TextRankNode[token_list.length];

        for (int i = 0; i < token_list.length; i++) {
            final String pos = tag_list[i];

            if (LOG.isDebugEnabled()) {
                LOG.debug("token: " + token_list[i] + " pos tag: " + pos);
            }

            //if (lang.isRelevant(pos)) {
            if (lang.isRelevant(pos)) {

                if (token_list[i].endsWith("-")) {
                    token_list[i] = token_list[i].substring(0, token_list[i].length() - 1);
                }

                final String key = lang.getNodeKey(token_list[i], pos);
                final KeyWord value = new KeyWord(token_list[i], pos);
                final TextRankNode n = TextRankNode.buildNode(graph, key, value);
                value.setParentNode(n); // SET PARENT NODE

                // emit nodes to construct the graph

                if (last_node != null) {
                    n.connect(last_node, Node.DEFAULT_EDGE_WEIGHT);
                }

                last_node = n;
                node_list[i] = n;
            }
        }
    }
}
