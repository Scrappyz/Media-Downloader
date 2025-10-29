package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadChunkRequest {
  private String id;
  private String mediaId;
  private int chunkSize;
  private String message;
}
