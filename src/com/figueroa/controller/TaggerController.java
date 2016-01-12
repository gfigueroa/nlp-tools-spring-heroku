package com.figueroa.controller;
 
import com.figueroa.nlp.NLPMain;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TaggerController {

	/**
	 * Handles a GET request to the tagger view.
	 * Initializes and opens tagger view.
	 * @param model
	 * @param request
	 * @return the result view
	 */
    @RequestMapping(value="/tagger", method = RequestMethod.GET)
    public String getPage(ModelMap model, HttpServletRequest request) {

        model.addAttribute("originalText", "");
        model.addAttribute("taggedText", "");
        // Prepare the result view:
        return "tagger";
    }
    
    /**
     * Handles a POST request to the tagger view.
     * Receives text parameter, tags it and returns tagger view.
     * @param model
     * @param request
     * @return the result view
     */
    @RequestMapping(value="/tagger", method = RequestMethod.POST)
    public String tagText(ModelMap model, HttpServletRequest request) {
        
        String contextPath = request.getSession().getServletContext().getRealPath("");
        //NLPMain stemmer = new NLPMain(contextPath);
        
        try {
            String taggedText;
            String text = request.getParameter("text");
            if (text != null) {
                taggedText = NLPMain.tagText(contextPath, text);
            }
            else {
                text = "";
                taggedText = "";
            }

            model.addAttribute("originalText", text);
            model.addAttribute("taggedText", taggedText);
            
            // Prepare the result view:
            return "tagger";
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        }
    }
}