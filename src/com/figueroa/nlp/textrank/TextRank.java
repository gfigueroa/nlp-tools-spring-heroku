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

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.figueroa.nlp.Node;
import com.figueroa.nlp.Stopwords;

import net.didion.jwnl.data.POS;

/**
 * Java implementation of the TextRank algorithm by Rada Mihalcea, et al.
 *    http://lit.csci.unt.edu/index.php/TextRankGraph-based_NLP
 *
 * @author paco@sharethis.com
 */
public class TextRank {
	
	private static final Logger logger = Logger.getLogger(TextRank.class);

    /**
     * Public definitions.
     */
    public final static String NLP_RESOURCES = "nlp.resources";
    public final static double MIN_NORMALIZED_RANK = 0.05D;
    //public final static double MIN_NORMALIZED_RANK = 0.00D; // Return all possible phrases
    public final static int MAX_NGRAM_LENGTH = 5;
    public final static long MAX_WORDNET_TEXT = 2000L;
    public final static long MAX_WORDNET_GRAPH = 600L;
    public final boolean removeStopwords = true;
    public Collection<MetricVector> answer = null;
    /**
     * Protected members.
     */
    protected LanguageModel lang = null;
    protected static String text;
    protected ArrayList<Sentence> s_list = null;
    protected TextRankGraph graph = null;
    protected TextRankGraph ngram_subgraph = null;
    protected TextRankGraph synset_subgraph = null;
    protected Map<NGram, MetricVector> metric_space = null;
    protected long start_time = 0L;
    protected long elapsed_time = 0L;
    private final Stopwords stopwords;

    // Public constructor (for use in other classes)
    public TextRank(Stopwords stopwords, LanguageModel lang) 
            throws Exception {

        // filter out overly large files

        this.lang = lang;

        this.stopwords = stopwords;
    }
    
    // Public constructor (for use in other classes)
//    public TextRank(ExceptionLogger logger, Stopwords stopwords) throws Exception {
//
//        this.logger = logger;
//
//        final String log4j_conf = "./res/log4j.properties";
//        final String res_path = "./res";
//        final String lang_code = "en";
//
//        // filter out overly large files
//
//        lang = LanguageModel.buildLanguage(res_path, lang_code);
//        WordNet.buildDictionary(res_path, lang_code);
//
//        this.stopwords = stopwords;
//    }

    /**
     * Prepare to call algorithm with a new text to analyze.
     */
    private Collection<Sentence> prepCall(final String _text) throws Exception {
        final String[] _para = new String[1];

        _para[0] = _text;

        return prepCall(_para);
    }

    /**
     * Prepare to call algorithm with a new text to analyze.
     */
    private Collection<Sentence> prepCall(final String[] _para) throws Exception {
        final StringBuilder sb = new StringBuilder();

        graph = new TextRankGraph();
        ngram_subgraph = null;
        synset_subgraph = new TextRankGraph();
        //metric_space = new HashMap<NGram, MetricVector>();

        this.s_list = new ArrayList<Sentence>();

        //////////////////////////////////////////////////
        // PASS 1: construct a graph from PoS tags

        initTime();

        // scan sentences to construct a graph of relevent morphemes

        s_list = new ArrayList<Sentence>();

        for (String para_text : _para) {
            if (para_text.trim().length() > 0) {
                for (String sent_text : lang.splitParagraph(para_text)) {
                    final Sentence s = new Sentence(sent_text.trim());
                    s.tokenize(lang);
                    if (removeStopwords) {
                        s.mapTokens(lang, graph, stopwords);
                    }
                    else {
                        s.mapTokens(lang, graph);
                    }
                    s_list.add(s);
//
//                    logger.debug("s: " + s.text, DebugLevel.DETAIL);
//                    logger.debug(s.md5_hash, DebugLevel.DETAIL);
                }
            }
        }

        TextRank.text = sb.toString();

        markTime("construct_graph");

        return s_list;
    }

    /**
     * Run the TextRank algorithm on the given semi-structured text
     * (e.g., results of parsed HTML from crawled web content) to
     * build a graph of weighted key phrases.
     */
    private Collection<MetricVector> call() throws Exception {
        //////////////////////////////////////////////////
        // PASS 2: run TextRank to determine keywords

        initTime();

        final int max_results = (int) Math.round(graph.size() * TextRankGraph.KEYWORD_REDUCTION_FACTOR); // ORIGINAL

        graph.runTextRank();
        graph.sortResults(max_results);

        ngram_subgraph = NGram.collectNGrams(lang, s_list, graph.getRankThreshold()); // ORIGINAL
        //ngram_subgraph = NGram.collectNGrams(lang, s_list, 0.0D); // Return all possible phrases

        markTime("basic_textrank");

        logger.trace("TEXT_BYTES:\t" + text.length());
        logger.trace("GRAPH_SIZE:\t" + graph.size());

//        logger.info("INITIAL GRAPH");
//        // Print TextRankGraph
//        for (TextRankNode node : graph.node_list) {
//            logger.info("Text: " + node.value.text + ", Score: " + node.rank);
//        }

        //////////////////////////////////////////////////
        // PASS 3: lemmatize selected keywords and phrases

        initTime();

        // filter for edge cases

        if ((text.length() < MAX_WORDNET_TEXT) && (graph.size() < MAX_WORDNET_GRAPH)) {
            // test the lexical value of nouns and adjectives in WordNet

            for (TextRankNode n : graph.values()) {
                final KeyWord kw = (KeyWord) n.value;

                if (lang.isNoun(kw.pos)) {
                    SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.NOUN);
                }
                else if (lang.isAdjective(kw.pos)) {
                    SynsetLink.addKeyWord(synset_subgraph, n, kw.text, POS.ADJECTIVE);
                }
            }

            // test the collocations in WordNet

            for (TextRankNode n : ngram_subgraph.values()) {
                final NGram gram = (NGram) n.value;
                if (gram.nodes.size() > 1) {
                    SynsetLink.addKeyWord(synset_subgraph, n, gram.getCollocation(), POS.NOUN);
                }
            }

            synset_subgraph = SynsetLink.pruneGraph(synset_subgraph, graph);
        }

//        logger.info("SYNSET SUBGRAPH");
//        // Print TextRankGraph
//        for (TextRankNode node : synset_subgraph.values()) {
//            logger.info("Text: " + node.value.text + ", Score: " + node.rank);
//        }

        // augment the graph with n-grams added as nodes

        for (TextRankNode n : ngram_subgraph.values()) {
            final NGram gram = (NGram) n.value;

            if (gram.length < MAX_NGRAM_LENGTH) {
                graph.put(n.key, n);

                for (TextRankNode keyword_node : gram.nodes) {
                    n.connect(keyword_node, Node.DEFAULT_EDGE_WEIGHT);
                }
            }
        }

        markTime("augment_graph");

//        logger.info("AUGMENTED GRAPH");
//        // Print TextRankGraph
//        for (TextRankNode node : graph.node_list) {
//            logger.info("Text: " + node.value.text + ", Score: " + node.rank);
//        }

        //////////////////////////////////////////////////
        // PASS 4: re-run TextRank on the augmented graph

        initTime();

        logger.trace("RERUN TEXTRANK");
        graph.runTextRank();
        //graph.sortResults(graph.size() / 2);

        markTime("ngram_textrank");

        //////////////////////////////////////////////////
        // PASS 5: construct a metric space for overall ranking

        initTime();
        metric_space = calculateMetrics();
        markTime("normalize_ranks");

        // return results
        return metric_space.values();
    }

    public HashMap<NGram, MetricVector> calculateMetrics() {
        
        HashMap metricSpace = new HashMap<NGram, MetricVector>();
        
        // Collect stats for metrics
        final int ngram_max_count = NGram.calcStats(ngram_subgraph);
        SynsetLink.calcStats(synset_subgraph);

        // Construct a metric space for overall ranking
        final double link_min = ngram_subgraph.dist_stats.getMin();
        final double link_coeff = ngram_subgraph.dist_stats.getMax() -
                ngram_subgraph.dist_stats.getMin();

        final double count_min = 1;
        final double count_coeff = (double) ngram_max_count - 1;

        final double synset_min = synset_subgraph.dist_stats.getMin();
        final double synset_coeff = synset_subgraph.dist_stats.getMax() -
                synset_subgraph.dist_stats.getMin();

        for (TextRankNode n : ngram_subgraph.values()) {
            final NGram gram = (NGram) n.value;
            
            if (gram.length < TextRank.MAX_NGRAM_LENGTH) {
                final double link_rank = (n.getRank() - link_min) / link_coeff;
                final double count_rank = (gram.getCount() - count_min) / count_coeff;
                final double synset_rank = n.maxNeighbor(synset_min, synset_coeff);

                final MetricVector mv =
                        new MetricVector(gram, link_rank, count_rank, synset_rank);
                metricSpace.put(gram, mv);
            }
        }

        return metricSpace;
    }

    //////////////////////////////////////////////////////////////////////
    // access and utility methods
    //////////////////////////////////////////////////////////////////////
    /**
     * Re-initialize the timer.
     */
    public void initTime() {
        start_time = System.currentTimeMillis();
    }

    /**
     * Report the elapsed time with a label.
     * @param label
     */
    public void markTime(final String label) {
        elapsed_time = System.currentTimeMillis() - start_time;

        logger.trace("ELAPSED_TIME:\t" + elapsed_time + "\t" + label);
    }

    /**
     * Accessor for the graph.
     * @return 
     */
    public TextRankGraph getGraph() {
        return graph;
    }

    /**
     * Accessor for ngram subgraph
     * @return 
     */
    public TextRankGraph getNGramSubgraph() {
        return ngram_subgraph;
    }

    /**
     * Accessor for synset subgraph
     * @return 
     */
    public TextRankGraph getSynsetSubgraph() {
        return synset_subgraph;
    }

    /**
     * Accessor for the language.
     * @return 
     */
    public LanguageModel getLanguageModel() {
        return lang;
    }

    /**
     * Serialize the graph to a file which can be rendered.
     * @param graph_file
     * @throws java.lang.Exception
     */
    public void serializeGraph(final String graph_file) throws Exception {
        for (TextRankNode n : graph.values()) {
            n.marked = false;
        }

        final TreeSet<String> entries = new TreeSet<String>();

        for (TextRankNode n : ngram_subgraph.values()) {
            final NGram gram = (NGram) n.value;
            final MetricVector mv = metric_space.get(gram);

            if (mv != null) {
                final StringBuilder sb = new StringBuilder();

                sb.append("rank").append('\t');
                sb.append(n.getId()).append('\t');
                sb.append(mv.render());
                entries.add(sb.toString());

                n.serializeGraph(entries);
            }
        }

        final OutputStreamWriter fw =
                new OutputStreamWriter(new FileOutputStream(graph_file), "UTF-8");

        try {
            for (String entry : entries) {
                fw.write(entry, 0, entry.length());
                fw.write('\n');
            }
        }
        finally {
            fw.close();
        }
    }

    /**
     * Serialize resulting graph to a string.
     */
    @Override
    public String toString() {
        final TreeSet<MetricVector> key_phrase_list = new TreeSet<MetricVector>(metric_space.values());
        final StringBuilder sb = new StringBuilder();

        for (MetricVector mv : key_phrase_list) {
            if (mv.metric >= MIN_NORMALIZED_RANK) {
                sb.append(mv.render()).append("\t").append(mv.value.text).append("\n");
            }
        }

        return sb.toString();
    }

    //////////////////////////////////////////////////////////////////////
    // command line interface
    //////////////////////////////////////////////////////////////////////
    /**
     * Main entry point.
     * @param t
     * @return 
     * @throws java.lang.Exception
     */
    public Collection<MetricVector> run(String t) throws Exception {

        text = t;

        // executes algorithm
        final Collection<Sentence> s_list = prepCall(text.split("\n"));
        answer = call();//null;

        //Print keyword list
        //logger.info("\n" + this.toString());

        return answer;
    }
}
