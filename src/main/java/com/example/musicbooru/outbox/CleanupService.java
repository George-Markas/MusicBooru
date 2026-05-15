package com.example.musicbooru.outbox;

import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanupService {

    private final TrackRepository trackRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanUpFinished() {
        outboxEventRepository.deleteByStatus(OutboxStatus.DONE);
        outboxEventRepository.deleteByStatus(OutboxStatus.FAILED);
        trackRepository.deleteByStatus(TrackStatus.FAILED);
    }

    @Scheduled(cron = "0 0 */1 * * *")
    @Transactional
    public void cleanUpStale() {
        List<OutboxEvent> stale = outboxEventRepository.findByStatusAndCreatedAtBefore(
                OutboxStatus.PENDING,
                Instant.now().minus(15, ChronoUnit.MINUTES)
        );

        List<Long> staleIds = stale.stream().map(OutboxEvent::getTrackId).toList();

        outboxEventRepository.deleteAll(stale);
        trackRepository.deleteAllById(staleIds);
    }
}
