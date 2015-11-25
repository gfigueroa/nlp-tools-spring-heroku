package com.figueroa.controller;
 
import com.figueroa.nlp.StemmerMain;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TaggerController {

    @RequestMapping(value="/tagger", method = RequestMethod.GET)
    public String getPage(ModelMap model, HttpServletRequest request) {

        model.addAttribute("originalText", "");
        model.addAttribute("taggedText", "");
        // Prepare the result view:
        return "tagger";
    }
    
    @RequestMapping(value="/tagger", method = RequestMethod.POST)
    public String tagText(ModelMap model, HttpServletRequest request) {
        
        String path = request.getSession().getServletContext().getRealPath("");
        StemmerMain stemmer = new StemmerMain(path);
        
        try {
            String taggedText;
            String text = request.getParameter("text");
            if (text != null) {
                taggedText = stemmer.tagText(text);
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