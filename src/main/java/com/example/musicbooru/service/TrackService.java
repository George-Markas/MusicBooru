package com.example.musicbooru.service;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class TrackService {

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public void addTrack(MultipartFile file) throws IOException {
        String filePath = saveAudioFile(file);
        Track newTrack = extractMetadata(filePath);

        trackRepository.save(newTrack);
    }

    private Track extractMetadata(String filePath) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(filePath));
            Tag tag = audioFile.getTag();

            return Track.builder()
                    .title(tag.getFirst(FieldKey.TITLE))
                    .album(tag.getFirst(FieldKey.ALBUM))
                    .artist(tag.getFirst(FieldKey.ARTIST))
                    .genre(tag.getFirst(FieldKey.GENRE))
                    .year(tag.getFirst(FieldKey.YEAR))
                    .sampleRate(audioFile.getAudioHeader().getSampleRate())
                    .path(filePath) // Might need tweaking
                    .build();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String saveAudioFile(MultipartFile file) throws IOException {
        String libraryPath = "./tracks/";
        Path path = Paths.get(libraryPath);
        Files.createDirectories(path);

        // TODO: rewrite; derive file name from metadata
        String originalName = file.getOriginalFilename();
        assert originalName != null; // Do not use in production
        Path fullDestinationPath = path.resolve(originalName);
        Files.copy(file.getInputStream(), fullDestinationPath, StandardCopyOption.REPLACE_EXISTING);

        return fullDestinationPath.toString();
    }

    public Optional<Track> getTrackById(Integer id) {
        return trackRepository.findById(id);
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }
}
