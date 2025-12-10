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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.ApiError;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.exception.custom.FailedProcessException;
import com.scrappyz.ytdlp.exception.custom.FormatUnavailableException;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.helper.YtdlpProcessResult;
import com.scrappyz.ytdlp.service.DownloadService.RequestStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadHelper.class);

    private final ObjectProvider<DownloadProgressHelper> progressHelperProvider;
    
    private final PathProperties paths;

    private final DownloadResourceHelper resourceHelper;
    private final YtdlpDownloadProcessHandler downloadProcessHandler;

    // Constants
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final HashSet<String> audioCodec = new HashSet<>(
        Arrays.asList("flac", "alac", "wav", "aiff", "opus", "vorbis", "aac", "mp4a", "m4a", "mp3", "ac4", "eac3", "ac3", "dts")
    );

    private final ConcurrentHashMap<String, String> resourceMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public enum RequestType {
        VIDEO("video"),
        VIDEO_ONLY("video_only"),
        AUDIO_ONLY("audio_only");

        private final String string;
        private static final HashMap<String, RequestType> byString = new HashMap<>();

        static {
            for(RequestType t: values()) {
                byString.put(t.string, t);
            }
        }

        private RequestType(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static RequestType getMediaType(String str) {
            return byString.get(str);
        }
    };

    public enum Site {
        YOUTUBE("youtube"),
        FACEBOOK("facebook"),
        INSTAGRAM("instagram"),
        UNKNOWN("unknown");

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

    public static class ProcessResult {
        private String outputName;
        private ErrorCode error;

        public String getOutputName() {
            return outputName;
        }

        public void setOutputName(String outputName) {
            this.outputName = outputName;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }

        public boolean hasOutputName() {
            return outputName != null && !outputName.isEmpty();
        }
    }

    // Methods:
    // For video + audio: yt-dlp -f best[ext=mp4][height<=720] <url>
    // For video only: yt-dlp -f bestvideo[ext=mp4][height<=720] <url>
    // For audio only: yt-dlp -f bestaudio[ext=m4a] <url>
    // For getting filename ahead of time: yt-dlp -o "%(title)s.%(ext)s" --get-filename <url>
    @Async("downloadExecutor")
    public void download(String id, DownloadRequest request) 
        throws InvalidUrlException, UnsupportedUrlException, FormatUnavailableException, DownloadFailedException, FailedProcessException {

        DownloadResult result = new DownloadResult("pending", "Download is pending");
        SseEmitter emitter = emitters.get(id);

        // Run when the download is complete
        emitter.onCompletion(() -> {
            log.info("[DownloadHelper.download] SseEmitter with ID " + id + " has completed");
            emitters.remove(id);
        });

        try {
            emitter.send(SseEmitter.event()
                .name("status")
                .data(result)
            );
        } catch(IOException e) {
            log.info("[DownloadHelper.download] Failed to send initial pending status via SseEmitter");
        }

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

        RequestType t = RequestType.getMediaType(type);
        boolean isVideo = (t == RequestType.VIDEO || t == RequestType.VIDEO_ONLY);
        boolean isVideoOnly = t == RequestType.VIDEO_ONLY;
        boolean isAudioOnly = t == RequestType.AUDIO_ONLY;

        String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audFormat);
        log.info("[DownloadHelper.download] Command Format: " + format);

        log.info("[DownloadHelper.download] Got output name '" + outputName + "'");

        List<String> commands = new ArrayList<>();
        commands.add(paths.getYtdlpBin().toString());
        commands.addAll(Arrays.asList("-f", format));
        commands.addAll(Arrays.asList(url, "-P", paths.getDownloadPath().toString()));
        commands.addAll(Arrays.asList("-o", outputName + ".%(ext)s", "--no-warnings", "--newline"));

        log.info("[DownloadHelper.download] Download Commands: " + String.join(" ", commands));

        YtdlpProcessResult processResult = new YtdlpProcessResult();

        try {
            DownloadProgressHelper progressHelper = progressHelperProvider.getObject();
            processResult = downloadProcessHandler.runProcess(
                commands,
                id,
                emitter,
                (line, em) -> {
                    progressHelper.processLine(line, em); // Or handle the output line as needed
                },
                (line) -> {
                    return parseError(line); // Or handle the error line as needed
                }
            );
        } catch(DownloadFailedException e) { // If something goes wrong with the download process or user cancelled
            if(downloadProcessHandler.isProcessCancelled(id)) {
                log.info("[DownloadHelper.download] Download with ID " + id + " was cancelled");
                result.setStatus("cancelled");
                result.setMessage("Download was cancelled");

                try {
                    emitter.send(SseEmitter.event()
                        .name("status")
                        .data(result)
                    );
                } catch(IOException ex) {
                    log.info("[DownloadHelper.download] Failed to send download cancelled status via SseEmitter");
                }

                downloadProcessHandler.removeProcessById(id);
                emitter.complete();
                return;
            }

            log.info("[DownloadHelper.download] Remove process with ID " + id + " because of error");
            throw new DownloadFailedException();
        }

        downloadProcessHandler.removeProcessById(id);

        // ========PROCESS COMPLETED SUCCESSFULLY========

        // Handle errors
        ErrorCode error = processResult.getError();
        try {
            if(error == ErrorCode.INVALID_URL) {
                log.info("[DownloadHelper.download] Invalid URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.INVALID_URL.getString(), "The URL provided is invalid"))
                );
            }

            if(error == ErrorCode.UNSUPPORTED_URL) {
                log.info("[DownloadHelper.download] Unsupported URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.UNSUPPORTED_URL.getString(), "The URL provided is invalid"))
                );
            }

            if(error == ErrorCode.FORMAT_UNAVAILABLE) {
                log.info("[DownloadHelper.download] Format unavailable");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.FORMAT_UNAVAILABLE.getString(), "The URL provided is invalid"))
                );
            }

            if(error != null) {
                emitter.complete();
                return;
            }

        } catch(IOException e) {
            log.info("[DownloadHelper.download] Failed to send download failed status via SseEmitter");
            emitter.completeWithError(e);
            return;
        }

        // ========DOWNLOAD COMPLETED SUCCESSFULLY========

        outputName = processResult.getOutputName();
        log.info("[DownloadHelper.download] Output filename is '" + outputName + "'");

        result.setStatus(RequestStatus.SUCCESS.getString());
        result.setMessage("Download has finished");

        resourceMap.put(id, outputName);

        resourceHelper.cleanup(id, outputName, resourceMap); // Cleanup resources in set time

        log.info("[DownloadHelper.download] Download with ID " + id + " has finished");

        try {
            emitter.send(SseEmitter.event()
                .name("status")
                .data(result)
            );
        } catch(IOException e) {
            log.info("[DownloadHelper.download] Failed to send initial pending status via SseEmitter");
        }

        // result.setStatus("success");

        // try {
        //     emitter.send(SseEmitter.event()
        //         .name("status")
        //         .data(result) 
        //     );
        // } catch(IOException e) {
        //     log.info("[DownloadHelper.processLine] Failed to send progress update via SseEmitter");
        // }

        emitter.complete();
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

    private String resolveCommandFormat(RequestType type, Site site, String videoFormat, int videoQuality, String audioFormat) {
        
        boolean isVideo = (type == RequestType.VIDEO || type == RequestType.VIDEO_ONLY);
        String formatType = "best";

        if(type == RequestType.VIDEO_ONLY) {
            formatType += "video";
        }

        if(type == RequestType.AUDIO_ONLY) {
            formatType = "bestaudio";
        }

        String format = formatType;

        if(isVideo) {
            format += String.format("[height<=%d]", videoQuality);

            if(!videoFormat.equals("default")) {
                format += String.format("[ext=%s]", videoFormat);
            }
        } else {
            if(audioFormat.equals("default")) {
                format = "bestaudio[ext=flac]/bestaudio[ext=m4a]/bestaudio[ext=mp3]/bestaudio";
            } else {
                format += String.format("[ext=%s]", audioFormat);
            }
        }

        if(isVideo && site != Site.YOUTUBE) {
            format += "/" + formatType;
        }

        return format;
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
            Map.entry("youtu.be", Site.YOUTUBE),
            Map.entry("facebook.com", Site.FACEBOOK),
            Map.entry("instagram.com", Site.INSTAGRAM)
        );

        for (Map.Entry<String, Site> entry : siteMap.entrySet()) {
            if(url.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return Site.UNKNOWN;
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

    public void cancelDownload(String id) {
        downloadProcessHandler.stopProcessById(id, true);
    }

    public void addEmitter(String id, SseEmitter emitter) {
        emitters.put(id, emitter);
    }

    public void removeEmitter(String id, Exception e) {
        emitters.get(id).completeWithError(e);
        emitters.remove(id);
    }

    public void removeEmitter(String id) {
        emitters.get(id).complete();
        emitters.remove(id);
    }

    public SseEmitter getEmitter(String id) throws InvalidProcessException {
        if(!emitters.containsKey(id)) {
            throw new InvalidProcessException("Emitter with request ID " + id + " could not be found");
        }

        return emitters.get(id);
    }

    public FileSystemResource getResource(String id, boolean removeInResourceMap) throws ResourceNotFoundException {

        if(!resourceMap.containsKey(id)) {
            log.info("[DownloadHelper.getResource] Not in resourceMap");
            throw new ResourceNotFoundException("Could not find resource with ID of " + id);
        }

        String resourceName = resourceMap.get(id);
        File resourceFile = paths.getDownloadPath().resolve(resourceName).normalize().toFile();

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

    public String getResourceMapAsString() {
        return resourceMap.keySet().toString();
    }
}
