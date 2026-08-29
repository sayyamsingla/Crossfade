package com.crossfade.crossfade_backend.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyPlaylistOwnerDto(String id, String display_name) {
}
