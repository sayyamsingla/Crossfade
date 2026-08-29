package com.crossfade.crossfade_backend.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyArtistDto(String id, String name, List<String> genres, List<SpotifyImageDto> images) {
}
