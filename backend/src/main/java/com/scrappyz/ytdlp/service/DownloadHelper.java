package com.scrappyz.ytdlp.service;

import java.io.File;
import java.io.IOException;
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
import com.scrappyz.ytdlp.config.YtdlpConfig;
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
    private final YtdlpConfig ytdlpConfig;

    private final DownloadResourceHelper resourceHelper;
    private final YtdlpDownloadProcessHandler downloadProcessHandler;

    // Constants
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final SortedSet<Integer> audioQuality = new TreeSet<>(
        Arrays.asList(128, 192, 256, 320) // bitrate in kbps
    );

    private static final HashSet<String> audioCodec = new HashSet<>(
        Arrays.asList("flac", "alac", "wav", "aiff", "opus", "vorbis", "aac", "mp4a", "m4a", "mp3", "ac4", "eac3", "ac3", "dts")
    );

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
        String vidQuality = resolveVideoQuality(request.getVideoQuality());
        String audQuality = resolveAudioQuality(request.getAudioQuality());
        String audFormat = resolveAudioFormat(request.getAudioFormat());

        if(url.isEmpty()) {
            throw new InvalidUrlException("The URL provided is empty");
        }

        Site site = parseSite(url);

        log.info("[DownloadHelper.download] Downloading: " + url);

        RequestType t = RequestType.getMediaType(type);
        boolean isVideo = (t == RequestType.VIDEO || t == RequestType.VIDEO_ONLY);
        boolean isVideoOnly = t == RequestType.VIDEO_ONLY;
        boolean isAudioOnly = t == RequestType.AUDIO_ONLY;

        String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audQuality, audFormat);
        log.info("[DownloadHelper.download] Command Format: " + format);

        Path outputPath = paths.getDownloadPath().resolve(id).normalize();

        List<String> commands = new ArrayList<>();
        commands.add(paths.getYtdlpBin().toString());
        if(ytdlpConfig.isUseCookies()) {
            commands.addAll(Arrays.asList("--cookies-from-browser", ytdlpConfig.getBrowserCookies(), "--js-runtime", ytdlpConfig.getJsRuntime()));
        }
        if(request.isEmbedMetadata()) {
            commands.addAll(Arrays.asList("--embed-metadata", "--embed-thumbnail", "--convert-thumbnails", "jpg"));
        }
        commands.addAll(Arrays.asList("-f", format));
        commands.addAll(Arrays.asList(url, "-P", outputPath.toString()));
        commands.addAll(Arrays.asList("-o", id + ".%(ext)s", "--no-warnings", "--newline"));

        log.info("[DownloadHelper.download] Download Commands: " + String.join(" ", commands));

        // Run the download process
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
                resourceHelper.removeResource(id); // Remove any partially downloaded resources
                return;
            }

            log.info("[DownloadHelper.download] Remove process with ID " + id + " because of error");
            downloadProcessHandler.removeProcessById(id);
            throw new DownloadFailedException();
        }

        downloadProcessHandler.removeProcessById(id); // Remove the process from the map

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

        result.setStatus(RequestStatus.SUCCESS.getString());
        result.setMessage("Download has finished");

        resourceHelper.cleanup(id); // Cleanup resources in set time

        log.info("[DownloadHelper.download] Download with ID " + id + " has finished");

        try {
            emitter.send(SseEmitter.event()
                .name("status")
                .data(result)
            );
        } catch(IOException e) {
            log.info("[DownloadHelper.download] Failed to send initial pending status via SseEmitter");
        }

        emitter.complete();
    }

    // ---HELPER METHODS---
    public static String resolveVideoQuality(String vidQuality) {
        if(vidQuality == null || vidQuality.isEmpty()) {
            return "best";
        }

        vidQuality = vidQuality.trim().toLowerCase();

        if(vidQuality.equals("best")) {
            return "best";
        }

        if(vidQuality.equals("worst")) {
            return "worst";
        }

        int numericQuality = Integer.parseInt(vidQuality);
        vidQuality = String.valueOf(numericQuality);

        Iterator<Integer> iterator = videoQuality.iterator();
        int firstValue = iterator.next();

        if(numericQuality < firstValue) return String.valueOf(firstValue);

        if(videoQuality.contains(numericQuality)) return vidQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : videoQuality) {
            if(i == firstValue) {
                prev = i;
                continue;
            }

            if(numericQuality > prev && numericQuality < i) {
                numericQuality = prev;
                break;
            }

            prev = i;
        }

        return String.valueOf(numericQuality);
    }

    public static String resolveAudioQuality(String audQuality) {
        if(audQuality == null || audQuality.isEmpty()) {
            return "best";
        }

        audQuality = audQuality.trim().toLowerCase();

        if(audQuality.equals("best")) {
            return "best";
        }

        if(audQuality.equals("worst")) {
            return "worst";
        }

        int numericQuality = Integer.parseInt(audQuality);
        audQuality = String.valueOf(numericQuality);

        Iterator<Integer> iterator = audioQuality.iterator();
        int firstValue = iterator.next();

        if(numericQuality < firstValue) return String.valueOf(firstValue);

        if(audioQuality.contains(numericQuality)) return audQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : audioQuality) {
            if(i == firstValue) {
                prev = i;
                continue;
            }

            if(numericQuality > prev && numericQuality < i) {
                numericQuality = prev;
                break;
            }

            prev = i;
        }

        return String.valueOf(numericQuality);
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

    private String resolveCommandFormat(RequestType type, Site site, String vidFormat, String vidQuality, String audQuality, String audFormat) {
        boolean isVideo = (type == RequestType.VIDEO);
        boolean isVideoOnly = (type == RequestType.VIDEO_ONLY);
        String formatType = "bestvideo";

        if(type == RequestType.AUDIO_ONLY) {
            formatType = "bestaudio";
        }

        String format = formatType;

        if(isVideo || isVideoOnly) {
            format = "(" + formatType;
            
            if(vidQuality.equals("best")) {
                format += "";
            } else if(vidQuality.equals("worst")) {
                int lowestQuality = videoQuality.first();
                format += String.format("[height<=%s]", lowestQuality);
            } else {
                format += String.format("[height<=%s]", vidQuality);
            }
            
            if(!vidFormat.equals("default")) {
                format += String.format("[ext=%s]", vidFormat);
            } else {
                format += "[ext=mp4]/" + formatType + "[ext=mkv]/" + formatType + "[ext=webm]";
            }

            format += ")";

            if(isVideo) {
                format += "+(bestaudio[ext=m4a]/bestaudio[ext=mp3]/bestaudio)";
            }
        } else { // Audio only
            if(audFormat.equals("default")) {
                if(audQuality.equals("best")) {
                    format = "bestaudio[ext=flac]/bestaudio[ext=m4a]/bestaudio[ext=mp3]/bestaudio";
                } else if(audQuality.equals("worst")) {
                    format = String.format("bestaudio[ext=m4a][abr<=%s]/bestaudio[ext=mp3][abr<=%s]", 128, 128);
                } else {
                    format = String.format("bestaudio[ext=m4a][abr<=%s]/bestaudio[ext=mp3][abr<=%s]", audQuality, audQuality);
                }
            } else {
                if(audQuality.equals("best")) {
                    format = String.format("bestaudio[ext=%s]", audFormat);
                } else if(audQuality.equals("worst")) {
                    format = String.format("bestaudio[ext=%s][abr<=%s]", audFormat, 128);
                } else {
                    format = String.format("bestaudio[ext=%s][abr<=%s]", audFormat, audQuality);
                }
            }
        }

        if(isVideo && site != Site.YOUTUBE) { // For non-Youtube sites
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

    public FileSystemResource getResource(String id) throws ResourceNotFoundException {
        Path resourcePath = paths.getDownloadPath().resolve(id).normalize();

        File directory = new File(resourcePath.toString());
        if(!directory.exists() || !directory.isDirectory()) {
            throw new ResourceNotFoundException("[DownloadHelper.getResource] Resource with request ID '" + id + "' could not be found");
        }

        File[] files = directory.listFiles();
        if(files == null || files.length == 0) {
            throw new ResourceNotFoundException("[DownloadHelper.getResource] Resource with request ID '" + id + "' has no files");
        }

        FileSystemResource resource = new FileSystemResource(files[0]);
        return resource;
    }
}
