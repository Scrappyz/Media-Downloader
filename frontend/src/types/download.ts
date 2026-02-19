export interface DownloadResponse {
    requestId: string,
    status: DownloadStatus
}

export interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: string,
  videoFormat?: string,
  audioQuality?: string,
  audioFormat?: string,
  embedMetadata: boolean,
  outputName?: string
};

export interface StatusResponse {
  status: string,
  message: string | null
};

export interface ApiError {
  code: string,
  message: string
}

export type DownloadStatus = 'pending' | 'ongoing' | 'failed' | 'completed' | 'cancelled' | null;