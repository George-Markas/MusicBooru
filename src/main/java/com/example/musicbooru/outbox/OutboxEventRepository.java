package com.example.musicbooru.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxStatus status, Instant createdAtBefore);

    void deleteByStatus(OutboxStatus status);

    void deleteByTrackId(Long trackId);
}
