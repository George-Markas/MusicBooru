package com.example.musicbooru.util;

import com.example.musicbooru.exception.GenericException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static com.example.musicbooru.util.Commons.*;

public class MetadataUtils {

    private final static Logger logger = LoggerFactory.getLogger(MetadataUtils.class);

    private final AudioFile audioFile;
    private final Tag tag;

    public MetadataUtils(File file) {
        try {
            this.audioFile = AudioFileIO.read(file);
            this.tag = audioFile.getTag();
        } catch(Exception e) {
            logger.error("Could not read the tag contained in the given file", e);
            throw new GenericException("Could not read the tag contained in the given file");
        }
    }

    public String generateFileName() {
        try {
            String artist = this.tag.getFirst(FieldKey.ARTIST);
            String title = this.tag.getFirst(FieldKey.TITLE);

            if(artist.isBlank() || title.isBlank()) {
                logger.warn("Field is blank; using UUID for filename");
                return null;
            }

            return String.format("%s - %s%s", artist, title, AUDIO_EXTENSION);
        } catch(KeyNotFoundException e) {
            logger.warn("Field does not exist; using UUID for filename", e);
            return null;
        }
    }

    public void extractArtwork(String id) {
        Artwork artwork = this.tag.getFirstArtwork();
        if(artwork != null) {
            byte[] imageData = artwork.getBinaryData();
            try {
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
                ImageIO.write(bufferedImage, "jpg", new File(ARTWORK + id + ARTWORK_EXTENSION));

                // Delete the embedded artwork since we don't need two instances of it
                this.tag.deleteArtworkField();
                this.audioFile.commit();
            } catch(IOException e) {
                logger.error("Could not read image data", e);
            } catch(CannotWriteException e) {
                logger.error("Could not write to file", e);
            }
        } else {
            logger.warn("Track with ID {} has no embedded cover art", id);
        }
    }

    public String getTitle() {
        try {
            return this.tag.getFirst(FieldKey.TITLE);
        } catch(KeyNotFoundException e) {
            logger.error("Field does not exist", e);
            return "";
        }
    }

    public String getArtist() {
        try {
            return this.tag.getFirst(FieldKey.ARTIST);
        } catch(KeyNotFoundException e) {
            logger.error("Field does not exist", e);
            return "";
        }
    }

    public String getAlbum() {
        try {
            return this.tag.getFirst(FieldKey.ALBUM);
        }  catch(KeyNotFoundException e) {
            logger.error("Field does not exist", e);
            return "";
        }
    }

    public String getGenre() {
        try {
            return this.tag.getFirst(FieldKey.GENRE);
        }  catch(KeyNotFoundException e) {
            logger.error("Field does not exist", e);
            return "";
        }
    }

    public String getYear() {
        try {
            return this.tag.getFirst(FieldKey.YEAR);
        }  catch(KeyNotFoundException e) {
            logger.error("Field does not exist", e);
            return "";
        }
    }
}
