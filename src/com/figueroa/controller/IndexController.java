package com.figueroa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {

    @RequestMapping(value={"/", "index", ""}, method = RequestMethod.GET)
    /**
     * Handles a GET request for the index page of the site.
     * It accepts "/", "index", or an empty URL.
     * @return the result view
     */
    public String getPage() {
        // Prepare the result view:
        return "/index";
    }
    
    @RequestMapping(value={"/sample"}, method = RequestMethod.GET)
    /**
     * Handles a GET request for the sample page of the site.
     * It accepts "/sample".
     * @return the result view
     */
    public String getSamplePage() {
        // Prepare the result view:
        return "/sample";
    }
    
    @RequestMapping(value={"/index_new"}, method = RequestMethod.GET)
    /**	
     *  Handles a GET request for the NEW index page of the site.
     * It accepts "/index_new".
     * @return the result view
     */
    public String getNewIndexPage() {
        // Prepare the result view:
        return "/index_new";
    }
}