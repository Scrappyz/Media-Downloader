package com.scrappyz.ytdlp.download.domain.model;

import java.util.HashMap;

public enum DownloadErrorCode {
    NONE("none"),
    UNSUPPORTED_URL("unsupported_url"),
    INVALID_URL("invalid_url"),
    FORMAT_UNAVAILABLE("format_unavailable"),
    POSTPROCESSING_ERROR("postprocessing_error"),
    FAILED_UNEXPECTEDLY("failed_unexpectedly");

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