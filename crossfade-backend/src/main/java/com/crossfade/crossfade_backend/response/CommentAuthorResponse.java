package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentAuthorResponse {
    private Long userId;
    private String displayName;
    private String avatarUrl;
}
