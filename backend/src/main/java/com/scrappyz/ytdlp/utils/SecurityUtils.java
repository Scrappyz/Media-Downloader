package com.scrappyz.ytdlp.utils;

import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    
    public String negatePathTraversal(String path) {
        return path.replace("..", "");
    }

}
