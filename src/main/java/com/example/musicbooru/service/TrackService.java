package com.example.musicbooru.service;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
import org.apache.tika.exception.TikaException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.musicbooru.util.FileTypeDetector.detectFileExtension;
import static com.example.musicbooru.util.FileTypeDetector.detectMediaType;
import static com.example.musicbooru.util.MetadataExtractor.extractArtwork;

@Service
public class TrackService {

    public static final String library = "./library/";
    public static final String artwork = "./artwork/";

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public void uploadTrack(MultipartFile file) {
        // Create directories for the audio files and accompanying artwork, if the directories don't exist
        try {
            Files.createDirectories(Path.of(library));
            Files.createDirectories(Path.of(artwork));
        } catch(IOException e) {
            // TODO Add proper logging
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        try {
            // Save as temporary file for metadata extraction
            String fileExtension =  detectFileExtension(file);
            Path tmp = Files.createTempFile(null, fileExtension);
            Files.copy(file.getInputStream(), tmp, StandardCopyOption.REPLACE_EXISTING);

            // Use the metadata to construct a file name
            AudioFile audioFile = AudioFileIO.read(tmp.toFile());
            Tag tag = audioFile.getTag();
            String fileName = tag.getFirst(FieldKey.ARTIST) + " - " + tag.getFirst(FieldKey.TITLE) + fileExtension;

            // Move file to the library directory
            Files.move(tmp, Paths.get(library + fileName), StandardCopyOption.REPLACE_EXISTING);

            Track track = Track.builder()
                    .title(tag.getFirst(FieldKey.TITLE))
                    .artist(tag.getFirst(FieldKey.ARTIST))
                    .album(tag.getFirst(FieldKey.ALBUM))
                    .genre(tag.getFirst(FieldKey.GENRE))
                    .year(tag.getFirst(FieldKey.YEAR))
                    .sampleRate(audioFile.getAudioHeader().getSampleRate())
                    .mediaType(detectMediaType(file))
                    .fileName(fileName)
                    .build();

            trackRepository.save(track);

            extractArtwork(tag, track.getId());

        } catch(TikaException | IOException | CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            // TODO Add proper logging
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteTrack(String id) throws NoSuchElementException, IOException {
        Track toBeDeleted = trackRepository.findById(id).orElseThrow();
        Path path = Paths.get(library, toBeDeleted.getFileName());
        Path coverPath = Paths.get(artwork, toBeDeleted.getId() + ".jpg");
        Files.delete(path);
        Files.delete(coverPath);
        trackRepository.deleteById(id);
    }

    public Optional<Track> getTrackById(String id) {
        return trackRepository.findById(id);
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }
}
