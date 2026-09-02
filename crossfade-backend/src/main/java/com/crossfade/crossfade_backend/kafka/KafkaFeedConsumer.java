package com.crossfade.crossfade_backend.kafka;

import com.crossfade.crossfade_backend.model.Playlist;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.repo.PlaylistRepository;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaFeedConsumer {

    @Autowired
    private FeedService feedService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlaylistRepository playlistRepository;

    @KafkaListener(topics = "feed-events", concurrency = "3")
    public void handleFeedEvent(FeedEventMessage feedEventMessage) {
        User actor = userRepository.findById(feedEventMessage.getActorId()).orElseThrow();

        switch (feedEventMessage.getType()) {
            case FOLLOWED -> {
                User followee = userRepository.findById(feedEventMessage.getTargetUserId()).orElseThrow();
                feedService.recordFollow(actor, followee);
            }
            case LIKED_PLAYLIST -> {
                Playlist playlist = playlistRepository.findById(feedEventMessage.getTargetUserId()).orElseThrow();
                feedService.recordLikePlaylist(actor, playlist);
            }
            case COMMENTED -> {
                User receiver = userRepository.findById(feedEventMessage.getTargetUserId()).orElseThrow();
                feedService.recordComment(actor, receiver, feedEventMessage.getText());
            }
            default -> throw new IllegalArgumentException("Unhandled feed event type: " + feedEventMessage.getType());
        }
    }
}
