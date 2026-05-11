package com.example.musicbooru.util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.example.musicbooru.util.Constants.ARTWORK_EXTENSION;

public class MetadataUtils {

    private final AudioFile audioFile;
    private final Tag tag;

    public MetadataUtils(File file) {
        try {
            this.audioFile = AudioFileIO.read(file);
            this.tag = this.audioFile.getTag();
        } catch (Exception e) {
            throw new RuntimeException("Could not read the tag contained in the given file", e);
        }
    }

    /**
     * Extracts the embedded cover art from the given file.
     *
     * @return the path to a newly created temporary file to which the cover art was copied,
     * wrapped in an {@code Optional}.
     * @throws RuntimeException if extracting the embedded cover art fails.
     */
    public Optional<Path> extractArtwork() {
        Artwork artwork = this.tag.getFirstArtwork();
        if (artwork == null) return Optional.empty();

        try {
            byte[] image = artwork.getBinaryData();
            Path temp = Files.createTempFile(null, ARTWORK_EXTENSION);

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            ImageIO.write(bufferedImage, "jpg", temp.toFile());

            // Delete the embedded artwork since we don't want it in the blob
            this.tag.deleteArtworkField();
            this.audioFile.commit();

            return Optional.of(temp);
        } catch (IOException | CannotWriteException e) {
            throw new RuntimeException("Failed to extract artwork", e);
        }
    }

    private String getField(FieldKey field) {
        try {
            return this.tag.getFirst(field);
        } catch (KeyNotFoundException e) {
            return "";
        }
    }

    public String getArtist() {
        return getField(FieldKey.ARTIST);
    }

    public String getTitle() {
        return getField(FieldKey.TITLE);
    }

    public String getAlbum() {
        return getField(FieldKey.ALBUM);
    }

    public String getYear() {
        return getField(FieldKey.YEAR).substring(0, 10);
    }

    public String getGenre() {
        return getField(FieldKey.GENRE);
    }

    public int getDuration() {
        return audioFile.getAudioHeader().getTrackLength();
    }
}
