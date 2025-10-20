# Media Downloader
A full-stack web app for downloading videos or audio from multiple platforms.
Built with React and Spring Boot, it features a mobile-first frontend and a multi-threaded backend to handle concurrent processing.

The backend implements a REST API interface over `yt-dlp`, exposing its functionality via HTTP.
The API is open-source and free to use.

## Table of Contents
- [Quick Demo](#quick-demo)
- [Dependencies](#dependencies)
  - [Frontend](#frontend)
  - [Backend](#backend)
- [Usage](#usage)
  - [Running the Application](#running-the-application)

## Quick Demo
[Watch Demo](https://github.com/user-attachments/assets/c486d42a-b81c-4d02-a74c-5bf06ac315c8) \
[Use Web App](https://downloader.micoapp.org/)

## Dependencies

### Required
- **Java 21 (JDK)** — required to run the Spring Boot backend.  
- **Maven** — backend build tool (`pom.xml`).  
- **Node.js (recommended 18+)** — runtime for frontend toolchain.  
  - **pnpm** preferred (used in this project) — but **npm** is also compatible (pnpm is cross-compatible with npm registries).  
- **React 19** — frontend framework used in this project.  
- **yt-dlp** — included in repo or placed at `backend/bin/youtube/yt-dlp`.  
- **ffmpeg** — required for conversions and must be placed at `backend/bin/youtube/ffmpeg` (not included in repo by default).  
- **Git** — for cloning and contributions.

## Usage
### Running the Application
1. Go to your terminal
2. Clone the application using `git clone`
3. Go to `backend` and run `mvn spring-boot:run`
4. Go to `frontend` and run `npm run dev`