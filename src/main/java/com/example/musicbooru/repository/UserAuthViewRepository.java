package com.example.musicbooru.repository;

import com.example.musicbooru.model.UserAuthView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthViewRepository extends JpaRepository<UserAuthView, Integer> {
    Optional<UserAuthView> findByUsername(String username);
}