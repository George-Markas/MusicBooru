package org.example.musicbooru.track;

import jakarta.persistence.*;
import org.example.musicbooru.util.PublicIdGenerator;

@Entity
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7, columnDefinition = "bpchar(7)")
    private String publicId;

    private String artist;
    private String title;
    private String album;
    private String year;
    private String genre;
    private int duration; // In seconds

    @Column(nullable = false, unique = true)
    private String filename;

    @Column(nullable = false)
    private String mimeType;

    @PrePersist
    private void generatePublicId() {
        this.publicId = PublicIdGenerator.generate();
    }
}
