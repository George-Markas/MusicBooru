package com.example.musicbooru.dto;

import com.example.musicbooru.model.Playlist;

import java.util.List;

public record PlaylistResponse(
        String id,
        String name,
        String ownerId,
        List<PlaylistEntryResponse> entries
) {
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId().toString(),
                playlist.getName(),
                playlist.getOwner().getId().toString(),
                playlist.getEntries().stream()
                        .map(PlaylistEntryResponse::from)
                        .toList()
        );
    }
}