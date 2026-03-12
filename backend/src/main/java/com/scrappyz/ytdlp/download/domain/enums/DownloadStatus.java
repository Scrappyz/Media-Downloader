package com.scrappyz.ytdlp.download.domain.enums;

public enum DownloadStatus {
    PENDING("PENDING"),
    ONGOING("ONGOING"),
    CANCELLED("CANCELLED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    DownloadStatus(String value) {
        this.value = value;
    }

    public String getString() {
        return value;
    }

    public static DownloadStatus getValue(String value) {
        for(DownloadStatus type : DownloadStatus.values()) {
            if(type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DownloadStatus value: " + value);
    }
}
