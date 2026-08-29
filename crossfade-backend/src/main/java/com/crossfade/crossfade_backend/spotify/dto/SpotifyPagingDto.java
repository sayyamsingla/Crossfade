package com.crossfade.crossfade_backend.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyPagingDto<T>(List<T> items) {
}
