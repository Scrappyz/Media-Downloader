package com.scrappyz.ytdlp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class MediaDownloader {
    public static final Logger logger = LoggerFactory.getLogger(MediaDownloader.class);

    public static final Path executablePath = Paths.get("./src/main/resources/executables/yt-dlp.exe").toAbsolutePath().normalize();
    public static final Path downloadPath = Paths.get("./temp").toAbsolutePath().normalize();

    private static List<String> commands = new ArrayList<>(Arrays.asList(executablePath.toString()));
 
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final HashSet<String> audioCodec = new HashSet<>(
        Arrays.asList("flac", "alac", "wav", "aiff", "opus", "vorbis", "aac", "mp4a", "m4a", "mp3", "ac4", "eac3", "ac3", "dts")
    );

    private static final SortedSet<Integer> audioBitrate = new TreeSet<>(
        Arrays.asList(32, 64, 96, 128, 160, 192, 256, 320) // kbps
    );

    public enum Type {
        VIDEO("video"),
        VIDEO_ONLY("video_only"),
        AUDIO_ONLY("audio_only");

        private final String string;
        private static final HashMap<String, Type> byString = new HashMap<>();

        static {
            for(Type t: values()) {
                byString.put(t.string, t);
            }
        }

        private Type(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static Type getType(String str) {
            return byString.get(str);
        }
    };
    
    // Methods:
    // Video + Audio
    // Video Only
    // Audio Only
    public static String download(String url, String type, int vidQuality, String audCodec, int audBitrate, String outputName) {
        // For video + audio: yt-dlp -f best -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
        // For video only: yt-dlp -f bv -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
        // For audio only: yt-dlp --audio-format mp3 --audio-quality 0 -x url

        if(url.isEmpty()) {
            return "No url given";
        }

        Type t = Type.getType(type);

        boolean isVideo = (t == Type.VIDEO || t == Type.VIDEO_ONLY);
        boolean isVideoOnly = t == Type.VIDEO_ONLY;
        boolean isAudioOnly = t == Type.AUDIO_ONLY;

        vidQuality = resolveVideoQuality(vidQuality);
        audBitrate = resolveAudioBitrate(audBitrate);

        if(isAudioOnly && audCodec.isEmpty()) {
            audCodec = "mp3"; // Assume mp3
        }

        if(isVideo) logger.info("Is Video");
        else logger.info("Bad");

        if(isVideo) {
            commands.addAll(Arrays.asList("-f", "best", "-S", String.format("height:%d", vidQuality)));
        } else if(isVideoOnly) {
            commands.addAll(Arrays.asList("-f", "bv", "-S", String.format("height:%d", vidQuality)));
        } else if(isAudioOnly) {
            commands.addAll(Arrays.asList("--audio-format", audCodec, "--audio-quality", "0", "-x"));
        }

        commands.addAll(Arrays.asList(url, "-P", downloadPath.toString()));

        if(!outputName.isEmpty()) {
            commands.addAll(Arrays.asList("-o", outputName));
        }

        // return String.join(" ", commands);

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

    public static void cleanDownloads() {
        try {
            FileUtils.cleanDirectory(downloadPath.toFile());
        } catch(IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }

    private static int resolveVideoQuality(int vidQuality) {
        Iterator<Integer> iterator = audioBitrate.iterator();
        int firstValue = iterator.next();

        if(vidQuality < firstValue) return -1;

        if(videoQuality.contains(vidQuality)) return vidQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : videoQuality) {
            if(i == firstValue) {
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

    private static int resolveAudioBitrate(int audBitrate) {
        Iterator<Integer> iterator = audioBitrate.iterator();
        int firstValue = iterator.next();

        if(audBitrate < firstValue) return -1;

        if(audioBitrate.contains(audBitrate)) return audBitrate;

        // Get the nearest audio bitrate
        int prev = -1;
        for(int i : audioBitrate) {
            if(i == firstValue) {
                prev = i;
                continue;
            }

            if(audBitrate > prev && audBitrate < i) {
                audBitrate = prev;
                break;
            }

            prev = i;
        }

        return audBitrate;
    }

    // public static String downloadVideo(String url, int vidQuality, ) {

    // }
}
