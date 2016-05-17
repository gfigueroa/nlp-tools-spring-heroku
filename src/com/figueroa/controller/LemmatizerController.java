package com.figueroa.controller;

import com.figueroa.nlp.NLPMain;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LemmatizerController {

	/**
	 * Handles a GET request to the lemmatizer view.
	 * Initializes and opens lemmatizer view.
	 * @param model
	 * @param request
	 * @return the result view
	 */
    @RequestMapping(value="/lemmatizer", method = RequestMethod.GET)
    public String getPage(ModelMap model, HttpServletRequest request) {

        model.addAttribute("originalText", "");
        model.addAttribute("lemmatizedText", "");
        // Prepare the result view:
        return "lemmatizer";
    }
    
    /**
     * Handles a POST request to the lemmatizer view.
     * Receives text parameter, lemmatizes it and returns lemmatizer view.
     * @param model
     * @param request
     * @return the result view
     */
    @RequestMapping(value="/lemmatizer", method = RequestMethod.POST)
    public String lemmatizeText(ModelMap model, HttpServletRequest request) {
        
        try {
            String lemmatizedText;
            String text = request.getParameter("text");
            if (text != null) {
            	NLPMain nlpMain = NLPMain.getNLPMainInstance();
                lemmatizedText = nlpMain.lemmatizeText(text);
            }
            else {
                text = "";
                lemmatizedText = "";
            }

            model.addAttribute("originalText", text);
            model.addAttribute("lemmatizedText", lemmatizedText);
            
            // Prepare the result view:
            return "lemmatizer";
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        }
    }
}