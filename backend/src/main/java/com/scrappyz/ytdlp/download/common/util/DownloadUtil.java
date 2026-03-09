package com.scrappyz.ytdlp.download.common.util;

import java.util.Iterator;

public class DownloadUtil {
    
    public static String resolveVideoQuality(String vidQuality) {
        if(vidQuality == null || vidQuality.isEmpty()) {
            return "best";
        }

        vidQuality = vidQuality.trim().toLowerCase();

        if(vidQuality.equals("best")) {
            return "best";
        }

        if(vidQuality.equals("worst")) {
            return "worst";
        }

        vidQuality = vidQuality.replaceAll("[^0-9]", ""); // Remove non-numeric characters

        int numericQuality = Integer.parseInt(vidQuality);
        vidQuality = String.valueOf(numericQuality);

        Iterator<Integer> iterator = DownloadConstants.VIDEO_QUALITY.iterator();
        int firstValue = iterator.next();

        if(numericQuality < firstValue) return String.valueOf(firstValue);

        if(DownloadConstants.VIDEO_QUALITY.contains(numericQuality)) return vidQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : DownloadConstants.VIDEO_QUALITY) {
            if(i == firstValue) {
                prev = i;
                continue;
            }

            if(numericQuality > prev && numericQuality < i) {
                numericQuality = prev;
                break;
            }

            prev = i;
        }

        return String.valueOf(numericQuality);
    }

    public static String resolveAudioQuality(String audQuality) {
        if(audQuality == null || audQuality.isEmpty()) {
            return "best";
        }

        audQuality = audQuality.trim().toLowerCase();

        if(audQuality.equals("best")) {
            return "best";
        }

        if(audQuality.equals("worst")) {
            return "worst";
        }

        audQuality = audQuality.replaceAll("[^0-9]", ""); // Remove non-numeric characters

        int numericQuality = Integer.parseInt(audQuality);
        audQuality = String.valueOf(numericQuality);

        Iterator<Integer> iterator = DownloadConstants.AUDIO_QUALITY.iterator();
        int firstValue = iterator.next();

        if(numericQuality < firstValue) return String.valueOf(firstValue);

        if(DownloadConstants.AUDIO_QUALITY.contains(numericQuality)) return audQuality;

        // Get the nearest video quality
        int prev = -1;
        for(int i : DownloadConstants.AUDIO_QUALITY) {
            if(i == firstValue) {
                prev = i;
                continue;
            }

            if(numericQuality > prev && numericQuality < i) {
                numericQuality = prev;
                break;
            }

            prev = i;
        }

        return String.valueOf(numericQuality);
    }

    public static String resolveVideoFormat(String videoFormat) {
        if(videoFormat == null || videoFormat.isEmpty() || videoFormat.equals("Default")) {
            return "default";
        }

        return videoFormat;
    }

    public static String resolveAudioFormat(String audioFormat) {
        if(audioFormat == null || audioFormat.isEmpty() || audioFormat.equals("Default")) {
            return "default";
        }

        return audioFormat;
    }

}
