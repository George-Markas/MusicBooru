package com.example.musicbooru.service;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.JaudiotaggerWrapper;
import org.jaudiotagger.tag.FieldKey;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

import static com.example.musicbooru.util.Commons.*;

@Service
public class TrackService {

    private final Logger logger = LoggerFactory.getLogger(TrackService.class);

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }

    public Optional<Track> getTrackById(String id) {
        return trackRepository.findById(id);
    }

    public void uploadTrack(MultipartFile file) {
        try {
            // Create directories for the audio files and accompanying artwork
            Files.createDirectories(Path.of(LIBRARY));
            Files.createDirectories(Path.of(ARTWORK));
        } catch(IOException e) {
            logger.error("Could not create directory", e);
            throw new GenericException("Could not create directory");
        }

        try {
            // Save song as temporary file for metadata extraction
            Path tmp = Files.createTempFile(null, AUDIO_EXTENSION);
            Files.copy(file.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);

            // TODO the wrapper for Jaudiotagger might need a rewrite to make it more "elegant"
            // Construct file name from metadata
            JaudiotaggerWrapper jwrap = new JaudiotaggerWrapper(tmp.toFile());
            final String fileName = jwrap.constructFileName(AUDIO_EXTENSION);

            // Make database entry
            Track track = Track.builder()
                    .title(jwrap.getTag().getFirst(FieldKey.TITLE))
                    .artist(jwrap.getTag().getFirst(FieldKey.ARTIST))
                    .album(jwrap.getTag().getFirst(FieldKey.ALBUM))
                    .genre(jwrap.getTag().getFirst(FieldKey.GENRE))
                    .year(jwrap.getTag().getFirst(FieldKey.YEAR))
                    .fileName(fileName)
                    .hasArtwork(false)
                    .build();
            trackRepository.save(track);

            // Extract cover art
            if(jwrap.extractArtwork(track.getId())) {
                track.setHasArtwork(true);
                trackRepository.save(track);
            }

            // Move song to the library directory
            final Path target = Paths.get(LIBRARY + fileName);
            if(Files.exists(target)) {
                logger.warn("File \"{}\" already exists and will be overwritten", fileName);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Uploaded track with ID {}", track.getId());
        } catch(IOException e) {
            logger.error("An unexpected error occurred", e);
            throw new GenericException("An unexpected error occurred");
        }
    }

    public void deleteTrack(String id) {
        Track track = trackRepository.findById(id).orElseThrow(() -> {
            logger.error("Could not find track with ID {}", id);
            return new ResourceNotFoundException("Could not find track with ID " + id);
        });

        try {
            Files.delete(Paths.get(LIBRARY + track.getFileName()));
            logger.info("Deleted audio file for track with ID {}", id);
        } catch(IOException e) {
            logger.error("Could not delete audio file for track with ID {}", id, e);
            throw new GenericException("Could not delete audio file for track with ID " + id);
        }

        try {
            Files.delete(Paths.get(ARTWORK + track.getId() + ARTWORK_EXTENSION));
            logger.info("Deleted artwork for track with ID {}", id);
        } catch(IOException e) {
            logger.error("Could not delete artwork for track with with ID {}; ", id, e);
            throw new GenericException("Could not delete artwork for track with ID " + id);
        }

        trackRepository.delete(track);
        logger.info("Deleted database entry for track with ID {}", id);
    }
}
