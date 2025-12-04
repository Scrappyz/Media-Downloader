package com.scrappyz.ytdlp.helper;

import java.util.concurrent.ExecutorService;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class YtdlpDownloadProcess {
    
    private Process process;
    private ExecutorService executorService;

}
