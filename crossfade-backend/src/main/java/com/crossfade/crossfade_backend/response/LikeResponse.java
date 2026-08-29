package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private Long likeCount;
    private Boolean likedByCurrentUser;
}
