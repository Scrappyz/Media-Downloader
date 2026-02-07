package com.scrappyz.ytdlp.download.infrastructure.model;

public enum AudioQuality {
    WORST("worst"),
    KBPS128("128kbps"),
    KBPS192("192kbps"),
    KBPS256("256kbps"),
    KBPS320("320kbps"),
    BEST("best");

    private final String value;

    AudioQuality(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AudioQuality fromValue(String value) {
        for(AudioQuality quality : AudioQuality.values()) {
            if(quality.value.equalsIgnoreCase(value)) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Unknown AudioQuality value: " + value);
    }

}
