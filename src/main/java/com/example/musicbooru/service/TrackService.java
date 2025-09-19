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

    public void saveTrack(MultipartFile file) throws IOException {
        String savedFilePath = saveAudioFile(file);
        Track track = extractMetadata(savedFilePath);

        trackRepository.save(track);
    }

    private Track extractMetadata(String savedFilePath) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(savedFilePath));
            Tag tag = audioFile.getTag();

            return Track.builder()
                    .title(tag.getFirst(FieldKey.TITLE))
                    .album(tag.getFirst(FieldKey.ALBUM))
                    .artist(tag.getFirst(FieldKey.ARTIST))
                    .genre(tag.getFirst(FieldKey.GENRE))
                    .path(savedFilePath)
                    .build();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String saveAudioFile(MultipartFile file) throws IOException {
        String filePath1 = "./tracks/";
        Path path = Paths.get(filePath1);
        Files.createDirectories(path);

        // TODO: derive file name from metadata
        String originalName = file.getOriginalFilename();
        assert originalName != null;
        Path filePath = path.resolve(originalName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    public Optional<Track> getTrackById(Integer id) {
        return trackRepository.findById(id);
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }

}
