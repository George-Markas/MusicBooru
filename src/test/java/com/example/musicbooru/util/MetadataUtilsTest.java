package com.example.musicbooru.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataUtilsTest {

    private static File audioWithMetadata;
    private static File audioWithoutMetadata;
    private static File audioWithArtwork;

    @BeforeAll
    static void setUp() {
        audioWithMetadata = new File("src/test/resources/test_files/has_metadata.mp3");
        audioWithoutMetadata = new File("src/test/resources/test_files/no_metadata.mp3");
        audioWithArtwork = new File("src/test/resources/test_files/has_artwork.mp3");
    }

    // --- Constructor ---

    @Test
    void constructor_throwsRuntimeException_whenFileUnreadable() {
        assertThrows(
                RuntimeException.class,
                () -> new MetadataUtils(new File("nonexistent.mp3"))
        );
    }

    // --- Getters ---

    @Test
    void getArtist_returnsArtist_whenPresent() {
        MetadataUtils metadataUtils = new MetadataUtils(audioWithMetadata);
        assertEquals("Test Artist", metadataUtils.getArtist());
    }

    @Test
    void getTitle_returnsTitle_whenPresent() {
        MetadataUtils metadataUtils = new MetadataUtils(audioWithMetadata);
        assertEquals("Test Title", metadataUtils.getTitle());
    }

    @Test
    void getAlbum_returnsAlbum_whenPresent() {
        MetadataUtils metadataUtils = new MetadataUtils(audioWithMetadata);
        assertEquals("Test Album", metadataUtils.getAlbum());
    }

    @Test
    void getYear_returnsYear_whenPresent() {
        MetadataUtils metadataUtils = new MetadataUtils(audioWithMetadata);
        assertEquals("1970-01-01", metadataUtils.getYear());
    }

    @Test
    void getGenre_returnsGenre_whenPresent() {
        MetadataUtils utils = new MetadataUtils(audioWithMetadata);
        assertEquals("Test Genre", utils.getGenre());
    }

    @Test
    void getDuration_returnsPositiveInt() {
        MetadataUtils utils = new MetadataUtils(audioWithMetadata);
        assertTrue(utils.getDuration() > 0);
    }

    @Test
    void getArtist_returnsEmptyString_whenMissing() {
        MetadataUtils utils = new MetadataUtils(audioWithoutMetadata);
        assertEquals("", utils.getArtist());
    }

    // --- extractArtwork ---

    @Test
    void extractArtwork_returnsEmpty_whenNoArtwork() {
        MetadataUtils utils = new MetadataUtils(audioWithoutMetadata);
        assertTrue(utils.extractArtwork().isEmpty());
    }

    @Test
    void extractArtwork_returnsPath_whenArtworkPresent(@TempDir Path tempDir) throws IOException {
        Path copy = Files.copy(audioWithArtwork.toPath(), tempDir.resolve("sample.mp3"));
        MetadataUtils metadataUtils = new MetadataUtils(copy.toFile());

        Optional<Path> result = metadataUtils.extractArtwork();

        assertTrue(result.isPresent());
        assertTrue(Files.exists(result.get()));
        assertTrue(Files.size(result.get()) > 0);

        Files.deleteIfExists(result.get());
    }

    @Test
    void extractArtwork_stripsArtworkFromFile(@TempDir Path tempDir) throws IOException {
        Path copy = Files.copy(audioWithArtwork.toPath(), tempDir.resolve("sample.mp3"));
        MetadataUtils metadataUtils = new MetadataUtils(copy.toFile());

        metadataUtils.extractArtwork();

        MetadataUtils reread = new MetadataUtils(copy.toFile());
        assertTrue(reread.extractArtwork().isEmpty());
    }
}
