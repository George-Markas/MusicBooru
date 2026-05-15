package com.example.musicbooru.track;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    boolean existsByPublicId(String publicId);

    Optional<Track> findByPublicId(String publicId);

    void deleteByStatus(TrackStatus status);
}
