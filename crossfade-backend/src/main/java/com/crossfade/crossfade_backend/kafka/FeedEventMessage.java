package com.crossfade.crossfade_backend.kafka;

import com.crossfade.crossfade_backend.model.FeedEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class FeedEventMessage {
    private FeedEventType type;
    private Long actorId;
    private Long targetUserId; // or playlistId, depending on type
    private String text; // comment text, only set for COMMENTED events
    private Instant occurredAt;

}