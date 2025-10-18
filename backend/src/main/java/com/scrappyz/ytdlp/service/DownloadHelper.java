package com.scrappyz.ytdlp.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.exception.custom.FailedProcessException;
import com.scrappyz.ytdlp.exception.custom.FormatUnavailableException;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.service.DownloadService.RequestStatus;
import com.scrappyz.ytdlp.utils.ProcessUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadHelper.class);
    
    private final PathProperties paths;

    private final DownloadResourceHelper resourceHelper;

    // Constants
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final HashSet<String> audioCodec = new HashSet<>(
        Arrays.asList("flac", "alac", "wav", "aiff", "opus", "vorbis", "aac", "mp4a", "m4a", "mp3", "ac4", "eac3", "ac3", "dts")
    );

    private final ConcurrentHashMap<String, CompletableFuture<DownloadResult>> processes = new ConcurrentHashMap<>();

    private final Set<String> cancelled = new ConcurrentHashMap<>().newKeySet();

    private final ConcurrentHashMap<String, String> resourceMap = new ConcurrentHashMap<>();

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

    public enum Site {
        YOUTUBE("youtube");

        private final String string;
        private static final HashMap<String, Site> byString = new HashMap<>();

        static {
            for(Site t: values()) {
                byString.put(t.string, t);
            }
        }

        private Site(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static Site getSite(String str) {
            return byString.get(str);
        }
    }

    public enum ErrorCode {
        UNSUPPORTED_URL("unsupported_url"),
        INVALID_URL("invalid_url"),
        FORMAT_UNAVAILABLE("format_unavailable");

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

    // Methods:
    // For video + audio: yt-dlp -f best[ext=mp4][height<=720] <url>
    // For video only: yt-dlp -f bestvideo[ext=mp4][height<=720] <url>
    // For audio only: yt-dlp -f bestaudio[ext=m4a] <url>
    // For getting filename ahead of time: yt-dlp -o "%(title)s.%(ext)s" --get-filename <url>
    @Async("downloadExecutor")
    public CompletableFuture<DownloadResult> download(String id, DownloadRequest request) 
        throws InvalidUrlException, UnsupportedUrlException, FormatUnavailableException, DownloadFailedException, FailedProcessException {

        DownloadResult result = new DownloadResult();

        String url = request.getUrl();
        String type = request.getRequestType();
        String vidFormat = resolveVideoFormat(request.getVideoFormat()); 
        int vidQuality = resolveVideoQuality(request.getVideoQuality());
        String audFormat = resolveAudioFormat(request.getAudioFormat());
        String outputName = id;

        if(url.isEmpty()) {
            throw new InvalidUrlException("The URL provided is empty");
        }

        if(vidQuality < 0) {
            vidQuality = 360;
        }

        Site site = parseSite(url);

        log.info("[DownloadHelper.download] Downloading: " + url);

        MediaType t = MediaType.getMediaType(type);
        boolean isVideo = (t == MediaType.VIDEO || t == MediaType.VIDEO_ONLY);
        boolean isVideoOnly = t == MediaType.VIDEO_ONLY;
        boolean isAudioOnly = t == MediaType.AUDIO_ONLY;

        String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audFormat);
        log.info("[DownloadHelper.download] Command Format: " + format);

        log.info("[DownloadHelper.download] Got output name '" + outputName + "'");

        List<String> commands = new ArrayList<>();
        commands.add(paths.getYtdlpBin().toString());
        commands.addAll(Arrays.asList("-f", format));
        commands.addAll(Arrays.asList(url, "-P", paths.getDownloadPath().toString()));
        commands.addAll(Arrays.asList("-o", outputName + ".%(ext)s", "--no-warnings", "--no-progress"));

        log.info("[DownloadHelper.download] Download Commands: " + String.join(" ", commands));

        ProcessUtils.ProcessResult processResult = new ProcessUtils.ProcessResult();

        try {
            processResult = ProcessUtils.runProcess(commands);
        } catch(IOException | InterruptedException e) {
            log.info("[DownloadHelper.download] Remove process with ID " + id + " because of error");
            throw new DownloadFailedException();
        }

        List<String> successOutput = processResult.getOutput();
        List<String> errorOutput = processResult.getErrorOutput();
        ErrorCode error = null;

        if(!errorOutput.isEmpty()) {
            error = parseError(errorOutput.get(errorOutput.size() - 1));
        }

        if(error == ErrorCode.INVALID_URL) {
            log.info("[DownloadHelper.download] Invalid URL");
            throw new InvalidUrlException("The URL '" + url + "' is invalid");
        }

        if(error == ErrorCode.UNSUPPORTED_URL) {
            log.info("[DownloadHelper.download] Unsupported URL");
            throw new UnsupportedUrlException("The URL '" + url + "'' is unsupported");
        }

        if(error == ErrorCode.FORMAT_UNAVAILABLE) {
            log.info("[DownloadHelper.download] Format unavailable");
            throw new FormatUnavailableException("The requested format is unavailable");
        }

        outputName = parseFilenameFromOutputStream(successOutput);
        log.info("[DownloadHelper.download] Output filename is '" + outputName + "'");

        result.setStatus(RequestStatus.SUCCESS.getString());
        result.setMessage("Download has finished");
        resourceMap.put(id, outputName);

        resourceHelper.cleanup(id, outputName, processes, cancelled, resourceMap); // Cleanup resources in set time

        log.info("[DownloadHelper.download] Download with ID " + id + " has finished");
        
        return CompletableFuture.completedFuture(result);
    }

    // ---HELPER METHODS---
    public static int resolveVideoQuality(int vidQuality) {
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

    public static String resolveVideoFormat(String videoFormat) {
        if(videoFormat == null || videoFormat.isEmpty() || videoFormat.equals("Default")) {
            return "default";
        }

        return videoFormat;
    }

    public static String resolveAudioFormat(String audioFormat) {
        if(audioFormat == null || audioFormat.isEmpty() || audioFormat.equals("Default")) {
            return "default";
        }

        return audioFormat;
    }

    private String resolveCommandFormat(MediaType type, Site site, String videoFormat, int videoQuality, String audioFormat) {
        if(type == MediaType.VIDEO) {
            if(videoFormat.equals("default")) {
                return String.format("best[height<=%d]", videoQuality);
            }
            return String.format("best[ext=%s][height<=%d]", videoFormat, videoQuality, videoQuality);
        } else if(type == MediaType.VIDEO_ONLY) {
            if(videoFormat.equals("default")) {
                return String.format("bestvideo[height<=%d]", videoQuality);
            }
            return String.format("bestvideo[ext=%s][height<=%d]", videoFormat, videoQuality, videoQuality);
        } else if(type == MediaType.AUDIO_ONLY) {
            if(audioFormat.equals("default")) {
                return "bestaudio[ext=mp3]/bestaudio[ext=m4a]/bestaudio";
            }

            return String.format("bestaudio[ext=%s]", audioFormat);
        }

        return "best";
    }

    private ErrorCode parseError(String error) {
        if(error.contains("Unsupported URL")) {
            return ErrorCode.UNSUPPORTED_URL;
        }

        if(error.contains("not a valid URL")) {
            return ErrorCode.INVALID_URL;
        }

        if(error.contains("Requested format is not available")) {
            return ErrorCode.FORMAT_UNAVAILABLE;
        }

        return null;
    }

    private Site parseSite(String url) {
        Map<String, Site> siteMap = Map.ofEntries(
            Map.entry("youtube.com", Site.YOUTUBE),
            Map.entry("youtu.be", Site.YOUTUBE)
        );

        for (Map.Entry<String, Site> entry : siteMap.entrySet()) {
            if(url.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String parseFilenameFromOutputStream(List<String> output) {
        if(output.isEmpty()) {
            return null;
        }

        String temp = "";
        int i = output.size() - 1;
        while(i >= 0) {
            temp = output.get(i);

            if(temp.startsWith("[download] Destination:")) {
                break;
            }

            i--;
        }

        if(i < 0) {
            return null;
        }

        int startIndex = temp.lastIndexOf('\\');

        if(startIndex < 0) {
            startIndex = temp.lastIndexOf('/');
        }

        String filename = temp.substring(startIndex + 1);
        return filename;
    }
    // ---HELPER METHODS---

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

    public void addProcess(String id, CompletableFuture<DownloadResult> future) {
        processes.put(id, future);
    }

    public boolean cancelProcess(String id) {
        boolean b = processes.get(id).cancel(true);
        processes.remove(id);
        cancelled.add(id);
        return b;
    }

    public boolean removeProcess(String id) {
        boolean b = processes.get(id).cancel(true);
        processes.remove(id);
        return b;
    }

    public FileSystemResource getResource(String id, boolean removeInResourceMap) throws ResourceNotFoundException {

        if(!resourceMap.containsKey(id)) {
            log.info("[DownloadHelper.getResource] Not in resourceMap");
            throw new ResourceNotFoundException("Could not find resource with ID of " + id);
        }

        String resourceName = resourceMap.get(id);
        File resourceFile = paths.getDownloadPath().resolve(resourceName).normalize().toFile();

        if(cancelled.contains(id) || !resourceFile.exists()) {
            log.info("[DownloadHelper.getResource] Either cancelled or does not exist");
            throw new ResourceNotFoundException("Could not find resource '" + resourceName + "'");
        }

        if(removeInResourceMap) {
            resourceMap.remove(id);
        }

        return new FileSystemResource(resourceFile);
    }

    public FileSystemResource getResource(String id) throws ResourceNotFoundException {
        return getResource(id, true);
    }

    public boolean removeResource(String id) {
        String resourceName = resourceMap.get(id);
        Path resourcePath = paths.getDownloadPath().resolve(resourceName).normalize();
        boolean deleted;

        try {
            deleted = Files.deleteIfExists(resourcePath);
        } catch(IOException e) {
            return false;
        }

        resourceMap.remove(id);

        return deleted;
    }

    public String getProcessesAsString() {
        return processes.keySet().toString();
    }

    public String getResourceMapAsString() {
        return resourceMap.keySet().toString();
    }

    public String getCancelledAsString() {
        return cancelled.toString();
    }
}
