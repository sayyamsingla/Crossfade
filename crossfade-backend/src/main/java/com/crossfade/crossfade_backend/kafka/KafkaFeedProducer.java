package com.crossfade.crossfade_backend.kafka;

import com.crossfade.crossfade_backend.model.FeedEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


import java.time.Instant;

@Service
public class KafkaFeedProducer {

    private static final String TOPIC = "feed-events";

    @Autowired
    private KafkaTemplate<String, FeedEventMessage> kafkaTemplate;

    public void publishUserFollowed(Long followerId, Long followeeId) {
        FeedEventMessage feedEventMessage = new FeedEventMessage(FeedEventType.FOLLOWED, followerId, followeeId, null, Instant.now());
        kafkaTemplate.send(TOPIC, followerId.toString(), feedEventMessage);
    }

    public void publishPlaylistLiked(Long userId, Long playlistId) {
        FeedEventMessage feedEventMessage = new FeedEventMessage(FeedEventType.LIKED_PLAYLIST, userId, playlistId, null, Instant.now());
        kafkaTemplate.send(TOPIC, userId.toString(), feedEventMessage);
    }

    public void publishCommentPosted(Long commentatorId, Long receiverId, String text) {
        FeedEventMessage feedEventMessage = new FeedEventMessage(FeedEventType.COMMENTED, commentatorId, receiverId, text, Instant.now());
        kafkaTemplate.send(TOPIC, commentatorId.toString(), feedEventMessage);
    }
}

