# VDownloader

## Dependencies
### Frontend
- npm
- React 19

### Backend
- Java 21
- Maven
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) (included in repo)

## Usage
### Setting Up
1. Go into `frontend/src/globals.tsx`.
2. You can either use `localhost` for the address or your LANs IPV4 address. To expose the website to other devices on your network, use your IPV4 address. It is a good idea to setup a static IP. To use the web app just in your local machine, use `localhost`.

### Starting the Application
1. Clone the repository with `git clone`.
2. Go into `frontend` then run `npm install` (if first time) then `npm run dev --host`. The frontend will run on port `5000`.
3. Go into `backend` then run `mvn spring-boot:run "-Dspring-boot.run.profiles=default"`.

### Using the Frontend
Use the frontend by going to `http://localhost:5000`. Use `http://<ipv4>:5000` for other devices in your network.