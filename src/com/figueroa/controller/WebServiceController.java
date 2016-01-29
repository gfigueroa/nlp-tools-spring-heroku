package com.figueroa.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.figueroa.nlp.KeyPhrase;
import com.figueroa.nlp.NLPMain;

@RestController
@RequestMapping("/ws/")
public class WebServiceController {
	
	private class LemmatizedTextJSON {
		public String originalText;
		public String lemmatizedText;
		public LemmatizedTextJSON(String originalText, String lemmatizedText) {
			this.originalText = originalText;
			this.lemmatizedText = lemmatizedText;
		}
	};
	
	private class TaggedTextJSON {
		public String originalText;
		public String taggedText;
		public TaggedTextJSON(String originalText, String taggedText) {
			this.originalText = originalText;
			this.taggedText = taggedText;
		}
	};
	
	public class KeywordListJSON {
		public String originalText;
		public ArrayList<KeyPhrase> keywords;
		public KeywordListJSON(String originalText, ArrayList<KeyPhrase> keywords) {
			this.originalText = originalText;
			this.keywords = keywords;
		}
	}
	
    @RequestMapping(value = "/lemmatize", method = RequestMethod.GET, 
    		headers="Accept=application/json")
    public LemmatizedTextJSON lemmatizeText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {
    	
    	String contextPath = request.getSession().getServletContext().getRealPath("");
        //NLPMain nlpMain = new NLPMain(contextPath);
        
        String lemmatizedText = NLPMain.lemmatizeText(contextPath, text);
        
    	LemmatizedTextJSON lt = new LemmatizedTextJSON(text, lemmatizedText);
    	
    	return lt;
    }
    
    @RequestMapping(value = "/tag", method = RequestMethod.GET, 
    		headers="Accept=application/json")
    public TaggedTextJSON tagText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {
    	
    	String contextPath = request.getSession().getServletContext().getRealPath("");
        //NLPMain nlpMain = new NLPMain(contextPath);
        
        String taggedText = NLPMain.tagText(contextPath, text);
        
        TaggedTextJSON tt = new TaggedTextJSON(text, taggedText);
    	
    	return tt;
    }
    
    @RequestMapping(value = "/keywords", method = RequestMethod.GET, 
    		headers="Accept=application/json")
    public KeywordListJSON extractKeywords(
    		@RequestParam(value="text", defaultValue="") String text,
    		@RequestParam(value="method", defaultValue="textrank") String method,
    		HttpServletRequest request) throws Exception {
    	
    	String contextPath = request.getSession().getServletContext().getRealPath("");
        //NLPMain nlpMain = new NLPMain(contextPath);
        
    	ArrayList<KeyPhrase> keywords;
    	
    	if (method.equalsIgnoreCase("textrank")) {
    		keywords = NLPMain.extractKeywords(contextPath, text);
    	}
    	else {
    		return null;
    	}
    	
    	KeywordListJSON keywordList = new KeywordListJSON(text, keywords);
        
    	return keywordList;
    }
    
}