package com.scrappyz.ytdlp.download.domain.exception.custom;

public class StorageFullException extends ApiException {

    public StorageFullException() {
        super("storage_full", "Storage is full. Please try again later.");
    }

    public StorageFullException(String message) {
        super("storage_full", message);
    }
    
}
