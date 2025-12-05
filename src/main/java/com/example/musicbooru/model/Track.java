package com.example.musicbooru.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static com.example.musicbooru.util.Commons.AUDIO_EXTENSION;

@Entity
@Table(name = "track")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @PrePersist
    public void prePersist() {
        if(this.id == null) this.id = UUID.randomUUID();
        if(this.fileName == null) this.fileName = this.id + AUDIO_EXTENSION;
    }
}
