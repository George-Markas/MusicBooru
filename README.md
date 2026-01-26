# MusicBooru
MusicBooru is a web app for streaming music over the network. For now, only MPEG-4 AAC is supported
(transcoding to that format might be implemented).

**This project is still very much work in progress.** It was initially conceived for the "Special Topics
in Software Engineering" course.

## Build and run
MusicBooru has the following dependencies:
- Java 21
- Docker

```sh
git clone https://github.com/George-Markas/MusicBooru.git
cd MusicBooru
docker-compose build 
docker-compose up # -d to run in the background

# Stop
docker-compose down
```
