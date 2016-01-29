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
}