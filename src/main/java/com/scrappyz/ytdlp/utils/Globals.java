package com.scrappyz.ytdlp.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Globals {
    public static Path executablePath = Paths.get("./src/main/resources/executables/yt-dlp.exe").toAbsolutePath().normalize();
    public static Path downloadPath = Paths.get("./src/main/resources/temp").toAbsolutePath().normalize(); 

    public void printExecPath() {
        System.out.println(executablePath.toString());
    }
}
