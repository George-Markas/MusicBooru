package com.example.musicbooru.util;

import com.example.musicbooru.exception.GenericException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ContentUtils {

    private static final Tika tika = new Tika();

    public static BufferedImage convertToJpeg(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String mimeType = tika.detect(inputStream);
            if (mimeType.startsWith("image/")) {
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                if (bufferedImage == null) throw new GenericException("Could not decode image");
                if ("image/jpeg".equals(mimeType)) return bufferedImage;

                // Handle alpha channel
                if (bufferedImage.getColorModel().hasAlpha()) {
                    BufferedImage rgbImage = new BufferedImage(
                            bufferedImage.getWidth(),
                            bufferedImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );
                    rgbImage.createGraphics().drawImage(bufferedImage, 0, 0, java.awt.Color.WHITE, null);
                    bufferedImage = rgbImage;
                }

                return bufferedImage;
            } else {
                throw new IllegalArgumentException("Uploaded file is not an image: " + mimeType);
            }
        } catch (IOException e) {
            throw new  GenericException("Could not convert image to JPEG");
        }
    }
}
