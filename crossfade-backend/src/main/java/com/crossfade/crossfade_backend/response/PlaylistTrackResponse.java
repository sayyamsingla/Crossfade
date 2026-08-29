package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistTrackResponse {
    private Integer position;
    private Long trackId;
    private String title;
    private String artist;
    private String coverUrl;
}
