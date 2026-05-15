package com.example.musicbooru.outbox;

import com.example.musicbooru.track.Track;
import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private TranscodingService transcodingService;

    @Mock
    private Channel channel;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private OutboxService outboxService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                outboxService,
                "transactionTemplate",
                new TransactionTemplate(transactionManager)
        );
        ReflectionTestUtils.setField(outboxService, "artworkBucket", "test-bucket-artwork");
        ReflectionTestUtils.setField(outboxService, "libraryBucket", "test-bucket-library");
    }

    private OutboxMessage message() {
        return new OutboxMessage(1L);
    }

    private OutboxEvent pendingEvent(Path audioPath, Path artworkPath, boolean needsTranscoding, int retries) {
        return OutboxEvent.builder()
                .id(1L)
                .trackId(10L)
                .trackPublicId("IG1MNki")
                .audioPath(audioPath.toString())
                .artworkPath(artworkPath != null ? artworkPath.toString() : null)
                .needsTranscoding(needsTranscoding)
                .status(OutboxStatus.PENDING)
                .retries(retries)
                .build();
    }

    @Test
    void processEvent_nacks_whenEventNotFound() throws IOException {
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.empty());

        outboxService.processEvent(message(), channel, 1L);

        verify(channel).basicNack(1L, false, false);
        verifyNoInteractions(s3Client, trackRepository, transcodingService);
    }

    @Test
    void processEvent_acks_whenEventAlreadyDone() throws IOException {
        OutboxEvent event = OutboxEvent.builder().status(OutboxStatus.DONE).build();
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        outboxService.processEvent(message(), channel, 1L);

        verify(channel).basicAck(1L, false);
        verifyNoInteractions(s3Client, trackRepository, transcodingService);
    }

    @Test
    void processEvent_acks_whenEventAlreadyFailed() throws IOException {
        OutboxEvent event = OutboxEvent.builder().status(OutboxStatus.FAILED).build();
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        outboxService.processEvent(message(), channel, 1L);

        verify(channel).basicAck(1L, false);
        verifyNoInteractions(s3Client, trackRepository, transcodingService);
    }

    @Test
    void processEvent_uploadsAndAcks_whenNoArtworkNoTranscoding() throws IOException {
        Path audioPath = Files.createTempFile(tempDir, null, ".flac");
        Track track = Track.builder().status(TrackStatus.PENDING).build();
        ReflectionTestUtils.setField(track, "id", 10L);

        OutboxEvent event = pendingEvent(audioPath, null, false, 0);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(trackRepository.findById(10L)).thenReturn(Optional.of(track));

        outboxService.processEvent(message(), channel, 1L);

        verify(s3Client, never()).putObject(
                argThat((PutObjectRequest request) -> request.bucket().equals("test-bucket-artwork")),
                any(RequestBody.class)
        );

        verify(s3Client).putObject(
                argThat((PutObjectRequest request) ->
                        request.bucket().equals("test-bucket-library") &&
                                request.key().equals("IG1MNki")
                ),
                any(RequestBody.class)
        );

        assertEquals(OutboxStatus.DONE, event.getStatus());
        assertEquals(TrackStatus.READY, track.getStatus());
        verify(outboxEventRepository).save(event);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void processEvent_uploadsArtwork_whenArtworkPresent() throws IOException {
        Path audioPath = Files.createTempFile(tempDir, null, ".flac");
        Path artworkPath = Files.createTempFile(tempDir, null, ".jpg");
        Track track = Track.builder().status(TrackStatus.PENDING).build();

        OutboxEvent event = pendingEvent(audioPath, artworkPath, false, 0);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(trackRepository.findById(10L)).thenReturn(Optional.of(track));

        outboxService.processEvent(message(), channel, 1L);

        verify(s3Client).putObject(
                argThat((PutObjectRequest request) ->
                        request.bucket().equals("test-bucket-artwork") &&
                                request.key().equals("IG1MNki")
                ),
                any(RequestBody.class)
        );

        verify(channel).basicAck(1L, false);
    }

    @Test
    void processEvent_transcodes_whenMarkedForTranscoding() throws IOException, InterruptedException {
        Path audioPath = Files.createTempFile(tempDir, null, ".wav");
        Path transcodedPath = Files.createTempFile(tempDir, null, ".flac");
        Track track = Track.builder().status(TrackStatus.PENDING).build();

        OutboxEvent event = pendingEvent(audioPath, null, true, 0);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(transcodingService.transcode(audioPath)).thenReturn(transcodedPath);
        when(trackRepository.findById(10L)).thenReturn(Optional.of(track));

        outboxService.processEvent(message(), channel, 1L);

        verify(transcodingService).transcode(audioPath);
        verify(s3Client).putObject(
                argThat((PutObjectRequest r) -> r.bucket().equals("test-bucket-library")),
                any(RequestBody.class)
        );

        verify(channel).basicAck(1L, false);
    }

    @Test
    void processEvent_incrementsRetries_andNacksWithRequeue_onFailure() throws IOException {
        Path audioPath = Files.createTempFile(tempDir, null, ".flac");
        OutboxEvent event = pendingEvent(audioPath, null, false, 0);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));

        outboxService.processEvent(message(), channel, 1L);

        assertEquals(1, event.getRetries());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        verify(outboxEventRepository).save(event);
        verify(channel).basicNack(1L, false, true);
        verifyNoInteractions(trackRepository);
    }

    @Test
    void processEvent_marksEventAndTrackFailed_whenMaxRetriesReached() throws IOException {
        Path audioPath = Files.createTempFile(tempDir, null, ".mp3");
        Track track = Track.builder().status(TrackStatus.PENDING).build();

        OutboxEvent event = pendingEvent(audioPath, null, false, 2);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));
        when(trackRepository.findById(10L)).thenReturn(Optional.of(track));

        outboxService.processEvent(message(), channel, 1L);

        assertEquals(3, event.getRetries());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(TrackStatus.FAILED, track.getStatus());
        verify(trackRepository).save(track);
        verify(outboxEventRepository).save(event);
        verify(channel).basicNack(1L, false, false);
    }
}