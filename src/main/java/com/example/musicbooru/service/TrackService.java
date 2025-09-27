package com.example.musicbooru.service;

import com.example.musicbooru.model.Track;
import com.example.musicbooru.repository.TrackRepository;
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;

@Service
public class TrackService {

    private final TrackRepository trackRepository;

    public TrackService(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public void addTrack(MultipartFile file) throws IOException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException {
        // Create library directory if it does not exist already
        Path libraryDir = Paths.get("./tracks/covers");
        Files.createDirectories(libraryDir);

        // Save as temporary file for metadata extraction
        String originalFilename = file.getOriginalFilename(); // The file extension needs to be preserved for Jaudiotagger to work
        if(originalFilename == null) {
            throw new IllegalArgumentException("File must have a name");
        }
        String extension = "." + FilenameUtils.getExtension(originalFilename);
        Path tmpFile = Files.createTempFile(null, extension);
        Files.copy(file.getInputStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING);

        // Derive proper file name from metadata
        AudioFile audioFile = AudioFileIO.read(tmpFile.toFile());
        Tag tag = audioFile.getTag();
        String derivedFileName = tag.getFirst(FieldKey.ARTIST) + " - " + tag.getFirst(FieldKey.TITLE) + extension;

        // Move the file to the library directory
        Files.move(tmpFile, Paths.get("./tracks", derivedFileName), StandardCopyOption.REPLACE_EXISTING);

        Track newTrack = Track.builder()
                .fileName(derivedFileName)
                .title(tag.getFirst(FieldKey.TITLE))
                .album(tag.getFirst(FieldKey.ALBUM))
                .artist(tag.getFirst(FieldKey.ARTIST))
                .genre(tag.getFirst(FieldKey.GENRE))
                .year(tag.getFirst(FieldKey.YEAR))
                .sampleRate(audioFile.getAudioHeader().getSampleRate())
                .build();

        trackRepository.save(newTrack);

        // Extract cover art and save it in `./tracks/cover/` using the song's ID as the file name
        byte[] imageData = tag.getFirstArtwork().getBinaryData(); // NullPointer
        if(imageData != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bais);
            File outputFile = new File("./tracks/covers/" + newTrack.getId() + ".jpg");
            ImageIO.write(image, "jpg", outputFile);
        }
    }

    public void deleteTrack(Integer id) throws NoSuchElementException, IOException {
        Track toBeDeleted = trackRepository.findById(id).orElseThrow();
        Path path = Paths.get("./tracks/", toBeDeleted.getFileName());
        Path coverPath = Paths.get("./tracks/covers/", toBeDeleted.getId() + ".jpg");
        Files.delete(path);
        Files.delete(coverPath);
        trackRepository.deleteById(id);
    }

    public Optional<Track> getTrackById(Integer id) {
        return trackRepository.findById(id);
    }

    public List<Track> getTracks() {
        return trackRepository.findAll();
    }
}
