package com.crossfade.crossfade_backend.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyPlaylistTrackItemDto(SpotifyTrackDto item, SpotifyTrackDto track) {
    public SpotifyTrackDto resolvedTrack() {
        return item != null ? item : track;
    }
}
