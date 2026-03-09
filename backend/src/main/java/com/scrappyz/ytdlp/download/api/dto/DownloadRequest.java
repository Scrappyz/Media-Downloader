package com.scrappyz.ytdlp.download.api.dto;

import com.scrappyz.ytdlp.download.common.model.AudioOption;
import com.scrappyz.ytdlp.download.common.model.VideoOption;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadRequest {
    
    private String requestType;
    private String url;
    private VideoOption video;
    private AudioOption audio;
    private boolean embedMetadata;

    public String getVideoFormat() {
        if(video != null) {
            return video.getFormat();
        }
        return null;
    }

    public String getVideoQuality() {
        if(video != null) {
            return video.getQuality();
        }
        return null;
    }

    public String getAudioFormat() {
        if(audio != null) {
            return audio.getFormat();
        }
        return null;
    }

    public String getAudioQuality() {
        if(audio != null) {
            return audio.getQuality();
        }
        return null;
    }

    public void setVideoFormat(String format) {
        if(video == null) {
            video = new VideoOption();
        }
        video.setFormat(format);
    }

    public void setVideoQuality(String quality) {
        if(video == null) {
            video = new VideoOption();
        }
        video.setQuality(quality);
    }

    public void setAudioFormat(String format) {
        if(audio == null) {
            audio = new AudioOption();
        }
        audio.setFormat(format);
    }

    public void setAudioQuality(String quality) {
        if(audio == null) {
            audio = new AudioOption();
        }
        audio.setQuality(quality);
    }

}
