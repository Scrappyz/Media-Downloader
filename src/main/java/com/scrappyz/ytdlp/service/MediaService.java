package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.model.DownloadResult;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.utils.ThreadUtils;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    @Lazy
    @Autowired
    private MediaService async; // Allow us to execute the asynch method
    
    public static final Path executablePath = Paths.get(System.getProperty("user.dir")).resolve("bin/yt-dlp.exe").toAbsolutePath().normalize();
    public static final Path downloadPath = Paths.get(System.getProperty("user.dir")).resolve("storage/private/downloads").toAbsolutePath().normalize();

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

    private final ConcurrentHashMap<String, CompletableFuture<DownloadResult>> processes = new ConcurrentHashMap<>();

    @Value("${resource.expiry.time:120000}")
    private long resourceExpiryTime;

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

    // ---HELPER METHODS---
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
    // ---HELPER METHODS---

    // Queue the download request
    public String enqueue(DownloadRequest request) {
        String id = UlidCreator.getMonotonicUlid().toString();
        CompletableFuture<DownloadResult> f = async.download(id, request); // Run in the background

        processes.put(id, f); // Keep track of the process with an id

        return id;
    }

    // Methods:
    // Video + Audio
    // Video Only
    // Audio Only
    // For video + audio: yt-dlp -f best -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
    // For video only: yt-dlp -f bv -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
    // For audio only: yt-dlp --audio-format mp3 --audio-quality 0 -x url
    @Async("downloadExecutor")
    public CompletableFuture<DownloadResult> download(String id, DownloadRequest request) {

        DownloadResult result = new DownloadResult();

        String url = request.getUrl();
        String type = request.getRequestType();
        int vidQuality = request.getVideoQuality();
        String audCodec = request.getAudioCodec();
        int audBitrate = request.getAudioBitrate();
        String outputName = id;

        if(url.isEmpty()) {
            result.setStatus("failed");
            result.setErrorMessage("URL is not provided");
            return CompletableFuture.completedFuture(result);
        }

        MediaType t = MediaType.getMediaType(type);

        boolean isVideo = (t == MediaType.VIDEO || t == MediaType.VIDEO_ONLY);
        boolean isVideoOnly = t == MediaType.VIDEO_ONLY;
        boolean isAudioOnly = t == MediaType.AUDIO_ONLY;

        vidQuality = resolveVideoQuality(vidQuality);
        audBitrate = resolveAudioBitrate(audBitrate);

        if(isVideo || isVideoOnly) {
            outputName += ".mp4";
        }

        if(audCodec.isEmpty()) {
            audCodec = "mp3"; // Assume mp3
        }

        if(isAudioOnly) {
            outputName += audCodec;
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

        commands.addAll(Arrays.asList("-o", outputName));

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
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
        }

        result.setDownloadResourceName(outputName);
        result.setStatus("success");

        async.expireResource(outputName); // Remove in set time (ms)

        // processes.remove(id); // Remove from processes once finished
        
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<DownloadResult> getProcess(String id) {
        return processes.get(id);
    }

    public boolean isProcessFinished(String id) {
        return processes.get(id).isDone();
    }

    public boolean cancelProcess(String id) {
        boolean b = processes.get(id).cancel(true);
        processes.remove(id);
        return b;
    }

    public FileSystemResource getResource(String resourceName) throws FileNotFoundException {
        File resourceFile = downloadPath.resolve(resourceName).normalize().toFile();

        if(!resourceFile.exists()) {
            throw new FileNotFoundException("Cannot find " + resourceName);
        }

        return new FileSystemResource(resourceFile);
    }

    @Async("resourceExecutor")
    public CompletableFuture<Boolean> expireResource(String resourceName) { // Delete downloaded resource after a certain time
        try {
            Thread.sleep(resourceExpiryTime);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        Path resourcePath = downloadPath.resolve(resourceName).normalize();
        try {
            boolean deleted = Files.deleteIfExists(resourcePath);
            return CompletableFuture.completedFuture(deleted);
        } catch(IOException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}
