package com.scrappyz.ytdlp.exception.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.scrappyz.ytdlp.dto.ApiError;
import com.scrappyz.ytdlp.exception.custom.ApiException;
import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.exception.custom.FullDownloadQueueException;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.exception.custom.FormatUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(FullDownloadQueueException.class)
    public ResponseEntity<ApiError> handleFullQueueException(FullDownloadQueueException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleMissingResource(ResourceNotFoundException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(DownloadFailedException.class)
    public ResponseEntity<ApiError> handleDownloadFail(DownloadFailedException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }

    @ExceptionHandler(InvalidProcessException.class)
    public ResponseEntity<ApiError> handleDownloadFail(InvalidProcessException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(UnsupportedUrlException.class)
    public ResponseEntity<ApiError> handleDownloadFail(UnsupportedUrlException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ApiError> handleDownloadFail(InvalidUrlException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(FormatUnavailableException.class)
    public ResponseEntity<ApiError> handleDownloadFail(FormatUnavailableException e) {
        ApiError error = new ApiError(e.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
}
