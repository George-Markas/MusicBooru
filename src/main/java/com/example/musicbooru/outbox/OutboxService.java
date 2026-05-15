package com.example.musicbooru.outbox;

import com.example.musicbooru.config.RabbitmqConfig;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final TrackRepository trackRepository;
    private final S3Client s3Client;
    private final TransactionTemplate transactionTemplate;
    private final TranscodingService transcodingService;

    private static final int MAX_RETRIES = 3;

    @Value("${garage.bucket-artwork}")
    private String artworkBucket;

    @Value("${garage.bucket-library}")
    private String libraryBucket;

    @RabbitListener(queues = RabbitmqConfig.QUEUE)
    public void processEvent(OutboxMessage message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws IOException {

        OutboxEvent event = outboxEventRepository.findById(message.outboxEventId())
                .orElse(null);

        // Event gone
        if (event == null) {
            log.warn("Event '{}' not found", message.outboxEventId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // Event already handled
        if (event.getStatus() != OutboxStatus.PENDING) {
            channel.basicAck(deliveryTag, false); // Already handled
            return;
        }

        try {
            uploadToS3(event);

            transactionTemplate.executeWithoutResult(status -> {
                markTrackReady(event.getTrackId());
                event.setStatus(OutboxStatus.DONE);
                outboxEventRepository.save(event);
            });

            channel.basicAck(deliveryTag, false);
            log.info("Track '{}' added", event.getTrackPublicId());

        } catch (RuntimeException e) {
            log.error("Outbox processing failed for event '{}'", event.getId(), e);
            event.setRetries(event.getRetries() + 1);

            if (event.getRetries() >= MAX_RETRIES) {
                transactionTemplate.executeWithoutResult(status -> {
                    event.setStatus(OutboxStatus.FAILED);
                    markTrackFailed(event.getTrackId());
                    outboxEventRepository.save(event);
                });
                channel.basicNack(deliveryTag, false, false);
            } else {
                transactionTemplate.executeWithoutResult(status ->
                        outboxEventRepository.save(event)
                );
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }

    private void uploadToS3(OutboxEvent event) {
        Path audioPath = null;
        Path userUpload = Path.of(event.getAudioPath());
        try {
            // If the audio file has been marked for transcoding, pass it to TranscodingService
            // and get the path to the transcoded output, otherwise use it as is.
            audioPath = event.isNeedsTranscoding()
                    ? transcodingService.transcode(userUpload)
                    : userUpload;

            if (event.getArtworkPath() != null) {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(artworkBucket)
                                .key(event.getTrackPublicId())
                                .build(),
                        RequestBody.fromFile(Path.of(event.getArtworkPath()))
                );
            }

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(libraryBucket)
                            .key(event.getTrackPublicId())
                            .build(),
                    RequestBody.fromFile(audioPath)
            );
        } catch (IOException | InterruptedException e) {
            throw new GenericException(e.getMessage(), e);
        } finally {
            deleteTempFile(userUpload);
            if (audioPath != null && !audioPath.equals(userUpload)) deleteTempFile(audioPath);
            if (event.getArtworkPath() != null) deleteTempFile(Path.of(event.getArtworkPath()));
        }
    }

    private void markTrackReady(Long trackId) {
        trackRepository.findById(trackId).ifPresent(track -> {
            track.setStatus(TrackStatus.READY);
            trackRepository.save(track);
        });
    }

    private void markTrackFailed(Long trackId) {
        trackRepository.findById(trackId).ifPresent(track -> {
            track.setStatus(TrackStatus.FAILED);
            trackRepository.save(track);
        });
    }

    private void deleteTempFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not clean up temporary file '{}'", path, e);
        }
    }
}
