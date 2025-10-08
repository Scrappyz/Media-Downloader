package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.model.DownloadResult;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    
    public static final Path executablePath = Paths.get("./src/main/resources/executables/yt-dlp.exe").toAbsolutePath().normalize();
    public static final Path downloadPath = Paths.get("./temp").toAbsolutePath().normalize();

    // BAD: Gets modified everytime `download` gets called
    // private List<String> commands = new ArrayList<>(Arrays.asList(executablePath.toString()));
 
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final HashSet<String> audioCodec = new HashSet<>(
        Arrays.asList("flac", "alac", "wav", "aiff", "opus", "vorbis", "aac", "mp4a", "m4a", "mp3", "ac4", "eac3", "ac3", "dts")
    );

    private static final SortedSet<Integer> audioBitrate = new TreeSet<>(
        Arrays.asList(32, 64, 96, 128, 160, 192, 256, 320) // kbps
    );

    public enum MediaType {
        VIDEO("video"),
        VIDEO_ONLY("video_only"),
        AUDIO_ONLY("audio_only");

        private final String string;
        private static final HashMap<String, MediaType> byString = new HashMap<>();

        static {
            for(MediaType t: values()) {
                byString.put(t.string, t);
            }
        }

        private MediaType(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static MediaType getMediaType(String str) {
            return byString.get(str);
        }
    };

    private int resolveVideoQuality(int vidQuality) {
        Iterator<Integer> iterator = videoQuality.iterator();
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

    private int resolveAudioBitrate(int audBitrate) {
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

    // Methods:
    // Video + Audio
    // Video Only
    // Audio Only
    public DownloadResult download(String url, String type, int vidQuality, String audCodec, int audBitrate, String outputName) {
        // For video + audio: yt-dlp -f best -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
        // For video only: yt-dlp -f bv -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
        // For audio only: yt-dlp --audio-format mp3 --audio-quality 0 -x url

        DownloadResult result = new DownloadResult();

        if(url.isEmpty()) {
            result.setError(1);
            result.setMessage("No url provided");
            return result;
        }

        MediaType t = MediaType.getMediaType(type);

        boolean isVideo = (t == MediaType.VIDEO || t == MediaType.VIDEO_ONLY);
        boolean isVideoOnly = t == MediaType.VIDEO_ONLY;
        boolean isAudioOnly = t == MediaType.AUDIO_ONLY;

        vidQuality = resolveVideoQuality(vidQuality);
        audBitrate = resolveAudioBitrate(audBitrate);

        if(isAudioOnly && audCodec.isEmpty()) {
            audCodec = "mp3"; // Assume mp3
        }

        List<String> commands = new ArrayList<>();
        commands.add(executablePath.toString());

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

        log.debug("[COMMANDS] " + String.join(" ", commands));

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
            result.setError(1);
            result.setMessage(e.getMessage());
        }

        Path resourcePath = downloadPath.resolve(outputName).normalize();
        result.setResource(new FileSystemResource(resourcePath));

        return result;
    }

    public static void cleanDownloads() {
        try {
            FileUtils.cleanDirectory(downloadPath.toFile());
        } catch(IllegalArgumentException | IOException e) {
            e.printStackTrace();
        }
    }
}
