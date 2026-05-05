package com.example.musicbooru.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long trackId;

    @Column(nullable = false, unique = true, length = 7, columnDefinition = "bpchar(7)")
    private String trackPublicId;

    @Column(nullable = false)
    private String audioPath;

    private String artworkPath;

    @Column(nullable = false)
    private boolean needsTranscoding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastAttemptedAt;

    public void updateAttempts() {
        attempts++;
        lastAttemptedAt = Instant.now();
    }
}
