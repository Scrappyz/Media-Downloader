package com.scrappyz.ytdlp.download.common.model;

import com.scrappyz.ytdlp.download.common.util.DownloadConstants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VideoOption {
    
    private String format;
    private String quality;

    public void setFormat(String format) {
        if(DownloadConstants.VIDEO_FORMAT_OPTION.contains(format)) {
            this.format = format;
        } else {
            throw new IllegalArgumentException("Invalid video format: " + format);
        }
    }

    public void setQuality(String quality) {
        if(DownloadConstants.VIDEO_QUALITY_OPTION.contains(quality)) {
            this.quality = quality;
        } else {
            throw new IllegalArgumentException("Invalid video quality: " + quality);
        }
    }

}
