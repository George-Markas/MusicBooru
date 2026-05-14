package com.example.musicbooru.track;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.outbox.OutboxEvent;
import com.example.musicbooru.outbox.OutboxEventRepository;
import com.example.musicbooru.outbox.OutboxStatus;
import com.example.musicbooru.util.ContentUtils;
import com.example.musicbooru.util.MediaType;
import com.example.musicbooru.util.MetadataUtils;
import com.example.musicbooru.util.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private TrackService trackService;

    private final String publicId = "IG1MNki";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(trackService, "artworkBucket", "test-bucket-artwork");
        ReflectionTestUtils.setField(trackService, "libraryBucket", "test-bucket-library");
    }

    // --- addTracks ---

    @Test
    void addTracks_savesTrackAndOutboxEvent() throws Exception {
        Path userUpload = Files.createTempFile(null, ".flac");
        MediaType mediaType = new MediaType("audio/flac", ".flac");

        //noinspection unused
        try (
                MockedStatic<ContentUtils> contentUtils = mockStatic(ContentUtils.class);
                MockedStatic<PublicIdGenerator> idGen = mockStatic(PublicIdGenerator.class);
                MockedStatic<TransactionSynchronizationManager> txManager = mockStatic(TransactionSynchronizationManager.class);
                MockedConstruction<MetadataUtils> MetadataUtilsMock =
                        mockConstruction(MetadataUtils.class, (mock, context) -> {
                            when(mock.getArtist()).thenReturn("Artist");
                            when(mock.getTitle()).thenReturn("Title");
                            when(mock.getAlbum()).thenReturn("Album");
                            when(mock.getYear()).thenReturn("2010-11-22");
                            when(mock.getGenre()).thenReturn("Genre");
                            when(mock.getDuration()).thenReturn(292);
                            when(mock.extractArtwork()).thenReturn(Optional.empty());
                        })
        ) {
            contentUtils.when(() -> ContentUtils.detectMediaType(multipartFile)).thenReturn(mediaType);
            idGen.when(() -> PublicIdGenerator.generate(anyInt(), anyInt(), any())).thenReturn(publicId);

            when(trackRepository.save(any(Track.class))).thenAnswer(invocation -> {
                Track track = invocation.getArgument(0);
                ReflectionTestUtils.setField(track, "id", 1L);

                return track;
            });

            List<Track> result = trackService.addTracks(List.of(multipartFile));

            // Track saved
            verify(trackRepository).save(argThat(track ->
                    publicId.equals(track.getPublicId()) &&
                            TrackStatus.PENDING.equals(track.getStatus())
            ));

            // OutboxEvent saved
            verify(outboxEventRepository).save(argThat(event ->
                    event.getTrackPublicId().equals(publicId) &&
                            event.getStatus() == OutboxStatus.PENDING &&
                            !event.isNeedsTranscoding()
            ));

            // TransactionSynchronization registered
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));

            assertEquals(1, result.size());
            assertEquals(publicId, result.getFirst().getPublicId());
        } finally {
            Files.deleteIfExists(userUpload);
        }
    }

    @Test
    void addTracks_marksForTranscoding_whenFormatIsUnsupported() {
        MediaType mediaType = new MediaType("audio/wav", ".wav");

        //noinspection unused
        try (
                MockedStatic<ContentUtils> contentUtils = mockStatic(ContentUtils.class);
                MockedStatic<PublicIdGenerator> idGen = mockStatic(PublicIdGenerator.class);
                MockedStatic<TransactionSynchronizationManager> ignored = mockStatic(TransactionSynchronizationManager.class);
                MockedConstruction<MetadataUtils> MetadataUtilsMock =
                        mockConstruction(MetadataUtils.class, (mock, context) ->
                                when(mock.extractArtwork()).thenReturn(Optional.empty()))
        ) {
            contentUtils.when(() -> ContentUtils.detectMediaType(multipartFile)).thenReturn(mediaType);
            idGen.when(() -> PublicIdGenerator.generate(anyInt(), anyInt(), any())).thenReturn(publicId);
            when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            trackService.addTracks(List.of(multipartFile));

            verify(outboxEventRepository).save(argThat(OutboxEvent::isNeedsTranscoding));
        }
    }

    @Test
    void addTracks_throwsGenericException_onIOError() throws Exception {
        MediaType mediaType = new MediaType("audio/flac", ".flac");

        //noinspection unused
        try (
                MockedStatic<ContentUtils> contentUtils = mockStatic(ContentUtils.class);
                MockedConstruction<MetadataUtils> MetadataUtilsMock = mockConstruction(MetadataUtils.class)
        ) {
            contentUtils.when(() -> ContentUtils.detectMediaType(multipartFile)).thenReturn(mediaType);
            doThrow(new IOException("I/O error")).when(multipartFile).transferTo(any(Path.class));

            assertThrows(GenericException.class, () -> trackService.addTracks(List.of(multipartFile)));
        }
    }

    // --- removeTracks ---

    @Test
    void removeTracks_deletesS3ObjectsAndEntities() {
        Track track = Track.builder()
                .publicId(publicId)
                .status(TrackStatus.READY)
                .build();

        ReflectionTestUtils.setField(track, "id", 1L);

        when(trackRepository.findByPublicId(publicId)).thenReturn(Optional.of(track));

        trackService.removeTracks(List.of(publicId));

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("test-bucket-artwork") && request.key().equals(publicId)
        ));

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("test-bucket-library") && request.key().equals(publicId)
        ));

        verify(outboxEventRepository).deleteByTrackId(1L);
        verify(trackRepository).delete(track);
    }

    @Test
    void removeTracks_throwsResourceNotFoundException_whenTrackIsNotFound() {
        when(trackRepository.findByPublicId("non-existent-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> trackService.removeTracks(List.of("non-existent-id")));

        verifyNoInteractions(s3Client, outboxEventRepository);
    }

    @Test
    void removeTracks_throws_whenTrackIsStillPending() {
        Track track = Track.builder()
                .publicId("pending-id")
                .status(TrackStatus.PENDING)
                .build();

        when(trackRepository.findByPublicId("pending-id")).thenReturn(Optional.of(track));

        GenericException e = assertThrows(GenericException.class,
                () -> trackService.removeTracks(List.of("pending-id")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verifyNoInteractions(s3Client);
    }
}
