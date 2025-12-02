package com.example.musicbooru.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "track")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String year;
    private String fileName;
    private boolean hasArtwork;
}
