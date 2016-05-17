package com.figueroa.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.figueroa.nlp.NLPMain;
import com.figueroa.util.MiscUtils;

/**
 * Controller for all web service calls.
 * All web service calls use the mapping "/ws/"
 * @author Figueroa
 *
 */
@RestController
@RequestMapping("/ws/")
public class WebServiceController {
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(WebServiceController.class);
	
	private static class LemmatizedTextJSON {
		public String originalText;
		public String lemmatizedText;
		public LemmatizedTextJSON(String originalText, String lemmatizedText) {
			this.originalText = originalText;
			this.lemmatizedText = lemmatizedText;
		}
	};
	
	private static class TaggedTextJSON {
		public String originalText;
		public String taggedText;
		public TaggedTextJSON(String originalText, String taggedText) {
			this.originalText = originalText;
			this.taggedText = taggedText;
		}
	};
	
	public static class KeyPhraseSimple implements Comparable<KeyPhraseSimple> {
		public String text;
		public double score;
		public KeyPhraseSimple(String text, double score) {
			this.text = text;
			this.score = score;
		}
		@Override
		public int compareTo(final KeyPhraseSimple that) {
	        if (this.score > that.score) {
	            return -1;
	        }
	        else if (this.score < that.score) {
	            return 1;
	        }
	        else {
	            return this.text.compareTo(that.text);
	        }
	    }
	    @Override
	    public String toString() {
	        String output = "";
	
	        String adjustedText = text;
	        while (adjustedText.length() < 40) {
	            adjustedText = adjustedText.concat(" ");
	        }
	
	        String scoreString = 
	        		MiscUtils.convertDoubleToFixedCharacterString(score, 2);
	        
	        output += adjustedText;
	        output += "S: " + scoreString; // Current Score
	
	        return output;
	    }
	}
	
	public class KeywordListJSON {
		public String originalText;
		public ArrayList<KeyPhraseSimple> keywords;
		public KeywordListJSON(String originalText, ArrayList<KeyPhraseSimple> keywords) {
			this.originalText = originalText;
			this.keywords = keywords;
		}
	}
	
	/**
	 * Web service to lemmatize a given text
	 * @param text
	 * @param request the HTTP servlet request
	 * @return a LemmatizedTextJSON instance representing the requested service
	 * {
	 *   "originalText":"worlds",
	 *   "lemmatizedText":"world"
	 * }
	 * @throws Exception
	 */
//    @RequestMapping(value = "/lemmatize", method = RequestMethod.GET, 
//    		headers={"Accept=application/json", "Access-Control-Allow-Origin=*", 
//			"Access-Control-Allow-Headers=Origin,X-Requested-With,Content-Type,Accept"})
	@RequestMapping(value = "/lemmatize", method = RequestMethod.GET, 
    		headers={"Accept=application/json"})
    public LemmatizedTextJSON lemmatizeText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {

        NLPMain nlpMain = NLPMain.getNLPMainInstance();
        String lemmatizedText = nlpMain.lemmatizeText(text);
        
    	LemmatizedTextJSON lt = new LemmatizedTextJSON(text, lemmatizedText);
    	
    	return lt;
    }
    
	/**
	 * Web service to POS-tag a given text
	 * @param text
	 * @param request the HTTP servlet request
	 * @return a TaggedTextJSON instance representing the requested service
	 * {
	 *   "originalText":"worlds",
	 *   "taggedText":"worlds_NNS"
	 * }
	 * @throws Exception
	 */
//    @RequestMapping(value = "/tag", method = RequestMethod.GET, 
//    		headers={"Accept=application/json", "Access-Control-Allow-Origin=*", 
//			"Access-Control-Allow-Headers=Origin,X-Requested-With,Content-Type,Accept"})
    @RequestMapping(value = "/tag", method = RequestMethod.GET, 
			headers={"Accept=application/json"})
    public TaggedTextJSON tagText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {
        
    	NLPMain nlpMain = NLPMain.getNLPMainInstance();
        String taggedText = nlpMain.tagText(text);
        
        TaggedTextJSON tt = new TaggedTextJSON(text, taggedText);
    	
    	return tt;
    }
    
    /**
	 * Web service to extract the keywords of a given text
	 * @param text
	 * @param method the keyword extraction method.
	 * Possible values are: "textrank" (default="textrank")
	 * @param request the HTTP servlet request
	 * @return a KeywordListJSON instance representing the requested service
	 * {
	 *   "originalText":"Daallo Airlines Flight 159 makes a successful emergency 
	 *   landing after an explosion aboard kills one person and injures three.",
	 *   "keywords":
	 *   [
	 *   {
	 *     "text":"airlines flight",
	 *     "score":0.5773502691896257
	 *   },
	 *   {
	 *     "text":"successful emergency landing",
	 *     "score":0.39145289597815713
	 *   },
	 *   {
	 *     "text":"person",
	 *     "score":0.05258928570117398
	 *   },
	 *   {
	 *     "text":"explosion",
	 *     "score":0.0
	 *   }
	 *   ]
	 * }
	 * @throws Exception
	 */
//    @RequestMapping(value = "/keywords", method = RequestMethod.GET, 
//    		headers={"Accept=application/json", "Access-Control-Allow-Origin=*", 
//    				"Access-Control-Allow-Headers=Origin,X-Requested-With,Content-Type,Accept"})
    @RequestMapping(value = "/keywords", method = RequestMethod.GET, 
    		headers={"Accept=application/json"})
    public KeywordListJSON extractKeywords(
    		@RequestParam(value="text", defaultValue="") String text,
    		@RequestParam(value="method", defaultValue="textrank") String method,
    		HttpServletRequest request) throws Exception {
    	
    	NLPMain nlpMain = NLPMain.getNLPMainInstance();
    	ArrayList<KeyPhraseSimple> keywords = nlpMain.extractKeywords(text, method);
    	KeywordListJSON keywordList = new KeywordListJSON(text, keywords);
        
    	return keywordList;
    }
    
}