package com.example.musicbooru.service;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.*;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final static Logger logger = LoggerFactory.getLogger(TrackService.class);

    private final TrackRepository trackRepository;

    public boolean trackExists(String trackId) {
        return trackRepository.existsById(UUID.fromString(trackId));
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }

    public List<Track> getTracks(String field) {
        return trackRepository.findAll(Sort.by(Sort.Direction.ASC, field));
    }

    public Optional<Track> getTrackById(String trackId) {
        return trackRepository.findById(UUID.fromString(trackId));
    }

    public TrackRepository.FileNameOnly getFileName(String trackId) {
        return trackRepository.findProjectedById(UUID.fromString(trackId))
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));
    }

    public Track addTrack(MultipartFile file) {
        try {
            // Save song as temporary file for metadata extraction
            Path temp = Files.createTempFile(null, AUDIO_EXTENSION);
            Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);

            MetadataUtils metadataUtils = new MetadataUtils(temp.toFile());
            String fileName = metadataUtils.generateFileName();
            if (fileName != null && trackRepository.existsByFileName(fileName)) {
                logger.warn("Track with filename '{}' already exists; using UUID for filename", fileName);
                fileName = null;
            }

            UUID trackId = UUID.randomUUID();

            Track track = Track.builder()
                    .id(trackId)
                    .title(metadataUtils.getTitle())
                    .artist(metadataUtils.getArtist())
                    .album(metadataUtils.getAlbum())
                    .genre(metadataUtils.getGenre())
                    .year(metadataUtils.getYear())
                    .fileName((fileName != null) ? fileName : trackId + AUDIO_EXTENSION)
                    .build();

            metadataUtils.extractArtwork(String.valueOf(trackId));

            Path destination = Paths.get(LIBRARY + track.getFileName());
            if (Files.exists(destination)) {
                logger.warn("File '{}' already exists and will be overwritten", track.getFileName());
            }
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);

            return trackRepository.save(track);
        } catch (IOException e) {
            throw new GenericException("Could not add track");
        }
    }

    public void removeTrack(String trackId) {
        Track track = trackRepository.findById(UUID.fromString(trackId))
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));

        try {
            Files.deleteIfExists(Paths.get(ARTWORK + trackId + ARTWORK_EXTENSION));
            Files.deleteIfExists(Paths.get(LIBRARY + track.getFileName()));
            trackRepository.delete(track);
        } catch (IOException e) {
            throw new GenericException("Could not delete track");
        }
    }

    public List<Track> searchTracks(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        return trackRepository.searchTracks(query.trim(), QUERY_CHARACTER_LIMIT);
    }
}
