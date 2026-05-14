package com.example.musicbooru.track;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private TrackService trackService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(trackService, "artworkBucket", "test-bucket-artwork");
        ReflectionTestUtils.setField(trackService, "libraryBucket", "test-bucket-library");
    }

    // --- addTracks ---

    @Test
    void addTracks_savesTrackAndOutboxEvent() throws Exception {
        Path fakeUserUpload = Files.createTempFile(null, ".flac");
        MediaType mediaType = new MediaType("audio/flac", ".flac");
        String fakePublicId = "IG1MNki";

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
            idGen.when(() -> PublicIdGenerator.generate(anyInt(), anyInt(), any())).thenReturn(fakePublicId);

            when(trackRepository.save(any(Track.class))).thenAnswer(invocation -> {
                Track track = invocation.getArgument(0);
                ReflectionTestUtils.setField(track, "id", 1L);

                return track;
            });

            List<Track> result = trackService.addTracks(List.of(multipartFile));

            // Track saved
            verify(trackRepository).save(argThat(track ->
                    fakePublicId.equals(track.getPublicId()) &&
                            TrackStatus.PENDING.equals(track.getStatus())
            ));

            // OutboxEvent saved
            verify(outboxEventRepository).save(argThat(event ->
                    event.getTrackPublicId().equals(fakePublicId) &&
                            event.getStatus() == OutboxStatus.PENDING &&
                            !event.isNeedsTranscoding()
            ));

            // TransactionSynchronization registered
            txManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));

            assertEquals(1, result.size());
            assertEquals(fakePublicId, result.getFirst().getPublicId());
        } finally {
            Files.deleteIfExists(fakeUserUpload);
        }
    }
}
