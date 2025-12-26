package com.example.musicbooru.repository;

import com.example.musicbooru.model.UserAuthView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthViewRepository extends JpaRepository<UserAuthView, UUID> {
    Optional<UserAuthView> findByUsername(String username);
}