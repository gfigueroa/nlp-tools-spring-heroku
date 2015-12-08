package com.figueroa.controller;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.figueroa.nlp.Greeting;

@RestController
@RequestMapping("/ws/greeting/")
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value = "/{name}", method = RequestMethod.GET, headers="Accept=application/json")
    public Greeting greeting(@PathVariable String name) {
        return new Greeting(counter.incrementAndGet(),
                            String.format(template, name));
    }
}