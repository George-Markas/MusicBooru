package com.example.musicbooru.repository;

import com.example.musicbooru.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository extends JpaRepository<Track, UUID> {

    boolean existsByFileName(String fileName);

    interface FileNameOnly {
        String getFileName();
    }

    Optional<FileNameOnly> findProjectedById(UUID trackId);

    @Query(value = """
            SELECT * FROM track
            WHERE similarity(COALESCE(title, ''), :query) > 0.2
               OR similarity(COALESCE(artist, ''), :query) > 0.2
               OR similarity(COALESCE(album, ''), :query) > 0.2
            ORDER BY
                GREATEST(
                    similarity(COALESCE(title, ''), :query),
                    similarity(COALESCE(artist, ''), :query),
                    similarity(COALESCE(album, ''), :query)
                ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Track> searchTracks(@Param("query") String query, @Param("limit") int characterLimit);
}