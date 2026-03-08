# Media Downloader
A full-stack web app for downloading videos or audio from multiple platforms.
Built with React and Spring Boot, it features a mobile-first frontend and a multi-threaded backend to handle concurrent processing.

The backend implements a REST API interface over `yt-dlp`, exposing its functionality via HTTP.
The API is open-source and free to use.

## Table of Contents
- [Quick Demo](#quick-demo)
- [Dependencies](#dependencies)
- [Documentation](#documentation)
  - [API Endpoints](#api-endpoints)

## Quick Demo
[Watch Demo](https://github.com/user-attachments/assets/c486d42a-b81c-4d02-a74c-5bf06ac315c8) \
[Use Web App](https://downloader.micoapp.org/)

## Dependencies

### Required
- **Java 25 (JDK)** — required to run the Spring Boot backend.  
- **Maven** — backend build tool (`pom.xml`).
- **PostgreSQL 18** — required to run backend and database.
- **Node.js (recommended 18+)** — runtime for frontend toolchain.  
  - **pnpm** preferred (used in this project) — but **npm** is also compatible (pnpm is cross-compatible with npm registries).  
- **React 19** — frontend framework used in this project.  
- **yt-dlp** — included in repo or placed at `backend/bin/youtube/yt-dlp`.  
- **ffmpeg** — required for conversions and must be placed at `backend/bin/youtube/ffmpeg` (not included in repo by default).  
- **Git** — for cloning and contributions.

## Documentation

### API Endpoints

#### `/downloads`
**HTTP:** `POST` \
Creates a download request.

##### Request
- `requestType`: The type of media to get. Values are `video`, `video_only`, or `audio_only`.
- `url`: The video url to download.
- `videoFormat`: The video format to download. Values are `mp4`, `mkv`. Nullable if `requestType` is `audio_only`.
- `videoQuality`: The video quality to download. Values are `worst`, `144p`, `240p`, `360p`, `480p`, `720p`, `1080p`, `1440p`, `2160p`, `best`. Nullable if `requestType` is `audio_only`.
- `audioFormat`: The audio format to download. Values are `flac`, `m4a`, `mp3`. Nullable if `requestType` is `video` or `video_only`.
- `audioQuality`: The audio quality to download. Values are `worst`, `128kbps`, `192kbps`, `256kbps`, `320kbps`, `best`. Nullable if `requestType` is `video` or `video_only`.
- `embedMetadata`: If to include video metadata or not (e.g. thumbnails). Useful for music. Values are `true`, `false`.

##### Response
- `requestId`: The unique ID generated for your download request.
- `status`: The current status of your download request.

#### `/downloads/{requestId}/status`
A real-time server-sent event (SSE) of the progress of your download request.

##### Response
- `status`: Event type sent at the start and end of SSE.
  - `status`: The current status of the download requests. Values are `pending`, `ongoing`, `cancelled`, `failed`, or `completed`.
  - `message`: A message associated with the status.
- `progress`: Event type sent during the download.
  - `progress`: The current progress percentage of the download.
  - `message`: A message associated with the event.

#### `/downloads/{requestId}`
**HTTP:** `GET` \
Retrieves the download request associated with the request ID.

##### Response
- `requestType`: The type of media to get. Values are `video`, `video_only`, or `audio_only`.
- `url`: The video url to download.
- `videoFormat`: The video format to download. Values are `mp4`, `mkv`. Nullable if `requestType` is `audio_only`.
- `videoQuality`: The video quality to download. Values are `worst`, `144p`, `240p`, `360p`, `480p`, `720p`, `1080p`, `1440p`, `2160p`, `best`. Nullable if `requestType` is `audio_only`.
- `audioFormat`: The audio format to download. Values are `flac`, `m4a`, `mp3`. Nullable if `requestType` is `video` or `video_only`.
- `audioQuality`: The audio quality to download. Values are `worst`, `128kbps`, `192kbps`, `256kbps`, `320kbps`, `best`. Nullable if `requestType` is `video` or `video_only`.
- `embedMetadata`: If to include video metadata or not (e.g. thumbnails). Useful for music. Values are `true`, `false`.

#### `/downloads/{requestId}/file`
**HTTP:** `GET` \
Retrieves the downloaded file associated with the request ID if finished.

##### Response
A video or audio file.

#### `/downloads/{requestId}`
**HTTP:** `DELETE` \
Cancels an ongoing request via request ID.