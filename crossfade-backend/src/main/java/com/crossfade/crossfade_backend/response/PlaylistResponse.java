package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {
    private Long playlistId;
    private String title;
    private Long trackCount;
    private String coverUrl;
    private Long likeCount;
    private Boolean likedByCurrentUser;
}
