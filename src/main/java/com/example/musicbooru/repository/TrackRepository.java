package com.example.musicbooru.repository;

import com.example.musicbooru.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, String> {
}