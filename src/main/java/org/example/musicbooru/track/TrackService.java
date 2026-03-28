package org.example.musicbooru.track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public class TrackService {

    private static final Logger logger = LoggerFactory.getLogger(TrackService.class);

    private final S3Client s3Client;

    private final TrackRepository trackRepository;

    public TrackService(S3Client s3Client, TrackRepository trackRepository) {
        this.s3Client = s3Client;
        this.trackRepository = trackRepository;
    }

    public Track addTrack() {
        throw new UnsupportedOperationException("Method has not been yet implemented");
    }

    public void removeTrack() {
        throw new UnsupportedOperationException("Method has not been yet implemented");
    }
}
