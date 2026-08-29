package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemResponse {
    private Long feedId;
    private String type;
    private CommentAuthorResponse actor;
    private String verb;
    private String target;
    private String subtitle;
    private Instant createdAt;
}
