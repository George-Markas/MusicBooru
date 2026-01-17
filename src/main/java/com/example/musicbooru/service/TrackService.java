package com.example.musicbooru.service;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.MetadataUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.*;

@Service
public class TrackService {

    private final static Logger logger = LoggerFactory.getLogger(TrackService.class);

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public boolean trackExists(String id) {
        return trackRepository.existsById(UUID.fromString(id));
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }

    public List<Track> getTracks(String field) {
        return trackRepository.findAll(Sort.by(Sort.Direction.ASC, field));
    }

    public Optional<Track> getTrackById(String id) {
        return trackRepository.findById(UUID.fromString(id));
    }

    public void uploadTrack(MultipartFile file) {
        try {
            // Create directories for the audio files and accompanying artwork
            Files.createDirectories(Path.of(LIBRARY));
            Files.createDirectories(Path.of(ARTWORK));
        } catch (IOException e) {
            logger.error("Could not create directory", e);
            throw new GenericException("Could not create directory");
        }

        try {
            // Save song as temporary file for metadata extraction
            Path temp = Files.createTempFile(null, AUDIO_EXTENSION);
            Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);

            // Generate filename from metadata
            MetadataUtils metadataUtils = new MetadataUtils(temp.toFile());
            String fileName = metadataUtils.generateFileName();
            if (fileName != null && trackRepository.existsByFileName(fileName)) {
                logger.warn("Track with filename \"{}\" already exists; using UUID for filename", fileName);
                fileName = null;
            }

            // Create database entry
            Track track = Track.builder()
                    .title(metadataUtils.getTitle())
                    .artist(metadataUtils.getArtist())
                    .album(metadataUtils.getAlbum())
                    .genre(metadataUtils.getGenre())
                    .year(metadataUtils.getYear())
                    .fileName(fileName)
                    .build();
            trackRepository.save(track);

            // Extract cover art
            metadataUtils.extractArtwork(String.valueOf(track.getId()));

            // Move song to the library directory
            Path target = Paths.get(LIBRARY + track.getFileName());
            if (Files.exists(target)) {
                logger.warn("File \"{}\" already exists and will be overwritten", track.getFileName());
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Added track '{}'", track.getId());
        } catch (IOException e) {
            logger.error("An unexpected error occurred", e);
            throw new GenericException("An unexpected error occurred");
        }
    }

    public void deleteTrack(String id) {
        Track track = trackRepository.findById(UUID.fromString(id)).orElseThrow(() -> {
            logger.error("Could not find track '{}'", id);
            return new ResourceNotFoundException("Could not find track '" + id + "'");
        });

        try {
            Files.delete(Paths.get(LIBRARY + track.getFileName()));
            logger.info("Deleted audio file for track '{}'", id);
        } catch (IOException e) {
            logger.error("Could not delete audio file for track '{}'", id, e);
            throw new GenericException("Could not delete audio file for track '" + id + "'");
        }

        if (Files.exists(Path.of(ARTWORK + id + ARTWORK_EXTENSION))) {
            try {
                Files.delete(Paths.get(ARTWORK + track.getId() + ARTWORK_EXTENSION));
                logger.info("Deleted artwork for track '{}'", id);
            } catch (IOException e) {
                logger.error("Could not delete artwork for track '{}'; ", id, e);
                throw new GenericException("Could not delete artwork for track '" + id + "'");
            }
        }

        trackRepository.delete(track);
        logger.info("Deleted database entry for track '{}'", id);
    }
}
