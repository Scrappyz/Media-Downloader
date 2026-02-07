package com.scrappyz.ytdlp.download.infrastructure.model;

public enum RequestStatus {
    PENDING("pending"),
    ONGOING("ongoing"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    RequestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RequestStatus fromValue(String value) {
        for(RequestStatus status : RequestStatus.values()) {
            if(status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RequestStatus value: " + value);
    }
}
