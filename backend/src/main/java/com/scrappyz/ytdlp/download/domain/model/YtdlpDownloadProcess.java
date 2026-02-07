package com.scrappyz.ytdlp.download.domain.model;

import java.util.concurrent.ExecutorService;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class YtdlpDownloadProcess {
    
    private Process process;
    private ExecutorService executorService;
    private boolean running;
    private boolean cancelled;
    private boolean readable = true; // Prevents reading from stream

    public YtdlpDownloadProcess(Process process, ExecutorService executorService) {
        this.process = process;
        this.executorService = executorService;
    }

}
