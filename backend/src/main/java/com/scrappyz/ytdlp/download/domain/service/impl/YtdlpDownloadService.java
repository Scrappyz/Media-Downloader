package com.scrappyz.ytdlp.download.domain.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.scrappyz.ytdlp.config.properties.DownloadProperties;
import com.scrappyz.ytdlp.config.properties.PathProperties;
import com.scrappyz.ytdlp.config.properties.YtdlpProperties;
import com.scrappyz.ytdlp.download.api.dto.ApiError;
import com.scrappyz.ytdlp.download.api.dto.DownloadRequest;
import com.scrappyz.ytdlp.download.api.dto.DownloadResponse;
import com.scrappyz.ytdlp.download.api.dto.DownloadResult;
import com.scrappyz.ytdlp.download.domain.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.download.domain.exception.custom.ExpiredResourceException;
import com.scrappyz.ytdlp.download.domain.exception.custom.FailedProcessException;
import com.scrappyz.ytdlp.download.domain.exception.custom.FormatUnavailableException;
import com.scrappyz.ytdlp.download.domain.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.download.domain.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.download.domain.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.download.domain.model.DownloadErrorCode;
import com.scrappyz.ytdlp.download.domain.model.YtdlpProcessResult;
import com.scrappyz.ytdlp.download.domain.service.DownloadRepositoryService;
import com.scrappyz.ytdlp.download.domain.service.DownloadService;
import com.scrappyz.ytdlp.download.domain.service.DownloadSseService;
import com.scrappyz.ytdlp.download.domain.service.helper.DownloadProgressHelper;
import com.scrappyz.ytdlp.download.domain.service.helper.DownloadResourceHelper;
import com.scrappyz.ytdlp.download.domain.service.helper.YtdlpDownloadProcessHandler;
import com.scrappyz.ytdlp.download.infrastructure.entity.Request;
import com.scrappyz.ytdlp.download.infrastructure.entity.RequestDetail;
import com.scrappyz.ytdlp.download.infrastructure.entity.Resource;
import com.scrappyz.ytdlp.download.infrastructure.model.RequestType;
import com.scrappyz.ytdlp.download.infrastructure.repository.RequestRepository;
import com.scrappyz.ytdlp.download.infrastructure.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("ytdlp")
@Primary
@Slf4j
@RequiredArgsConstructor
public class YtdlpDownloadService implements DownloadService {

    private final ObjectProvider<DownloadProgressHelper> progressHelperProvider;
    
    private final PathProperties paths;
    private final YtdlpProperties ytdlpProperties;
    private final DownloadProperties downloadProperties;

    @Qualifier("downloadExecutor")
    private final ExecutorService downloadExecutor;

    private final RequestRepository requestRepository;
    private final ResourceRepository resourceRepository;

    private final DownloadRepositoryService downloadRepositoryService;
    private final DownloadSseService sseService;

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

    

    private Request createRequestEntity(String id, String status, DownloadRequest request) {
        Request req = new Request();
        req.setId(id);
        req.setStatus(status);
        req.setCreatedAt(java.time.Instant.now());
        
        RequestDetail detail = new RequestDetail();
        detail.setRequestId(id);
        detail.setRequest(req);
        detail.setUrl(request.getUrl());
        detail.setRequestType(request.getRequestType());
        detail.setVideoQuality(request.getVideoQuality());
        detail.setVideoFormat(request.getVideoFormat());
        detail.setAudioQuality(request.getAudioQuality());
        detail.setAudioFormat(request.getAudioFormat());
        detail.setMetadata(request.isEmbedMetadata());

        req.setRequestDetail(detail);

        return req;
    }

    private void cleanDownloadRequest(DownloadRequest request) {
        if(request.getVideoQuality() != null) {
            request.setVideoQuality(request.getVideoQuality().trim().toLowerCase());
        }

        if(request.getAudioQuality() != null) {
            request.setAudioQuality(request.getAudioQuality().trim().toLowerCase());
        }

        if(request.getVideoFormat() != null) {
            request.setVideoFormat(request.getVideoFormat().trim().toLowerCase());
        }

        if(request.getAudioFormat() != null) {
            request.setAudioFormat(request.getAudioFormat().trim().toLowerCase());
        }
    }

    // Queue the download request
    @Override
    public DownloadResponse enqueue(DownloadRequest request) {
        boolean isPreviousRequest = Ulid.isValid(request.getUrl()); // If a request ID is provided in the URL field

        cleanDownloadRequest(request); // Normalize values for easier processing later on
        
        DownloadResponse result = new DownloadResponse();
        String id = isPreviousRequest ? request.getUrl() : UlidCreator.getMonotonicUlid().toString();
        String status = "pending";

        if(isPreviousRequest) {
            log.info("[YtdlpDownloadService.enqueue] Previous request with ID " + id + " is being re-queued");

            // If the request is already completed, return the same request with the same ID and status
            Request existingReq = requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No request found with ID: " + id));
            RequestDetail detail = existingReq.getRequestDetail();

            request.setUrl(detail.getUrl());
            request.setRequestType(detail.getRequestType());
            request.setVideoQuality(detail.getVideoQuality());
            request.setVideoFormat(detail.getVideoFormat());
            request.setAudioQuality(detail.getAudioQuality());
            request.setAudioFormat(detail.getAudioFormat());
            request.setEmbedMetadata(detail.isMetadata());

            status = existingReq.getStatus();

        } else {
            // Set as 'pending' in the database
            Request req = createRequestEntity(id, "pending", request);
            downloadRepositoryService.addNewRequest(req);
        }

        if(status.equals("ongoing")) {
            sseService.addEmitter(id, downloadProperties.getTimeout().toMillis());
        } else if(!status.equals("completed")) { // If the request is not already completed or ongoing, start the download process
            sseService.addEmitter(id, downloadProperties.getTimeout().toMillis());
            downloadExecutor.submit(() -> download(id, request));
        }

        result.setRequestId(id);
        result.setStatus(status);
        log.info("[YtdlpDownloadService.enqueue] Total Emitters: " + sseService.getTotalEmitters());

        return result;
    }

    @Override
    public DownloadRequest getDownloadRequest(String id) {
        Request req = requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No request found with ID: " + id));
        RequestDetail detail = req.getRequestDetail();

        DownloadRequest downloadRequest = new DownloadRequest();
        downloadRequest.setUrl(detail.getUrl());
        downloadRequest.setRequestType(detail.getRequestType());
        downloadRequest.setVideoQuality(detail.getVideoQuality());
        downloadRequest.setVideoFormat(detail.getVideoFormat());
        downloadRequest.setAudioQuality(detail.getAudioQuality());
        downloadRequest.setAudioFormat(detail.getAudioFormat());
        downloadRequest.setEmbedMetadata(detail.isMetadata());

        return downloadRequest;
    }

    @Override
    public void cancelDownload(String id) {
        downloadProcessHandler.cancelProcessById(id);
    }

    @Override
    public FileSystemResource getResource(String id) throws ResourceNotFoundException {
        Resource r = resourceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Resource not found with request ID: " + id));
        
        if(r.getExpireAt() != null && r.getExpireAt().isBefore(Instant.now())) {
            throw new ExpiredResourceException("Resource with request ID " + id + " has expired");
        }

        FileSystemResource resource = new FileSystemResource(resourceHelper.getFile(id));
        downloadRepositoryService.incrementFetchCountByRequestId(id);
        
        return resource;
    }

    private void download(String id, DownloadRequest request) 
        throws InvalidUrlException, UnsupportedUrlException, FormatUnavailableException, DownloadFailedException, FailedProcessException {

        DownloadResult result = new DownloadResult("pending", "Download is pending");

        sseService.sendStatus(id, result.getStatus(), result.getMessage());

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

        RequestType t = RequestType.fromValue(type);

        String format = resolveCommandFormat(t, site, vidFormat, vidQuality, audQuality, audFormat);
        log.info("[YtdlpDownloadService.download] Command Format: " + format);

        Path outputPath = paths.getDownloadPath().resolve(id).normalize();

        // Compile commands
        List<String> commands = new ArrayList<>();
        commands.add(paths.getYtdlpBin().toString());
        if(ytdlpProperties.isUseCookies()) {
            log.info("[YtdlpDownloadService.download] Cookies Path: " + ytdlpProperties.getCookiesPath());
            if(ytdlpProperties.getCookiesPath() == null || ytdlpProperties.getCookiesPath().isEmpty()) { // Use browser
                commands.addAll(Arrays.asList("--cookies-from-browser", ytdlpProperties.getBrowserCookies()));
            } else {
                commands.addAll(Arrays.asList("--cookies", ytdlpProperties.getCookiesPath()));
            }

            commands.addAll(Arrays.asList("--js-runtime", ytdlpProperties.getJsRuntime()));
        }
        if(request.isEmbedMetadata()) {
            commands.addAll(Arrays.asList("--embed-metadata", "--embed-thumbnail", "--convert-thumbnails", "jpg"));
        }
        commands.addAll(Arrays.asList("-f", format));
        commands.addAll(Arrays.asList(url, "-P", outputPath.toString()));
        commands.addAll(Arrays.asList("-o", id + ".%(ext)s", "--no-warnings", "--newline"));

        log.info("[YtdlpDownloadService.download] Download Commands: " + String.join(" ", commands));

        // Set request as 'ongoing' in database
        try {
            downloadRepositoryService.updateRequestStatusById(id, "ongoing");
        } catch(Exception e) {
            log.info("[YtdlpDownloadService.download] Failed to update request status to ongoing for request ID " + id);
        }

        // Run the download process
        YtdlpProcessResult processResult = new YtdlpProcessResult();
        try {
            DownloadProgressHelper progressHelper = progressHelperProvider.getObject();
            processResult = downloadProcessHandler.runProcess(
                commands,
                id,
                (line, requestId) -> {
                    return progressHelper.processLine(line, requestId); // Or handle the output line as needed
                },
                (line) -> {
                    return parseError(line); // Or handle the error line as needed
                }
            );
        } catch(DownloadFailedException e) { // If something goes wrong with the download process
            log.info("[YtdlpDownloadService.download] Remove process with ID " + id + " because of error");
            downloadProcessHandler.removeProcessById(id);
            sseService.completeEmitter(id);
            sseService.removeEmitter(id);
            downloadRepositoryService.updateRequestStatusById(id, "failed");
            throw new DownloadFailedException();
        }

        if(downloadProcessHandler.isProcessCancelled(id)) {
            log.info("[YtdlpDownloadService.download] Download with ID " + id + " was cancelled");
            result.setStatus("cancelled");
            result.setMessage("Download was cancelled");

            sseService.sendStatus(id, result.getStatus(), result.getMessage());

            downloadProcessHandler.removeProcessById(id);

            // Set request to 'cancelled' in database
            downloadRepositoryService.updateRequestStatusById(id, "cancelled");

            sseService.completeEmitter(id);
            sseService.removeEmitter(id);
            resourceHelper.removeResource(id); // Remove any partially downloaded resources
            return;
        }

        downloadProcessHandler.removeProcessById(id); // Remove the process from the map

        // ========PROCESS COMPLETED SUCCESSFULLY========

        // Handle errors
        DownloadErrorCode error = processResult.getError();

        if(error != DownloadErrorCode.NONE) {
            try {
                sseService.sendError(id, error);   
            } finally {
                sseService.completeEmitter(id);
                sseService.removeEmitter(id);
            }
            return;
        }

        // ========DOWNLOAD COMPLETED SUCCESSFULLY========
        log.info("[YtdlpDownloadService.download] Download with ID " + id + " has finished");

        result.setStatus("completed");
        result.setMessage("Download has finished");

        log.info("[YtdlpDownloadService.download] Download with ID " + id + " has finished");

        Instant createdAt = Instant.now();
        Instant expireAt = createdAt.plus(resourceHelper.getResourceExpiryTime());
        long storageUsed = resourceHelper.getFileSize(id);

        // Set request to 'completed' in database and add resource details
        try {
            downloadRepositoryService.completeRequestById(id);
            downloadRepositoryService.addNewResource(
                id, 
                createdAt,
                expireAt, 
                storageUsed
            );
        } catch(Exception e) {
            e.printStackTrace();
        }

        resourceHelper.queue(id, expireAt, storageUsed);

        sseService.sendStatus(id, result.getStatus(), result.getMessage());
        sseService.completeEmitter(id);
        sseService.removeEmitter(id);
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

        vidQuality = vidQuality.replaceAll("[^0-9]", ""); // Remove non-numeric characters

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

        audQuality = audQuality.replaceAll("[^0-9]", ""); // Remove non-numeric characters

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
        
        boolean isVideo = (type == RequestType.VIDEO || type == RequestType.VIDEO_ONLY);
        boolean isVideoOnly = type == RequestType.VIDEO_ONLY;
        boolean isAudioOnly = type == RequestType.AUDIO_ONLY;

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

    private DownloadErrorCode parseError(String error) {
        log.info("[YtdlpDownloadService.parseError] " + error);

        if(!error.startsWith("ERROR:")) {
            return DownloadErrorCode.NONE;
        }

        if(error.contains("Unsupported URL")) {
            return DownloadErrorCode.UNSUPPORTED_URL;
        }

        if(error.contains("not a valid URL")) {
            return DownloadErrorCode.INVALID_URL;
        }

        if(error.contains("Requested format is not available")) {
            return DownloadErrorCode.FORMAT_UNAVAILABLE;
        }

        if(error.contains("Supported filetypes for thumbnail embedding")) {
            return DownloadErrorCode.POSTPROCESSING_ERROR;
        }

        if(error.contains("[generic]")) {
            return DownloadErrorCode.FAILED_UNEXPECTEDLY;
        }

        return DownloadErrorCode.NONE;
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

    private void sendSseError(DownloadErrorCode error, SseEmitter emitter) {
        log.info("[YtdlpDownloadService.sendSseError] " + error);
        try {
            if(error == DownloadErrorCode.INVALID_URL) {
                log.info("[YtdlpDownloadService.sendSseError] Invalid URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.INVALID_URL.getString(), "The URL provided is not valid"))
                );
            }

            if(error == DownloadErrorCode.UNSUPPORTED_URL) {
                log.info("[YtdlpDownloadService.sendSseError] Unsupported URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.UNSUPPORTED_URL.getString(), "The URL provided is not supported"))
                );
            }

            if(error == DownloadErrorCode.FORMAT_UNAVAILABLE) {
                log.info("[YtdlpDownloadService.sendSseError] Format unavailable");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.FORMAT_UNAVAILABLE.getString(), "The format requested is unavailable"))
                );
            }

            if(error == DownloadErrorCode.POSTPROCESSING_ERROR) {
                log.info("[YtdlpDownloadService.sendSseError] Postprocessing error");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.POSTPROCESSING_ERROR.getString(), "There was a problem in postprocessing"))
                );
            }

            if(error == DownloadErrorCode.FAILED_UNEXPECTEDLY) {
                log.info("[YtdlpDownloadService.sendSseError] Download has failed unexpectedly");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.FAILED_UNEXPECTEDLY.getString(), "Download has failed unexpectedly"))
                );
            }

            if(error != DownloadErrorCode.NONE) {
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

}
