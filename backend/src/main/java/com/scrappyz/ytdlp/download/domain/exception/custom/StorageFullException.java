package com.scrappyz.ytdlp.download.domain.exception.custom;

public class StorageFullException extends ApiException {

    public StorageFullException() {
        super("STORAGE_FULL", "Storage is full. Please try again later.");
    }

    public StorageFullException(String message) {
        super("STORAGE_FULL", message);
    }
    
}
