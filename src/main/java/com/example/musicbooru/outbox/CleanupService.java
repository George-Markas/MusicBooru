package com.example.musicbooru.outbox;

import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CleanupService {

    private final TrackRepository trackRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "0 0 0 */7 * *")
    @Transactional
    public void cleanUp() {
        outboxEventRepository.deleteByStatus(OutboxStatus.DONE);
        outboxEventRepository.deleteByStatus(OutboxStatus.FAILED);
        trackRepository.deleteByStatus(TrackStatus.FAILED);
    }
}
