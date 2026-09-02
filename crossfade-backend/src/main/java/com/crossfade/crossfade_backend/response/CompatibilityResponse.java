package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityResponse {
    private int score;
    private String caption;
    private List<String> sharedArtists;
    private List<String> sharedTracks;
}
