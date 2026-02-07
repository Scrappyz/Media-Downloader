package com.scrappyz.ytdlp.download.infrastructure.model;

public enum VideoFormat {
    DEFAULT("default"),
    MP4("mp4"),
    MKV("mkv");

    private final String value;

    VideoFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static VideoFormat fromValue(String value) {
        for(VideoFormat format : VideoFormat.values()) {
            if(format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown VideoFormat value: " + value);
    }
}
