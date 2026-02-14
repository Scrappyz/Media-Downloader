package com.scrappyz.ytdlp.download.infrastructure.model;

public enum RequestType {
    VIDEO("video"),
    VIDEO_ONLY("video_only"),
    AUDIO_ONLY("audio_only");

    private final String value;

    RequestType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RequestType fromValue(String value) {
        for(RequestType type : RequestType.values()) {
            if(type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RequestType value: " + value);
    }
}
