package com.scrappyz.ytdlp.download.domain.enums;

import java.util.HashMap;

public enum DownloadErrorCode {
    NONE("NONE"),
    UNSUPPORTED_URL("UNSUPPORTED_URL"),
    INVALID_URL("INVALID_URL"),
    FORMAT_UNAVAILABLE("FORMAT_UNAVAILABLE"),
    POSTPROCESSING_ERROR("POSTPROCESSING_ERROR"),
    FAILED_UNEXPECTEDLY("FAILED_UNEXPECTEDLY");

    private final String string;
    private static final HashMap<String, DownloadErrorCode> byString = new HashMap<>();

    static {
        for(DownloadErrorCode t: values()) {
            byString.put(t.string, t);
        }
    }

    private DownloadErrorCode(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public static DownloadErrorCode getErrorCode(String str) {
        return byString.get(str);
    }
};