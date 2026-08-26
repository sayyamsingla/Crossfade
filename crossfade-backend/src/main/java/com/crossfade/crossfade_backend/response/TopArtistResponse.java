package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopArtistResponse {
    private Integer rank;
    private Long artistId;
    private String name;
    private String imageUrl;
}
