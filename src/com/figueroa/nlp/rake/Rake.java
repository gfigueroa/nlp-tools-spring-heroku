package com.figueroa.nlp.rake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.util.PythonInterpreter;

import com.figueroa.nlp.Node;
import com.figueroa.nlp.rake.RakeNode.RakeNodeType;
import com.figueroa.util.Abstract;

/**
 * A wrapper class for the RAKE implementation in Python
 * 
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2015
 */
public class Rake {
    
	private static final Logger logger = Logger.getLogger(Rake.class);
	
    public final static String JYTHON_ROOT_PATH = "/home/gfigueroa/jython2.5.3/Lib";
    public final static String RAKE_DIR = "./python/rake";
    public final static String RAKE_STOPWORDS_PATH = "./python/rake/SmartStoplist.txt";
    
    public final PythonInterpreter pythonInterpreter;
    public HashMap<String, RakeNode> rakeFullGraph = null;
    public HashMap<String, Double> currentKeyphrases = null;
    public HashMap<String, Double> currentWords = null;
    
    public Rake() {
        pythonInterpreter = initPython();
        initRake();
    }
    
    /**
     * Loads main components required by the Jython interpreter (for RAKE)
     * @return an instance of the PythonInterpreter already initialized
     */
    private PythonInterpreter initPython() {
        logger.debug("Initializing Jython...");
        PythonInterpreter interp = new PythonInterpreter(null, new PySystemState());
        PySystemState sys = Py.getSystemState();
        sys.path.append(new PyString(JYTHON_ROOT_PATH));
        sys.path.append(new PyString(RAKE_DIR));
        interp.exec("import sys");
        interp.exec("import rake");
        
        return interp;
    }
    
    private void initRake() {
        pythonInterpreter.exec("extractor = rake.Rake('" + RAKE_STOPWORDS_PATH + "')");
    }
    
    /**
     * Set this class's and Jython's instance back to the original state
     * @param abs: the abstract containing the original state's variables
     */
    public void setRakeToOriginalState(Abstract abs) {
        // First, set this class's fields
        rakeFullGraph = abs.getRakeFullGraph();
        currentKeyphrases = abs.getCurrentRakeKeyphrases();
        currentWords = abs.getCurrentRakeWords();
        
        // Then, set phrase list in Jython
        pythonInterpreter.exec("phrase_list = []");
        for (String keyphrase : currentKeyphrases.keySet()){
            pythonInterpreter.exec("phrase_list.append(\"" + keyphrase + "\")");
        }
        pythonInterpreter.exec("extractor.phrase_list = phrase_list");
        
        // Then, set co-occurrence graph in Jython
        pythonInterpreter.exec("co_occ_graph = {}");
        for (RakeNode node : rakeFullGraph.values()) {
            // Only do it for Words
            if (node.type != RakeNodeType.WORD) {
                continue;
            }
            String nodeText = node.getText();
            pythonInterpreter.exec("edgeNodeDictionary = {}");
            HashMap<RakeNode, Double> originalEdges = node.getOriginalEdges();
            for (RakeNode edgeNode : originalEdges.keySet()) {
                // Only do it for Words
                if (edgeNode.type != RakeNodeType.WORD) {
                    continue;
                }
                String edgeNodeText = edgeNode.getText();
                double weight = originalEdges.get(edgeNode);
                pythonInterpreter.exec("edgeNodeDictionary[\"" + edgeNodeText + "\"]" +
                        " = " + weight);
            }
            pythonInterpreter.exec("co_occ_graph[\"" + nodeText + "\"] = edgeNodeDictionary");
        }
        pythonInterpreter.exec("extractor.co_occ_graph = co_occ_graph");
    }    
    /**
     * Converts the keywords object returned by RAKE (a PyList of PyTuples) into a
     * HashMap<String, Double>.
     * @param keywordObjectList: The PyList of PyTuples returned by RAKE
     * @return 
     */
    private static HashMap<String, Double> getKeywordHashMapFromPyList(
            PyList keywordObjectList) {
                
        HashMap<String, Double> keywordMap = new HashMap<>();
        for (Object keywordObject : keywordObjectList) {
            PyTuple keywordTuple = (PyTuple) keywordObject;
            String keyword = (String) keywordTuple.get(0);
            double score;
            try {
                score = (Double) keywordTuple.get(1);
            }
            catch (ClassCastException e) {
                score = (Integer) keywordTuple.get(1);
            }
            keywordMap.put(keyword, score);
        }
        
        return keywordMap;
    }
    
    /**
     * Converts the word_scores object returned by RAKE (a PyDictionary) into a
     * HashMap<String, Double>.
     * @param wordDictionary: The PyDictionary of words returned by RAKE
     * @return 
     */
    private static HashMap<String, Double> getWordHashMapFromPyDictionary(
            PyDictionary wordDictionary) {
                
        HashMap<String, Double> wordMap = new HashMap<>();
        for (Object wordObject : wordDictionary.keySet()) {
            String word = (String) wordObject;
            double score;
            try {
                score = (Double) wordDictionary.get(wordObject);
            }
            catch (ClassCastException e) {
                score = (Integer) wordDictionary.get(wordObject);
            }
            wordMap.put(word, score);
        }
        
        return wordMap;
    }
    
    /**
     * Constructs a new RAKE Graph (for HashMap<String, RakeNode> rakeFullGraph)
 from the Jython Rake instance co-occurrence graph.
     * @throws Exception 
     */
    private void constructRakeGraph() throws Exception {
        
        // Construct rakeFullGraph with Words
        rakeFullGraph = new HashMap<>();
        pythonInterpreter.exec("co_occ_graph = extractor.co_occ_graph");
        PyDictionary pyGraph = (PyDictionary) pythonInterpreter.get("co_occ_graph");
        for (Object key : pyGraph.keySet()) {
            String word = (String) key;
            RakeNode rakeNode = 
                    RakeNode.buildNode(rakeFullGraph, word, RakeNodeType.WORD);
            
            // Construct rakeFullGraph edges
            PyDictionary pyEdges = (PyDictionary) pyGraph.get(key);
            for (Object edgeKey : pyEdges.keySet()) {
                String edgeWord = (String) edgeKey;
                double edgeWeight = 
                        pyEdges.get(edgeKey) instanceof Double ?
                        (Double) pyEdges.get(edgeKey) : (Integer) pyEdges.get(edgeKey);
                RakeNode edgeNode = 
                        RakeNode.buildNode(rakeFullGraph, edgeWord, RakeNodeType.WORD);
                
                rakeNode.connect(edgeNode, edgeWeight);
            }
        }
        
        // Set the Word node score
        for (RakeNode node : rakeFullGraph.values()) {
            double frequency = 0;
            double degree = 0;
            for (Node edgeNode : node.getEdges().keySet()) {
                if (node.equals(edgeNode)) {
                    frequency = node.getEdges().get(edgeNode);
                }
                degree += node.getEdges().get(edgeNode);
            }
            double score = degree / frequency;
            node.setRank(score);
        }
        
        // Construct rakeFullGraph with Keywords
        for (String keyword : currentKeyphrases.keySet()) {
            double score = currentKeyphrases.get(keyword);
            RakeNode rakeNode = 
                    RakeNode.buildNode(rakeFullGraph, keyword, RakeNodeType.KEYWORD);
            rakeNode.setRank(score);
            // Construct keyword edges
            pythonInterpreter.exec("word_list = rake.get_word_list(\"" + keyword + "\")");
            PyList wordList = (PyList) pythonInterpreter.get("word_list");
            for (Object wordObject : wordList) {
                String word = (String) wordObject;
                RakeNode edgeNode = 
                        RakeNode.buildNode(rakeFullGraph, word, RakeNodeType.WORD);
                rakeNode.connect(edgeNode, RakeNode.DEFAULT_EDGE_WEIGHT);
            }
        }
    }
    
    /**
     * Updates the RAKE Graph Keyword and Word node scores from the current Jython Rake 
     * instance keyphrases and word_scores.
     * @throws Exception 
     */
    private void updateRakeGraph() throws Exception {
        
        for (String keyword : currentKeyphrases.keySet()) {
            double rank = currentKeyphrases.get(keyword);
            String key = RakeNode.buildRakeNodeKey(keyword, RakeNodeType.KEYWORD);
            RakeNode node = rakeFullGraph.get(key);
            
            node.setRank(rank);
        }
        
        for (String word: currentWords.keySet()) {
            double rank = currentWords.get(word);
            String key = RakeNode.buildRakeNodeKey(word, RakeNodeType.WORD);
            RakeNode node = rakeFullGraph.get(key);
            
            node.setRank(rank);
        }
    }
    
    /**
     * Updates the Co-Occurrence Graph in Python's RAKE using the current
     * Word nodes
     */
    private void updateRakePythonGraph() {
        for (RakeNode node : getWordNodes()) {
            
            for (Node edgeNode : node.getEdges().keySet()) {
                RakeNode rakeEdgeNode = (RakeNode) edgeNode;
                
                // Only do it for WORD nodes
                if (rakeEdgeNode.type != RakeNodeType.WORD) {
                    continue;
                }
                
                double weight = node.getEdges().get(edgeNode);
                
                // def modify_edge_weight(self, node1, node2):
                pythonInterpreter.exec("extractor.modify_edge_weight(" + 
                        "\"" + node.getText() + "\", " + 
                        "\"" + edgeNode.getText() + "\", " + 
                        weight + ")");
            }
        }
    }
    
    /**
     * Returns the current list of RakeNodes marked as KEYWORD
     * @return a List of RakeNode of type KEYWORD
     */
    public List<RakeNode> getKeywordNodes() {
        ArrayList<RakeNode> keywordNodes = new ArrayList<>();
        for (RakeNode rakeNode : rakeFullGraph.values()) {
            if (rakeNode.type == RakeNodeType.KEYWORD) {
                keywordNodes.add(rakeNode);
            }
        }
        return keywordNodes;
    }
    
    /**
     * Returns the current list of RakeNodes marked as WORD
     * @return a List of RakeNode of type WORD
     */
    public List<RakeNode> getWordNodes() {
        ArrayList<RakeNode> wordNodes = new ArrayList<>();
        for (RakeNode rakeNode : rakeFullGraph.values()) {
            if (rakeNode.type == RakeNodeType.WORD) {
                wordNodes.add(rakeNode);
            }
        }
        return wordNodes;
    }
    
    /**
     * Runs RAKE (initial) and returns keywords from RAKE Python program
     * @param text: the text to process
     * @return a HashMap of <String, Double> with the keywords and their scores
     * @throws java.lang.Exception
     */
    public HashMap<String, Double> runRake(String text) throws Exception {
        logger.debug("Extracting RAKE keywords...");
        // Clean new line
        text = text.replaceAll("\n", "\\n");
        
        try {
            pythonInterpreter.exec("keywords = extractor.run(\"" + text + "\")");
            pythonInterpreter.exec("word_scores = extractor.word_scores");
            }
        catch (Exception e) {
            logger.trace("Error in runRake(): " + e.getMessage());
            throw e;
        }
        
        // Get keywords
        PyList keywordObjectList = (PyList) pythonInterpreter.get("keywords");
        currentKeyphrases = getKeywordHashMapFromPyList(keywordObjectList);
        
        // Get words
        PyDictionary wordDictionary = 
                (PyDictionary) pythonInterpreter.get("word_scores");
        currentWords = getWordHashMapFromPyDictionary(wordDictionary);
        
        // Construct the graph
        constructRakeGraph();
        
        return currentKeyphrases;
    }
    
    /**
     * Reruns RAKE with the given co-occurrence rakeFullGraph and returns the keywords
     * @return a HashMap of <Keyword, Score>
     * @throws java.lang.Exception
     */
    public HashMap<String, Double> rerunRake() throws Exception {
        logger.debug("Rerunning RAKE...");
        
        // First, update the co-occurrence graph in Python
        updateRakePythonGraph();
        
        try {
            pythonInterpreter.exec("keywords = extractor.rerun()");
            pythonInterpreter.exec("word_scores = extractor.word_scores");
        }
        catch (Exception e) {
            logger.trace("Error in rerunRake(): " + e.getMessage());
            throw e;
        }
        
        // Get keywords
        PyList keywordObjectList = (PyList) pythonInterpreter.get("keywords");
        currentKeyphrases = getKeywordHashMapFromPyList(keywordObjectList);
        
        // Get words
        PyDictionary wordDictionary = 
                (PyDictionary) pythonInterpreter.get("word_scores");
        currentWords = getWordHashMapFromPyDictionary(wordDictionary);
        
        // Update RakeNode scores in the graph
        updateRakeGraph();
        
        return currentKeyphrases;
    }
    
    /**
     * Print graph in text mode
     * @param logger 
     */
    public void printRakeGraph() {
        logger.trace("");
        logger.trace("*** TextRank Graph ***");
        for (Node node : this.rakeFullGraph.values()) {
            
//            // For debugging only
//            if (node.getExpectedScore() < 0 && node.get_d_j() == 0) {
//                continue;
//            }
            
            String nodeString = node.toString();
            
            HashMap<Node, Double> previousEdges = node.getPreviousEdges();
            HashMap<Node, Double> edges = node.getEdges();

            logger.trace(nodeString);
            logger.trace("\tEdges: ");
            for (Node edgeNode : edges.keySet()) {
                
                String edgeText = edgeNode.toString();
                double previousEdgeWeight = previousEdges.get(edgeNode) != null ?
                        previousEdges.get(edgeNode) : -1.0;
                double edgeWeight = edges.get(edgeNode);
                
                String edgeInformation = "\t\t" + edgeText +
                        " - w: " + MathUtils.round(edgeWeight, 2);
                edgeInformation += " (P: " + MathUtils.round(previousEdgeWeight, 2) +
                        ")";
                logger.trace(edgeInformation);
            }
        }
        logger.trace("**********************");
    }
    
}
