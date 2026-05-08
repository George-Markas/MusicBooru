package com.example.musicbooru.track;

import com.example.musicbooru.config.RabbitmqConfig;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.outbox.OutboxEvent;
import com.example.musicbooru.outbox.OutboxEventRepository;
import com.example.musicbooru.outbox.OutboxMessage;
import com.example.musicbooru.outbox.OutboxStatus;
import com.example.musicbooru.util.ContentUtils;
import com.example.musicbooru.util.MediaType;
import com.example.musicbooru.util.MetadataUtils;
import com.example.musicbooru.util.PublicIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.musicbooru.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final S3Client s3Client;

    @Value("${garage.bucket-artwork}")
    private String artworkBucket;

    @Value("${garage.bucket-library}")
    private String libraryBucket;

    @Transactional
    public List<Track> addTracks(List<MultipartFile> files) {
        List<Track> tracks = new ArrayList<>();
        for (MultipartFile file : files) {
            // Copy the user uploaded content to a temporary file for processing
            Path userUpload;
            MediaType mediaType;
            try {
                mediaType = ContentUtils.detectMediaType(file);
                userUpload = Files.createTempFile(null, mediaType.extension());
                file.transferTo(userUpload);
            } catch (IOException e) {
                throw new GenericException("Failed to copy user upload to temporary file", e);
            }

            // Create a unique public ID for API use
            String publicId;
            try {
                publicId = PublicIdGenerator.generate(PUBLIC_ID_LENGTH, 3, trackRepository::existsByPublicId);
            } catch (RuntimeException e) {
                throw new GenericException(e.getMessage(), e);
            }

            // Check if the uploaded content is in one of the supported formats. If it isn't,
            // mark it for transcoding.
            boolean markedForTranscoding = !SUPPORTED_MEDIA_TYPES.containsValue(mediaType);
            String trackMimeType = markedForTranscoding
                    ? SUPPORTED_MEDIA_TYPES.get(FALLBACK_MEDIA_TYPE).mimeType()
                    : mediaType.mimeType();

            // Create track entity instance
            MetadataUtils metadataUtils = new MetadataUtils(userUpload.toFile());

            Track track = Track.builder()
                    .publicId(publicId)
                    .artist(metadataUtils.getArtist())
                    .title(metadataUtils.getTitle())
                    .album(metadataUtils.getAlbum())
                    .year(metadataUtils.getYear())
                    .genre(metadataUtils.getGenre())
                    .duration(metadataUtils.getDuration())
                    .mimeType(trackMimeType)
                    .status(TrackStatus.PENDING)
                    .build();

            trackRepository.save(track);

            // If embedded artwork is present, extract it
            String artworkPath;
            try {
                Optional<Path> artwork = metadataUtils.extractArtwork();
                artworkPath = artwork.map(Path::toString).orElse(null);
            } catch (RuntimeException e) {
                throw new GenericException(e.getMessage(), e);
            }

            // Create outbox event
            OutboxEvent event = OutboxEvent.builder()
                    .trackId(track.getId())
                    .trackPublicId(publicId)
                    .audioPath(userUpload.toString())
                    .artworkPath(artworkPath)
                    .needsTranscoding(markedForTranscoding)
                    .status(OutboxStatus.PENDING)
                    .retries(0)
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.save(event);

            // Send message to the exchange. We need the outbox event to be committed
            // to the database before the message is published so the OutboxService can find it, hence
            // the TransactionSynchronization.
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitTemplate.convertAndSend(
                                    RabbitmqConfig.EXCHANGE,
                                    RabbitmqConfig.ROUTING_KEY,
                                    new OutboxMessage(event.getId())
                            );
                        }
                    }
            );

            tracks.add(track);
        }

        return tracks;
    }

    @Transactional
    public void removeTracks(List<String> trackPublicIds) {
        for (String trackPublicId : trackPublicIds) {
            Track track = trackRepository.findByPublicId(trackPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Track", trackPublicId));

            if (track.getStatus() == TrackStatus.PENDING) {
                throw new GenericException("Track is still being processed", HttpStatus.CONFLICT);
            }

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(artworkBucket)
                            .key(trackPublicId)
                            .build()
            );

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(libraryBucket)
                            .key(trackPublicId)
                            .build()
            );

            outboxEventRepository.deleteByTrackId(track.getId());
            trackRepository.delete(track);
            log.info("Track '{}' removed", track.getPublicId());
        }
    }

    @Profile("dev")
    @Transactional
    public void removeAllTracks() {
        outboxEventRepository.deleteAll();
        trackRepository.deleteAll();
        emptyS3Bucket(libraryBucket);
        emptyS3Bucket(artworkBucket);
        log.debug("Removed all tracks");
    }

    @Profile("dev")
    private void emptyS3Bucket(String bucket) {
        List<ObjectIdentifier> objects = new ArrayList<>();
        s3Client.listObjectsV2Paginator(r -> r.bucket(bucket))
                .contents()
                .forEach(obj -> objects.add(
                        ObjectIdentifier.builder().key(obj.key()).build()
                ));

        if (!objects.isEmpty()) {
            s3Client.deleteObjects(r -> r
                    .bucket(bucket)
                    .delete(d -> d.objects(objects))
            );
        }
    }
}
