package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private CommentAuthorResponse author;
    private String text;
    private Instant createdAt;
    private Long likeCount;
    private Boolean likedByCurrentUser;
}
