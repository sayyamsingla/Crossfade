package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopTrackResponse {
    private Integer rank;
    private Long trackId;
    private String title;
    private String artist;
    private String coverUrl;
    private Integer playCount;
}