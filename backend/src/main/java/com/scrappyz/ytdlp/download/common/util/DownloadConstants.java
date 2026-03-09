package com.scrappyz.ytdlp.download.common.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

public class DownloadConstants {
    public static final SortedSet<Integer> VIDEO_QUALITY = new TreeSet<>(
        Arrays.asList(144, 240, 360, 480, 720, 1080, 2140)
    );

    public static final SortedSet<Integer> AUDIO_QUALITY = new TreeSet<>(
        Arrays.asList(128, 192, 256, 320)
    );

    public static final HashSet<String> VIDEO_FORMAT = new HashSet<>(
        Arrays.asList("mp4", "mkv")
    );

    public static final HashSet<String> AUDIO_FORMAT = new HashSet<>(
        Arrays.asList("flac", "m4a", "mp3")
    );
}