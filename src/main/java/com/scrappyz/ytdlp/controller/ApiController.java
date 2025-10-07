package com.scrappyz.ytdlp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.utils.Globals;
import com.scrappyz.ytdlp.utils.ProcessUtils;

@RestController
public class ApiController {
    
    @GetMapping("/print")
    public String print() {
        return Globals.executablePath.toString();
    }

    @GetMapping("/yt")
    public String processExec() {
        return ProcessUtils.download("https://youtube.com/shorts/9TQlX9gQ7a4?si=uLCr5oU_MyPZVY1I", "test.mp4");
    }
    
}
