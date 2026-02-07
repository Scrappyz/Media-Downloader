package com.scrappyz.ytdlp.download.infrastructure.model;

public enum AudioFormat {
    DEFAULT("default"),
    MP3("mp3"),
    M4A("m4a"),
    FLAC("flac");

    private final String value;

    AudioFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AudioFormat fromValue(String value) {
        for(AudioFormat format : AudioFormat.values()) {
            if(format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown AudioFormat value: " + value);
    }
}
