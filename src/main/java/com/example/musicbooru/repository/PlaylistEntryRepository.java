package com.example.musicbooru.repository;

import com.example.musicbooru.model.PlaylistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaylistEntryRepository  extends JpaRepository<PlaylistEntry, UUID> {
}