package com.example.musicbooru.service;

import com.example.musicbooru.dto.PlaylistResponse;
import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.model.*;
import com.example.musicbooru.repository.PlaylistEntryRepository;
import com.example.musicbooru.repository.PlaylistRepository;
import com.example.musicbooru.repository.TrackRepository;
import com.example.musicbooru.util.ContentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.musicbooru.util.Commons.ICON;
import static com.example.musicbooru.util.Commons.ICON_EXTENSION;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistEntryRepository playlistEntryRepository;
    private final TrackRepository trackRepository;

    private Playlist getPlaylist(String playlistId, User requester) {
        Playlist playlist = playlistRepository.findById(UUID.fromString(playlistId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist '" + playlistId + "' not found"));

        if (!playlist.getOwner().getId().equals(requester.getId())) {
            throw new GenericException("You do not own this playlist", HttpStatus.UNAUTHORIZED);
        }

        return playlist;
    }

    public boolean playlistExists(String playlistId, User requester) {
        Optional<Playlist> playlist = playlistRepository.findById(UUID.fromString(playlistId));
        return playlist.isPresent() && (playlist.get().getOwner().getId().equals(requester.getId())
                || requester.getRole().equals(Role.ADMIN));
    }

    public List<Playlist> getPlaylistsByOwner(User owner) {
        return playlistRepository.findByOwner(owner);
    }

    public Playlist createPlaylist(User owner, String name) {
        Playlist playlist = Playlist.builder()
                .name(name)
                .owner(owner)
                .build();

        return playlistRepository.save(playlist);
    }

    public PlaylistResponse addTrack(String playlistId, String trackId, User requester) {
        Playlist playlist = getPlaylist(playlistId, requester);

        Track track = trackRepository.findById(UUID.fromString(trackId))
                .orElseThrow(() -> new ResourceNotFoundException("Track '" + trackId + "' not found"));

        PlaylistEntry entry = PlaylistEntry.builder()
                .playlist(playlist)
                .track(track)
                .addedOn(Instant.now())
                .build();

        playlist.getEntries().add(entry);

        return PlaylistResponse.from(playlistRepository.save(playlist));
    }

    public void removeTrack(String playlistId, String entryId, User requester) {
        Playlist playlist = getPlaylist(playlistId, requester);

        PlaylistEntry entry = playlistEntryRepository.findById(UUID.fromString(entryId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist entry '" + entryId + "' not found"));

        playlist.getEntries().remove(entry);
        playlistEntryRepository.delete(entry);
        playlistRepository.save(playlist);
    }

    public void addIcon(MultipartFile file, String playlistId, User requester) {
        if (playlistExists(playlistId, requester)) {
            try {
                BufferedImage bufferedImage = ContentUtils.convertToJpeg(file);
                ImageIO.write(bufferedImage, "jpg", new File(ICON + playlistId + ICON_EXTENSION));
            } catch (IOException e) {
                throw new GenericException("Image I/O error");
            }
        } else {
            throw new GenericException("Could not set icon for playlist");
        }
    }

    public void removeIcon(String playlistId, User requester) {
        if (playlistExists(playlistId, requester)) {
            try {
                Files.deleteIfExists(Paths.get(ICON + playlistId + ICON_EXTENSION));
            } catch (IOException e) {
                throw new GenericException("Could not delete playlist icon");
            }
        }
    }

    public void deletePlaylist(String playlistId, User requester) {
        playlistRepository.delete(getPlaylist(playlistId, requester));
        removeIcon(playlistId, requester);
    }
}
