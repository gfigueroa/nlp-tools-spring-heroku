package com.figueroa.controller;
 
import com.figueroa.nlp.NLPMain;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class KeywordExtractionController {

	/**
	 * Handles a GET request to the keywords view.
	 * Initializes and opens keywords view.
	 * @param model
	 * @param request
	 * @return the result view
	 */
    @RequestMapping(value="/keywords", method = RequestMethod.GET)
    public String getPage(ModelMap model, HttpServletRequest request) {

        model.addAttribute("text", "");
        model.addAttribute("keywordsText", "");
        // Prepare the result view:
        return "keywords";
    }
    
    /**
     * Handles a POST request to the keywords view.
     * Receives text parameter, extracts its keywords and returns keywords view.
     * @param model
     * @param request
     * @return the result view
     */
    @RequestMapping(value="/keywords", method = RequestMethod.POST)
    public String tagText(ModelMap model, HttpServletRequest request) {
        
        String contextPath = request.getSession().getServletContext().getRealPath("");
        
        try {
            String text = request.getParameter("text");
            String keywordsText = "";
            if (text != null) {
                ArrayList<String> keywords = 
                		NLPMain.extractKeywords(contextPath, text);
                for (String keyword : keywords) {
                	keywordsText += keyword + "\n";
                }
            }
            else {
                text = "";
            }

            model.addAttribute("text", text);
            model.addAttribute("keywordsText", keywordsText);
            
            // Prepare the result view:
            return "keywords";
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        }
    }
}