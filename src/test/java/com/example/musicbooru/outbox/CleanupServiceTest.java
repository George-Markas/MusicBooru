package com.example.musicbooru.outbox;

import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CleanupServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private CleanupService cleanupService;

    // --- cleanUpFinished ---

    @Test
    void cleanUpFinished_deletesAllOutboxEventsAndTracksWithFinishedStatuses() {
        cleanupService.cleanUpFinished();

        verify(outboxEventRepository).deleteByStatus(OutboxStatus.DONE);
        verify(outboxEventRepository).deleteByStatus(OutboxStatus.FAILED);
        verify(trackRepository).deleteByStatus(TrackStatus.FAILED);

        verifyNoMoreInteractions(outboxEventRepository, trackRepository);
    }

    // --- cleanUpStale ---

    @Test
    void cleanUpStale_deletesStaleOutboxEventsAndTracks() {
        OutboxEvent stale1 = OutboxEvent.builder().trackId(1L).build();
        OutboxEvent stale2 = OutboxEvent.builder().trackId(2L).build();

        when(outboxEventRepository.findByStatusAndCreatedAtBefore(eq(OutboxStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(stale1, stale2));

        cleanupService.cleanUpStale();

        verify(outboxEventRepository).deleteAll(List.of(stale1, stale2));
        verify(trackRepository).deleteAllById(List.of(1L, 2L));

        verifyNoMoreInteractions(outboxEventRepository, trackRepository);
    }

    @Test
    void cleanUpStale_deletesPendingOutboxEventsOlderThan15Minutes() {
        Instant before = Instant.now();

        when(outboxEventRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of());

        cleanupService.cleanUpStale();

        Instant after = Instant.now();

        verify(outboxEventRepository).findByStatusAndCreatedAtBefore(
                eq(OutboxStatus.PENDING),
                argThat(threshold ->
                        !threshold.isAfter(before.minus(15, ChronoUnit.MINUTES)) ||
                                !threshold.isAfter(after.minus(15, ChronoUnit.MINUTES))
                )
        );
    }
}
