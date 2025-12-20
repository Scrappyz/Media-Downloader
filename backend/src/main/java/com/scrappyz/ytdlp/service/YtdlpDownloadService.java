package com.scrappyz.ytdlp.service;

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
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.f4b6a3.ulid.UlidCreator;
import com.scrappyz.ytdlp.config.DownloadProperties;
import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.config.YtdlpConfig;
import com.scrappyz.ytdlp.dto.ApiError;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.exception.custom.FailedProcessException;
import com.scrappyz.ytdlp.exception.custom.FormatUnavailableException;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.helper.YtdlpProcessResult;

import lombok.RequiredArgsConstructor;

@Service("ytdlp")
@Primary
@RequiredArgsConstructor
public class YtdlpDownloadService implements DownloadService {

    private static final Logger log = LoggerFactory.getLogger(YtdlpDownloadService.class);

    private final ObjectProvider<DownloadProgressHelper> progressHelperProvider;
    
    private final PathProperties paths;
    private final YtdlpConfig ytdlpConfig;
    private final DownloadProperties downloadProperties;

    @Qualifier("downloadExecutor")
    private final ExecutorService downloadExecutor;

    private final DownloadResourceHelper resourceHelper;
    private final YtdlpDownloadProcessHandler downloadProcessHandler;

    // Constants
    private static final SortedSet<Integer> videoQuality = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140) // height in pixels (p)
    );

    private static final SortedSet<Integer> audioQuality = new TreeSet<>(
        Arrays.asList(128, 192, 256, 320) // bitrate in kbps
    );

    private static final HashSet<String> videoFormat = new HashSet<>(
        Arrays.asList("mp4", "mkv")
    );

    private static final HashSet<String> audioFormat = new HashSet<>(
        Arrays.asList("flac", "m4a", "mp3")
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
        NONE("none"),
        UNSUPPORTED_URL("unsupported_url"),
        INVALID_URL("invalid_url"),
        FORMAT_UNAVAILABLE("format_unavailable"),
        POSTPROCESSING_ERROR("postprocessing_error"),
        FAILED_UNEXPECTEDLY("failed_unexpectedly");

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

    // Queue the download request
    @Override
    public DownloadResponse enqueue(DownloadRequest request) {
        DownloadResponse result = new DownloadResponse();
        String id = UlidCreator.getMonotonicUlid().toString();

        addEmitter(id, new SseEmitter(downloadProperties.getTimeout().toMillis()));
        downloadExecutor.submit(() -> download(id, request));

        result.setRequestId(id);

        return result;
    }

    @Override
    public void cancelDownload(String id) {
        downloadProcessHandler.cancelProcessById(id);
    }

    @Override
    public SseEmitter getEmitter(String id) throws InvalidProcessException {
        if(!emitters.containsKey(id)) {
            throw new InvalidProcessException("Emitter with request ID " + id + " could not be found");
        }

        return emitters.get(id);
    }

    @Override
    public FileSystemResource getResource(String id) throws ResourceNotFoundException {
        FileSystemResource resource = new FileSystemResource(resourceHelper.getFile(id));
        return resource;
    }

    private void download(String id, DownloadRequest request) 
        throws InvalidUrlException, UnsupportedUrlException, FormatUnavailableException, DownloadFailedException, FailedProcessException {

        DownloadResult result = new DownloadResult("pending", "Download is pending");
        SseEmitter emitter = emitters.get(id);

        // Run when the download is complete
        emitter.onCompletion(() -> {
            log.info("[YtdlpDownloadService.download] SseEmitter with ID " + id + " has completed");
            emitters.remove(id);
        });

        try {
            emitter.send(SseEmitter.event()
                .name("status")
                .data(result)
            );
        } catch(IOException e) {
            log.info("[YtdlpDownloadService.download] Failed to send initial pending status via SseEmitter");
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

        log.info("[YtdlpDownloadService.download] Downloading: " + url);

        RequestType t = RequestType.getMediaType(type);
        boolean isVideo = (t == RequestType.VIDEO || t == RequestType.VIDEO_ONLY);
        boolean isVideoOnly = t == RequestType.VIDEO_ONLY;
        boolean isAudioOnly = t == RequestType.AUDIO_ONLY;

        String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audQuality, audFormat);
        log.info("[YtdlpDownloadService.download] Command Format: " + format);

        Path outputPath = paths.getDownloadPath().resolve(id).normalize();

        // Compile commands
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

        log.info("[YtdlpDownloadService.download] Download Commands: " + String.join(" ", commands));

        // Run the download process
        YtdlpProcessResult processResult = new YtdlpProcessResult();
        try {
            DownloadProgressHelper progressHelper = progressHelperProvider.getObject();
            processResult = downloadProcessHandler.runProcess(
                commands,
                id,
                emitter,
                (line, em) -> {
                    return progressHelper.processLine(line, em); // Or handle the output line as needed
                },
                (line) -> {
                    return parseError(line); // Or handle the error line as needed
                }
            );
        } catch(DownloadFailedException e) { // If something goes wrong with the download process
            log.info("[YtdlpDownloadService.download] Remove process with ID " + id + " because of error");
            downloadProcessHandler.removeProcessById(id);
            throw new DownloadFailedException();
        }

        if(downloadProcessHandler.isProcessCancelled(id)) {
            log.info("[YtdlpDownloadService.download] Download with ID " + id + " was cancelled");
            result.setStatus("cancelled");
            result.setMessage("Download was cancelled");

            try {
                emitter.send(SseEmitter.event()
                    .name("status")
                    .data(result)
                );
            } catch(IOException ex) {
                log.info("[YtdlpDownloadService.download] Failed to send download cancelled status via SseEmitter");
            } catch(IllegalStateException ex) {
                log.info("[YtdlpDownloadService.download] Emitter has already completed");
            }

            downloadProcessHandler.removeProcessById(id);
            emitter.complete();
            resourceHelper.removeResource(id); // Remove any partially downloaded resources
            return;
        }

        downloadProcessHandler.removeProcessById(id); // Remove the process from the map

        // ========PROCESS COMPLETED SUCCESSFULLY========

        // Handle errors
        ErrorCode error = processResult.getError();
        sendSseError(error, emitter);

        // ========DOWNLOAD COMPLETED SUCCESSFULLY========
        log.info("[YtdlpDownloadService.download] Download with ID " + id + " has finished");

        result.setStatus(RequestStatus.SUCCESS.getString());
        result.setMessage("Download has finished");

        resourceHelper.queue(id); // Cleanup resources in set time

        // log.info("[YtdlpDownloadService.download] Download with ID " + id + " has finished");

        try {
            emitter.send(SseEmitter.event() // Send final result
                .name("status")
                .data(result)
            );
        } catch(IOException e) {
            log.info("[YtdlpDownloadService.download] Failed to send initial pending status via SseEmitter");
        } catch(IllegalStateException e) {
            log.info("[YtdlpDownloadService.download] Emitter has already completed");
            return;
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
        boolean isAudioOnly = (type == RequestType.AUDIO_ONLY);

        if((isVideo || isVideoOnly) && !vidFormat.equals("default") && !videoFormat.contains(vidFormat)) {
            log.info("[YtdlpDownloadService.resolveCommandFormat] '" + vidFormat + "' is not available");
            throw new FormatUnavailableException("'" + vidFormat + "' is not available");
        }

        if(isAudioOnly && !audFormat.equals("default") && !audioFormat.contains(audFormat)) {
            log.info("[YtdlpDownloadService.resolveCommandFormat] '" + audFormat + "' is not available");
            throw new FormatUnavailableException("'" + audFormat + "' is not available");
        }

        String videoCommand = "bestvideo";
        String audioCommand = "bestaudio";

        boolean bestVideoQuality = vidQuality.equals("best");
        boolean worstVideoQuality = vidQuality.equals("worst");
        boolean bestAudioQuality = audQuality.equals("best");
        boolean worstAudioQuality = audQuality.equals("worst");
        boolean defaultVideoFormat = vidFormat.equals("default");
        boolean defaultAudioFormat = audFormat.equals("default");

        if(isVideo || isVideoOnly) { // Process video command

            if(bestVideoQuality) {
                videoCommand += "";
            } else if(worstVideoQuality) {
                videoCommand = "worstvideo";
            } else {
                videoCommand += String.format("[height<=%s]", vidQuality);
            }
            
            if(defaultVideoFormat) {
                videoCommand += "[ext=mp4]/" + videoCommand + "[ext=mkv]";
            } else {
                videoCommand += String.format("[ext=%s]", vidFormat);
            }

        } 
        
        if(isVideo || isAudioOnly) { // Process audio command

            if(bestAudioQuality) {
                audioCommand += "";
            } else if(worstAudioQuality) {
                audioCommand = "worstaudio";
            } else {
                audioCommand += String.format("[abr<=%s]", audQuality);
            }

            if(defaultAudioFormat) {
                audioCommand += "[ext=flac]/" + audioCommand + "[ext=m4a]/" + audioCommand + "[ext=mp3]";
            } else {
                audioCommand += String.format("[ext=%s]", audFormat);
            }

        }

        String finalFormat;

        if(isVideo) {
            finalFormat = "(" + videoCommand + ")+(" + audioCommand + ")";
            if(bestVideoQuality) { // Fallbacks
                finalFormat += "/best";
            } else if(worstVideoQuality) {
                finalFormat += "/worst";
            } else {
                finalFormat += "/best" + String.format("[height<=%s]", vidQuality);
            }

            if(!defaultVideoFormat) {
                finalFormat += String.format("[ext=%s]", vidFormat);
            }
        } else if(isVideoOnly) {
            finalFormat = videoCommand;
        } else {
            finalFormat = audioCommand;
        }

        return finalFormat;
    }

    private ErrorCode parseError(String error) {
        log.info("[YtdlpDownloadService.parseError] " + error);

        if(!error.startsWith("ERROR:")) {
            return ErrorCode.NONE;
        }

        if(error.contains("Unsupported URL")) {
            return ErrorCode.UNSUPPORTED_URL;
        }

        if(error.contains("not a valid URL")) {
            return ErrorCode.INVALID_URL;
        }

        if(error.contains("Requested format is not available")) {
            return ErrorCode.FORMAT_UNAVAILABLE;
        }

        if(error.contains("Supported filetypes for thumbnail embedding")) {
            return ErrorCode.POSTPROCESSING_ERROR;
        }

        if(error.contains("[generic]")) {
            return ErrorCode.FAILED_UNEXPECTEDLY;
        }

        return ErrorCode.NONE;
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

    private void sendSseError(ErrorCode error, SseEmitter emitter) {
        log.info("[YtdlpDownloadService.sendSseError] " + error);
        try {
            if(error == ErrorCode.INVALID_URL) {
                log.info("[YtdlpDownloadService.sendSseError] Invalid URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.INVALID_URL.getString(), "The URL provided is not valid"))
                );
            }

            if(error == ErrorCode.UNSUPPORTED_URL) {
                log.info("[YtdlpDownloadService.sendSseError] Unsupported URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.UNSUPPORTED_URL.getString(), "The URL provided is not supported"))
                );
            }

            if(error == ErrorCode.FORMAT_UNAVAILABLE) {
                log.info("[YtdlpDownloadService.sendSseError] Format unavailable");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.FORMAT_UNAVAILABLE.getString(), "The format requested is unavailable"))
                );
            }

            if(error == ErrorCode.POSTPROCESSING_ERROR) {
                log.info("[YtdlpDownloadService.sendSseError] Postprocessing error");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.POSTPROCESSING_ERROR.getString(), "There was a problem in postprocessing"))
                );
            }

            if(error == ErrorCode.FAILED_UNEXPECTEDLY) {
                log.info("[YtdlpDownloadService.sendSseError] Download has failed unexpectedly");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(ErrorCode.FAILED_UNEXPECTEDLY.getString(), "Download has failed unexpectedly"))
                );
            }

            if(error != ErrorCode.NONE) {
                emitter.complete();
            }

        } catch(IOException e) {
            log.info("[YtdlpDownloadService.sendSseError] Failed to send download failed status via SseEmitter");
            emitter.completeWithError(e);
        } catch(IllegalStateException e) {
            log.info("[YtdlpDownloadSe rvice.sendSseError] Emitter has already completed");
        }

    }
    // ---HELPER METHODS---

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

}
