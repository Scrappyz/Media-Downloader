package com.scrappyz.ytdlp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ProcessUtils {

    public static final Path executablePath = Paths.get("./src/main/resources/executables/yt-dlp.exe").toAbsolutePath().normalize();
    public static final Path downloadPath = Paths.get("./src/main/resources/temp").toAbsolutePath().normalize(); 

    private static List<String> commands = new ArrayList<>(Arrays.asList(executablePath.toString()));
 
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140)
    );
    
    public static String download(String url, int vidQuality, String audioFormat, String outputName) {

        vidQuality = resolveVideoQuality(vidQuality);
        StringBuilder format = new StringBuilder();

        if(vidQuality < 0) {
            format.append("bv");
        } else {

        }

        commands.addAll(Arrays.asList("-P", downloadPath.toString(), "-o", outputName));

        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder pb = new ProcessBuilder(commands);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
        } catch(IOException | InterruptedException e) {
            return "Something went wrong";
        }

        return output.toString();
    }

    private static int resolveVideoQuality(int vidQuality) {
        if(vidQuality < 144) return -1;

        if(videoQuality.contains(vidQuality)) return vidQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : videoQuality) {
            if(i == 144) {
                prev = i;
                continue;
            }

            if(vidQuality > prev && vidQuality < i) {
                vidQuality = prev;
                break;
            }

            prev = i;
        }

        return vidQuality;
    }
}
