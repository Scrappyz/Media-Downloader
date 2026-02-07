package com.scrappyz.ytdlp.download.infrastructure.model;

public enum VideoQuality {
    WORST("worst"),
    P144("144p"),
    P240("240p"),
    P360("360p"),
    P480("480p"),
    P720("720p"),
    P1080("1080p"),
    P1440("1440p"),
    P2160("2160p"),
    BEST("best");

    private final String value;

    VideoQuality(String value) {
        this.value = value;
    }   

    public String getValue() {
        return value;
    }

    public static VideoQuality fromValue(String value) {
        for(VideoQuality quality : VideoQuality.values()) {
            if(quality.value.equalsIgnoreCase(value)) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Unknown VideoQuality value: " + value);
    }
}
