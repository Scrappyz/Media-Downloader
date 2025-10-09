package com.scrappyz.ytdlp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequiredArgsConstructor
@RequestMapping("/resources")
public class ResourceController {
    
    @GetMapping("/get/{id}")
    public String getResource(@PathVariable String id) {
        return "hello";
    }
    
}
