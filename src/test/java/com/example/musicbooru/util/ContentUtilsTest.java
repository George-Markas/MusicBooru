package com.example.musicbooru.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentUtilsTest {

    // MP3 header bytes (ID3v2)
    private static final byte[] MP3_BYTES = new byte[]{
            0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    // FLAC header bytes
    private static final byte[] FLAC_BYTES = new byte[]{
            0x66, 0x4C, 0x61, 0x43, 0x00, 0x00, 0x00, 0x22
    };

    // OGG header bytes
    private static final byte[] OGG_BYTES = new byte[]{
            0x4F, 0x67, 0x67, 0x53, 0x00, 0x02, 0x00, 0x00
    };

    private MultipartFile mockFile(byte[] bytes) throws IOException {
        MultipartFile file = mock();
        when(file.getBytes()).thenReturn(bytes);
        return file;
    }

    @Test
    void detectMediaType_detectsMp3() throws IOException {
        MediaType result = ContentUtils.detectMediaType(mockFile(MP3_BYTES));

        assertEquals("audio/mpeg", result.mimeType());
        assertEquals(".mp3", result.extension());
    }

    @Test
    void detectMediaType_detectsFlac() throws IOException {
        MediaType result = ContentUtils.detectMediaType(mockFile(FLAC_BYTES));

        assertEquals("audio/flac", result.mimeType());
        assertEquals(".flac", result.extension());
    }

    @Test
    void detectMediaType_detectsOgg() throws IOException {
        MediaType result = ContentUtils.detectMediaType(mockFile(OGG_BYTES));

        assertEquals("audio/ogg", result.mimeType());
        assertEquals(".ogg", result.extension());
    }

    @Test
    void detectMediaType_throwsRuntimeException_onIOError() throws IOException {
        MultipartFile file = mock();
        when(file.getBytes()).thenThrow(new IOException("I/O error"));

        assertThrows(RuntimeException.class, () -> ContentUtils.detectMediaType(file));
    }

    @Test
    void detectMediaType_returnsMimeType_withPreferredExtension() throws IOException {
        MediaType result = ContentUtils.detectMediaType(mockFile(MP3_BYTES));

        assertNotEquals(".mpga", result.extension());
        assertEquals(".mp3", result.extension());
    }
}