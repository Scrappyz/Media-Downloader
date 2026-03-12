package com.scrappyz.ytdlp.download.domain.enums;

public enum RequestType {
    VIDEO("VIDEO"),
    VIDEO_ONLY("VIDEO_ONLY"),
    AUDIO_ONLY("AUDIO_ONLY");

    private final String value;

    RequestType(String value) {
        this.value = value;
    }

    public String getString() {
        return value;
    }

    public static RequestType getValue(String value) {
        for(RequestType type : RequestType.values()) {
            if(type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RequestType value: " + value);
    }
}
