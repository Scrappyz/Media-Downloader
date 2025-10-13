package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.FullDownloadQueueException;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.exception.custom.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    private final PathProperties paths;

    @Lazy
    @Autowired
    private DownloadService async; // Allow us to execute the asynch method
 
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

    private final ConcurrentHashMap<String, String> resourceMap = new ConcurrentHashMap<>();

    @Value("#{${resource.expiry.time} * ${time.multiplier}}")
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

    public enum RequestStatus {
        SUCCESS("success"),
        FAILED("failed"),
        PROCESSING("processing"),
        PENDING("pending"),
        INVALID("invalid");

        private final String string;
        private static final HashMap<String, RequestStatus> byString = new HashMap<>();

        static {
            for(RequestStatus t: values()) {
                byString.put(t.string, t);
            }
        }

        private RequestStatus(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static RequestStatus getRequestStatus(String str) {
            return byString.get(str);
        }
    };

    public enum ErrorCode {
        DENIED("denied");

        private final String string;
        private static final HashMap<String, ErrorCode> byString = new HashMap<>();

        static {
            for(ErrorCode t: values()) {
                byString.put(t.string, t);
            }
        }

        private ErrorCode(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static ErrorCode getErrorCode(String str) {
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
    public DownloadResponse enqueue(DownloadRequest request) {
        DownloadResponse result = new DownloadResponse();
        CompletableFuture<DownloadResult> f = new CompletableFuture<>();
        String id = UlidCreator.getMonotonicUlid().toString();

        try {
            f = async.download(id, request); // Run in the background
        } catch(RejectedExecutionException e) {
            log.info("[ERROR] Rejected Execution due to full queue");
            throw new FullDownloadQueueException("Download queue is full");
        }

        result.setRequestId(id);

        processes.put(id, f);

        return result;
    }

    // Methods:
    // For video + audio: yt-dlp -f best -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
    // For video only: yt-dlp -f bv -S height:720 https://youtu.be/0LV7y_HnQr4?si=dmTkA17cgIxIy_Us
    // For audio only: yt-dlp --audio-format m4a --audio-quality 0 -x url
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
            result.setMessage("URL is empty");
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
            audCodec = ".m4a"; // Assume mp3
        }

        if(isAudioOnly) {
            outputName += audCodec;
        }

        List<String> commands = new ArrayList<>();
        commands.add(paths.getYtdlpBin().toString());

        if(isVideo) {
            commands.addAll(Arrays.asList("-f", "best", "-S", String.format("height:%d", vidQuality)));
        } else if(isVideoOnly) {
            commands.addAll(Arrays.asList("-f", "bv", "-S", String.format("height:%d", vidQuality)));
        } else if(isAudioOnly) {
            commands.addAll(Arrays.asList("--audio-format", audCodec, "--audio-quality", "0", "-x"));
        }

        commands.addAll(Arrays.asList(url, "-P", paths.getDownloadPath().toString()));

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
            result.setMessage("Something went wrong");
            return CompletableFuture.completedFuture(result);
        }

        result.setStatus(RequestStatus.SUCCESS.getString());
        result.setMessage("Download has finished");
        resourceMap.put(id, outputName);

        async.expireResource(outputName); // Remove in set time (ms)
        
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<DownloadResult> getProcess(String id) throws InvalidProcessException {
        if(!processes.containsKey(id)) {
            throw new InvalidProcessException("Process with request ID " + id + " could not be found");
        }

        return processes.get(id);
    }

    public String getProcessStatus(String id) throws InvalidProcessException {
        if(!processes.containsKey(id)) {
            throw new InvalidProcessException("Process with request ID " + id + " could not be found");
        }

        CompletableFuture<DownloadResult> future = processes.get(id);

        if(!future.isDone()) {
            return RequestStatus.PENDING.getString();
        }

        return future.getNow(new DownloadResult()).getStatus();
    }

    public boolean isProcessFinished(String id) throws InvalidProcessException {
        if(!processes.containsKey(id)) {
            throw new InvalidProcessException("Process with request ID " + id + " could not be found");
        }

        return processes.get(id).isDone();
    }

    public boolean isProcessExist(String id) {
        return processes.containsKey(id);
    }

    public boolean cancelProcess(String id) {
        boolean b = processes.get(id).cancel(true);
        processes.remove(id);
        return b;
    }

    public FileSystemResource getResource(String id, boolean removeInResourceMap) throws ResourceNotFoundException {
        String resourceName = resourceMap.get(id);
        File resourceFile = paths.getDownloadPath().resolve(resourceName).normalize().toFile();

        if(!resourceFile.exists()) {
            throw new ResourceNotFoundException("Cannot find " + resourceName);
        }

        if(removeInResourceMap) {
            resourceMap.remove(id);
        }

        return new FileSystemResource(resourceFile);
    }

    public FileSystemResource getResource(String id) throws ResourceNotFoundException {
        return getResource(id, true);
    }

    @Async("resourceExecutor")
    public CompletableFuture<Boolean> expireResource(String resourceName) { // Delete downloaded resource after a certain time
        try {
            Thread.sleep(resourceExpiryTime);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        Path resourcePath = paths.getDownloadPath().resolve(resourceName).normalize();
        try {
            boolean deleted = Files.deleteIfExists(resourcePath);
            log.info("Resource \"" + resourceName + "\" expired");
            return CompletableFuture.completedFuture(deleted);
        } catch(IOException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}
