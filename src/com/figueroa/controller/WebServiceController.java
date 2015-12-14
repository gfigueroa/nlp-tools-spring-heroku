package com.figueroa.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	
    @RequestMapping(value = "/lemmatize", method = RequestMethod.GET, 
    		headers="Accept=application/json")
    public LemmatizedTextJSON lemmatizeText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {
    	
    	String contextPath = request.getSession().getServletContext().getRealPath("");
        NLPMain nlpMain = new NLPMain(contextPath);
        
        String lemmatizedText = nlpMain.lemmatizeText(text);
        
    	LemmatizedTextJSON lt = new LemmatizedTextJSON(text, lemmatizedText);
    	
    	return lt;
    }
    
    @RequestMapping(value = "/tag", method = RequestMethod.GET, 
    		headers="Accept=application/json")
    public TaggedTextJSON tagText(
    		@RequestParam(value="text", defaultValue="") String text,
    		HttpServletRequest request) throws Exception {
    	
    	String contextPath = request.getSession().getServletContext().getRealPath("");
        NLPMain nlpMain = new NLPMain(contextPath);
        
        String taggedText = nlpMain.tagText(text);
        
        TaggedTextJSON tt = new TaggedTextJSON(text, taggedText);
    	
    	return tt;
    }
    
}