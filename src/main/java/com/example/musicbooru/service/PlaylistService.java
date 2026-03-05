package com.example.musicbooru.service;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.Playlist;
import com.example.musicbooru.model.Track;
import com.example.musicbooru.model.User;
import com.example.musicbooru.repository.PlaylistRepository;
import com.example.musicbooru.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final static Logger logger = LoggerFactory.getLogger(PlaylistService.class);

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;

    public List<Playlist> getPlaylistsByOwner(User owner) {
        return playlistRepository.findByOwner(owner);
    }

    public Playlist getPlaylist(String playlistId, User requester) {
        Playlist playlist = playlistRepository.findById(UUID.fromString(playlistId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist '" + playlistId + "' not found"));

        if (!playlist.getOwner().getId().equals(requester.getId())) {
            throw new GenericException("You do not own this playlist", HttpStatus.UNAUTHORIZED);
        }

        return playlist;
    }

    public Playlist createPlaylist(User owner, String name) {
        Playlist playlist = Playlist.builder()
                .name(name)
                .owner(owner)
                .build();

        return playlistRepository.save(playlist);
    }

    public Playlist addTrackToPlaylist(String playlistId, String trackId, User requester) {
        Playlist playlist = getPlaylist(playlistId, requester);

        Track track = trackRepository.findById(UUID.fromString(trackId))
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));

        playlist.getTracks().add(track);

        return playlistRepository.save(playlist);
    }

    public Playlist removeTrackFromPlaylist(String playlistId, String trackId, User requester) {
        Playlist playlist = getPlaylist(playlistId, requester);

        playlist.getTracks().removeIf(track -> track.getId().equals(UUID.fromString(trackId)));

        return playlistRepository.save(playlist);
    }

    public void deletePlaylist(String playlistId, User requester) {
        playlistRepository.delete(getPlaylist(playlistId, requester));
        logger.info("Deleted playlist '{}'", playlistId);
    }
}
