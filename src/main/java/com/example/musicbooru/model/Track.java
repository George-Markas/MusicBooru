package com.example.musicbooru.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "track")
public class Track {
    @Id
    private UUID id;

    private String title;
    private String artist;
    private String album;
    private String genre;
    private String year;

    @Column(unique = true)
    private String fileName;
}
