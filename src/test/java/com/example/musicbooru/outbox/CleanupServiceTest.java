package com.example.musicbooru.outbox;

import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class CleanupServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private CleanupService cleanupService;

    // --- cleanUp ---

    @Test
    void cleanUp_deletesAllTracksAndOutboxEventsWithFinishedStatuses() {
        cleanupService.cleanUp();

        verify(outboxEventRepository).deleteByStatus(OutboxStatus.DONE);
        verify(outboxEventRepository).deleteByStatus(OutboxStatus.FAILED);
        verify(trackRepository).deleteByStatus(TrackStatus.FAILED);

        verifyNoMoreInteractions(outboxEventRepository, trackRepository);
    }
}
