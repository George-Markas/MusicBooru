package com.example.musicbooru.util;

import org.jaudiotagger.tag.Tag;

import static com.example.musicbooru.service.TrackService.artwork;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class MetadataExtractor {

    public static void extractArtwork(Tag tag, String id) throws IOException {
        byte[] imageData = tag.getFirstArtwork().getBinaryData();
        if(imageData != null) {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bais);
            File output = new File(artwork + id + ".jpg");
                ImageIO.write(image, "jpg", output);
        }
    }
}
