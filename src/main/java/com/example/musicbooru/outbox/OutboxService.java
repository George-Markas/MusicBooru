package com.example.musicbooru.outbox;

import com.example.musicbooru.config.RabbitmqConfig;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private static final int MAX_ATTEMPTS = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final TrackRepository trackRepository;
    private final S3Client s3Client;
    private final RabbitTemplate rabbitTemplate;
    private final TranscodingService transcodingService;

    @Value("${garage.bucket-artwork}")
    private String artworkBucket;

    @Value("${garage.bucket-library}")
    private String libraryBucket;

    @RabbitListener(queues = RabbitmqConfig.QUEUE)
    @Transactional
    public void processEvent(OutboxMessage message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws IOException {

        OutboxEvent event = outboxEventRepository.findById(message.outboxEventId())
                .orElse(null);

        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            channel.basicAck(deliveryTag, false); // Already handled or gone
            return;
        }

        try {
            uploadToS3(event);
            markTrackReady(event.getTrackId());
            event.setStatus(OutboxStatus.DONE);
            outboxEventRepository.save(event);
            channel.basicAck(deliveryTag, false);
            log.info("Track '{}' added", event.getTrackPublicId());
        } catch (RuntimeException e) {
            log.error("Outbox processing failed for event '{}'", event.getId(), e);
            event.updateAttempts();

            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.setStatus(OutboxStatus.FAILED);
                markTrackFailed(event.getTrackId());
                outboxEventRepository.save(event);
                channel.basicNack(deliveryTag, false, false);
                throw new GenericException("Could not process the uploaded content", e);
            }
            outboxEventRepository.save(event);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    // Republishes "stuck" events every 5 minutes. Events with a 'PENDING' status
    // that are older than a minute are considered "stuck".
    @Scheduled(fixedDelay = 300_000)
    public void recoverStuck() {
        Instant threshold = Instant.now().minusSeconds(60);
        outboxEventRepository
                .findByStatusAndCreatedAtBefore(OutboxStatus.PENDING, threshold)
                .forEach(event -> rabbitTemplate.convertAndSend(
                        RabbitmqConfig.EXCHANGE,
                        RabbitmqConfig.ROUTING_KEY,
                        new OutboxMessage(event.getId())
                ));
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
