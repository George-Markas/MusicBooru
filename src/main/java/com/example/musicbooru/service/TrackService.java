package com.example.musicbooru.service;

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
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TrackService {

    private final Logger logger = LoggerFactory.getLogger(TrackService.class.getName());
    private final TrackRepository trackRepository;

    public final static String LIBRARY = "./library/";
    public final static String ARTWORK = "./artwork/";

    // TODO Support more audio formats
    public final static String FILE_EXTENSION = ".flac"; // Assuming FLAC for now

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public void uploadTrack(MultipartFile file) {
        try {
            // Create directories for the audio files and accompanying artwork, if said directories don't exist
            Files.createDirectories(Path.of(LIBRARY));
            Files.createDirectories(Path.of(ARTWORK));
        } catch(IOException e) {
            logger.error("Could not create directory; an I/O error occurred", e);
        }

        try {
            // Save song as temporary file for metadata extraction
            Path tmp = Files.createTempFile(null, FILE_EXTENSION);
            Files.copy(file.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);

            // Construct file name from metadata
            JaudiotaggerWrapper jwrap = new JaudiotaggerWrapper(tmp.toFile());
            final String fileName = jwrap.constructFileName();

            // Move song to the library directory
            final Path target = Paths.get(LIBRARY + fileName);
            if(Files.exists(target)) {
                logger.warn("File \"{}\" already exists and will be overwritten", fileName);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);

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
            }

            logger.info("Uploaded track with ID {}", track.getId());
        } catch(IOException e) {
            logger.error("Could not create temporary file; an I/O error occurred", e);
        }
    }

    public void deleteTrack(String id) {
        try {
            Track track = trackRepository.findById(id).orElseThrow();
            try {
                trackRepository.delete(track);
                Files.delete(Paths.get(LIBRARY + track.getFileName()));
                Files.delete(Paths.get(ARTWORK + track.getId() + ".webp"));
                logger.info("Deleted track with ID {}", id);
            } catch(NoSuchFileException e) {
                logger.error("File does not exist", e);
            } catch(IOException e) {
                logger.error("Could not delete file; an I/O error occurred", e);
            }
        } catch(NoSuchElementException e) {
            logger.error("Could not find track with ID {}", id, e);
        }
    }

    public Optional<Track> getTrackById(String id) {
        return trackRepository.findById(id);
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }
}
