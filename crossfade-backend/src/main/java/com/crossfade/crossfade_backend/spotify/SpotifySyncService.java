package com.crossfade.crossfade_backend.spotify;

import com.crossfade.crossfade_backend.model.Artist;
import com.crossfade.crossfade_backend.model.Playlist;
import com.crossfade.crossfade_backend.model.PlaylistTrack;
import com.crossfade.crossfade_backend.model.SyncLog;
import com.crossfade.crossfade_backend.model.Track;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.model.UserTopTrack;
import com.crossfade.crossfade_backend.repo.ArtistRepository;
import com.crossfade.crossfade_backend.repo.PlaylistRepository;
import com.crossfade.crossfade_backend.repo.PlaylistTrackRepository;
import com.crossfade.crossfade_backend.repo.SyncLogRepository;
import com.crossfade.crossfade_backend.repo.TrackRepository;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopArtistRepository;
import com.crossfade.crossfade_backend.repo.UserTopGenreRepository;
import com.crossfade.crossfade_backend.repo.UserTopTrackRepository;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyArtistDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyImageDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyPlaylistDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyPlaylistTrackItemDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyProfileDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyTrackDto;
import com.crossfade.crossfade_backend.response.SyncStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpotifySyncService {

    private static final Logger log = LoggerFactory.getLogger(SpotifySyncService.class);

    @Autowired
    private SpotifyApiClient spotifyApiClient;
    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private TrackRepository trackRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private UserTopArtistRepository userTopArtistRepository;
    @Autowired
    private UserTopTrackRepository userTopTrackRepository;
    @Autowired
    private UserTopGenreRepository userTopGenreRepository;
    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;
    @Autowired
    private SyncLogRepository syncLogRepository;

    private static final int MAX_SYNCS_PER_WINDOW = 3;
    private static final Duration SYNC_WINDOW = Duration.ofDays(7);

    @Transactional
    public void syncUser(Long localUserId, Authentication authentication) {
        Instant windowStart = Instant.now().minus(SYNC_WINDOW);
        List<SyncLog> recentSyncs = syncLogRepository
                .findByUser_IdAndSyncedAtAfterOrderBySyncedAtAsc(localUserId, windowStart);
        if (recentSyncs.size() >= MAX_SYNCS_PER_WINDOW) {
            Instant nextAvailableAt = recentSyncs.get(0).getSyncedAt().plus(SYNC_WINDOW);
            throw new SyncQuotaExceededException(nextAvailableAt);
        }

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("spotify")
                .principal(authentication)
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        User user = userRepository.findById(localUserId).orElseThrow();

        syncProfile(user, authorizedClient);
        List<SpotifyArtistDto> topArtists = syncTopArtists(user, authorizedClient);
        syncTopGenres(user, topArtists);
        syncTopTracks(user, authorizedClient);
        syncPlaylists(user, authorizedClient);

        syncLogRepository.save(new SyncLog(null, user, Instant.now()));
    }

    public SyncStatusResponse getSyncStatus(Long localUserId) {
        Instant windowStart = Instant.now().minus(SYNC_WINDOW);
        List<SyncLog> recentSyncs = syncLogRepository
                .findByUser_IdAndSyncedAtAfterOrderBySyncedAtAsc(localUserId, windowStart);
        Instant nextAvailableAt = recentSyncs.size() >= MAX_SYNCS_PER_WINDOW
                ? recentSyncs.get(0).getSyncedAt().plus(SYNC_WINDOW)
                : null;
        return new SyncStatusResponse(recentSyncs.size(), MAX_SYNCS_PER_WINDOW, nextAvailableAt);
    }

    private void syncProfile(User user, OAuth2AuthorizedClient authorizedClient) {
        SpotifyProfileDto profile = spotifyApiClient.fetchProfile(authorizedClient);
        user.setDisplayName(profile.display_name());
        user.setAvatarUrl(firstImageUrl(profile.images()));
        userRepository.save(user);
    }

    private List<SpotifyArtistDto> syncTopArtists(User user, OAuth2AuthorizedClient authorizedClient) {
        List<SpotifyArtistDto> topArtists = spotifyApiClient.fetchTopArtists(authorizedClient, "medium_term", 50).items();

        userTopArtistRepository.deleteByUser_Id(user.getId());
        int rank = 1;
        for (SpotifyArtistDto dto : topArtists) {
            Artist artist = upsertArtist(dto);
            UserTopArtist userTopArtist = new UserTopArtist();
            userTopArtist.setUser(user);
            userTopArtist.setArtist(artist);
            userTopArtist.setRank(rank++);
            userTopArtistRepository.save(userTopArtist);
        }
        return topArtists;
    }

    private void syncTopGenres(User user, List<SpotifyArtistDto> topArtists) {
        Map<String, Integer> genreCounts = new LinkedHashMap<>();
        int totalGenreMentions = 0;
        for (SpotifyArtistDto artist : topArtists) {
            log.info("Artist {} genres from Spotify: {}", artist.name(), artist.genres());
            if (artist.genres() == null) {
                continue;
            }
            for (String genre : artist.genres()) {
                genreCounts.merge(genre, 1, Integer::sum);
                totalGenreMentions++;
            }
        }

        userTopGenreRepository.deleteByUser_Id(user.getId());
        if (totalGenreMentions == 0) {
            return;
        }

        int finalTotalGenreMentions = totalGenreMentions;
        genreCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(entry -> {
                    UserTopGenre userTopGenre = new UserTopGenre();
                    userTopGenre.setUser(user);
                    userTopGenre.setGenreName(entry.getKey());
                    userTopGenre.setPercentage(Math.round(100f * entry.getValue() / finalTotalGenreMentions));
                    userTopGenreRepository.save(userTopGenre);
                });
    }

    private void syncTopTracks(User user, OAuth2AuthorizedClient authorizedClient) {
        List<SpotifyTrackDto> topTracks = spotifyApiClient.fetchTopTracks(authorizedClient, "medium_term", 50).items();

        userTopTrackRepository.deleteByUser_Id(user.getId());
        int rank = 1;
        for (SpotifyTrackDto dto : topTracks) {
            Track track = upsertTrack(dto);
            UserTopTrack userTopTrack = new UserTopTrack();
            userTopTrack.setUser(user);
            userTopTrack.setTrack(track);
            userTopTrack.setRank(rank++);
            userTopTrack.setPlayCount(null);
            userTopTrackRepository.save(userTopTrack);
        }
    }

    private void syncPlaylists(User user, OAuth2AuthorizedClient authorizedClient) {
        List<SpotifyPlaylistDto> playlists = spotifyApiClient.fetchOwnPlaylists(authorizedClient).items();

        for (SpotifyPlaylistDto dto : playlists) {
            boolean ownedByUser = dto.owner() != null
                    && dto.owner().id() != null
                    && dto.owner().id().equals(user.getSpotifyUserId());
            if (!ownedByUser) {
                log.info("Skipping playlist {} ({}) — not owned by this user (owner={})",
                        dto.name(), dto.id(), dto.owner() != null ? dto.owner().id() : "null");
                continue;
            }

            Playlist playlist = upsertPlaylist(dto, user);
            try {
                syncPlaylistTracks(playlist, authorizedClient);
            } catch (Exception e) {
                log.warn("Skipping tracks for playlist {} ({}): {}", playlist.getTitle(), playlist.getSpotifyId(), e.getMessage());
            }
        }
    }

    private void syncPlaylistTracks(Playlist playlist, OAuth2AuthorizedClient authorizedClient) {
        List<SpotifyPlaylistTrackItemDto> items =
                spotifyApiClient.fetchPlaylistTracks(authorizedClient, playlist.getSpotifyId()).items();

        playlistTrackRepository.deleteByPlaylist_Id(playlist.getId());
        int position = 1;
        for (SpotifyPlaylistTrackItemDto item : items) {
            if (item.resolvedTrack() == null) {
                continue;
            }
            Track track = upsertTrack(item.resolvedTrack());
            PlaylistTrack playlistTrack = new PlaylistTrack();
            playlistTrack.setPlaylist(playlist);
            playlistTrack.setTrack(track);
            playlistTrack.setPosition(position++);
            playlistTrackRepository.save(playlistTrack);
        }
    }

    private Artist upsertArtist(SpotifyArtistDto dto) {
        Artist artist = artistRepository.findBySpotifyId(dto.id()).orElseGet(Artist::new);
        artist.setSpotifyId(dto.id());
        artist.setName(dto.name());
        String imageUrl = firstImageUrl(dto.images());
        if (imageUrl != null) {
            artist.setImageUrl(imageUrl);
        }
        return artistRepository.save(artist);
    }

    private Track upsertTrack(SpotifyTrackDto dto) {
        Track track = trackRepository.findBySpotifyId(dto.id()).orElseGet(Track::new);
        track.setSpotifyId(dto.id());
        track.setTitle(dto.name());
        track.setCoverUrl(dto.album() != null ? firstImageUrl(dto.album().images()) : null);
        if (!dto.artists().isEmpty()) {
            track.setArtist(upsertArtist(dto.artists().get(0)));
        }
        return trackRepository.save(track);
    }

    private Playlist upsertPlaylist(SpotifyPlaylistDto dto, User owner) {
        Playlist playlist = playlistRepository.findBySpotifyId(dto.id()).orElseGet(Playlist::new);
        playlist.setSpotifyId(dto.id());
        playlist.setTitle(dto.name());
        playlist.setCoverUrl(firstImageUrl(dto.images()));
        playlist.setOwner(owner);
        return playlistRepository.save(playlist);
    }

    private String firstImageUrl(List<SpotifyImageDto> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0).url();
    }
}
