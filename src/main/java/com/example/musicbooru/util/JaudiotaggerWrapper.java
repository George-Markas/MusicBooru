package com.example.musicbooru.util;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.Getter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.example.musicbooru.service.TrackService.ARTWORK;
import static com.example.musicbooru.service.TrackService.FILE_EXTENSION;

@Getter
public class JaudiotaggerWrapper {

    private AudioFile audioFile;
    private Tag tag;
    private final Logger logger = LoggerFactory.getLogger(JaudiotaggerWrapper.class.getName());

    public JaudiotaggerWrapper(File file) {
        try {
            audioFile = AudioFileIO.read(file);
            this.tag = audioFile.getTag();
        } catch(CannotReadException e) {
            logger.error("File could not be read", e);
        } catch(IOException e) {
            logger.error("An I/O error occurred", e);
        } catch(TagException e) {
            logger.error("Jaudiotagger tag error", e);
        } catch(ReadOnlyFileException e) {
            logger.error("Attempted to access a read only file", e);
        } catch(InvalidAudioFrameException e) {
            logger.error("File portion thought to be an AudioFrame is not one", e);
        }
    }

    public String constructFileName() {
        return this.tag.getFirst(FieldKey.ARTIST) + " - " + this.tag.getFirst(FieldKey.TITLE) + FILE_EXTENSION;
    }

    public boolean extractArtwork(String id) {
        Artwork artwork = this.tag.getFirstArtwork();
        if(artwork != null) {
            byte[] imageData = artwork.getBinaryData();
            try {
                ImmutableImage immutableImage = ImmutableImage.loader().fromBytes(imageData);
                immutableImage.output(WebpWriter.MAX_LOSSLESS_COMPRESSION, ARTWORK + id + ".webp");
                // Delete the embedded artwork since we don't need two instances of it
                this.tag.deleteArtworkField();
                this.audioFile.commit();
                return true;
            } catch(IOException e) {
                logger.error("Could not read image data; an I/O error occurred", e);
            } catch(CannotWriteException e) {
                logger.error("Could not write to file", e);
            }
        } else {
            logger.warn("Track with ID {} has no embedded cover art", id);
        }
        return false;
    }
}
