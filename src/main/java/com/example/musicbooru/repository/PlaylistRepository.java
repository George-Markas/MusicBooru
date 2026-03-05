package com.example.musicbooru.repository;

import com.example.musicbooru.model.Playlist;
import com.example.musicbooru.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    List<Playlist> findByOwner(User owner);
}
