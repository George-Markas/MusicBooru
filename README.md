# MusicBooru
MusicBooru is a web app for streaming music over the network. It uses MPEG-4 AAC (for which transcoding support will
be added).

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
