package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.model.FeedEvent;
import com.crossfade.crossfade_backend.model.FeedEventType;
import com.crossfade.crossfade_backend.model.Playlist;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.repo.FeedEventRepository;
import com.crossfade.crossfade_backend.repo.FollowRepository;
import com.crossfade.crossfade_backend.response.CommentAuthorResponse;
import com.crossfade.crossfade_backend.response.FeedItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FeedService {
    @Autowired
    FollowRepository followRepo;

    @Autowired
    FeedEventRepository feedEventRepo;

    public List<FeedItemResponse> getFeed(Long userId) {
        List<Long> followeeIds = followRepo.findByFollower_Id(userId)
                .stream().map(f -> f.getFollowee().getId()).toList();
        if (followeeIds.isEmpty()) return List.of();

        return feedEventRepo.findByActor_IdInOrderByCreatedAtDesc(followeeIds)
                .stream().map(this::toResponse).toList();
    }

    private FeedItemResponse toResponse(FeedEvent e) {
        User actor = e.getActor();
        CommentAuthorResponse actorResp = new CommentAuthorResponse(
                actor.getId(), actor.getDisplayName(), actor.getAvatarUrl());
        return new FeedItemResponse(e.getId(), e.getType().name(), actorResp,
                e.getVerb(), e.getTarget(), e.getSubtitle(), e.getCreatedAt());
    }

    public void recordFollow(User follower, User followee) {
        feedEventRepo.save(new FeedEvent(null, follower, FeedEventType.FOLLOWED,
                "started following", followee.getDisplayName(), null, Instant.now()));
    }

    public void recordComment(User commentator, User receiver, String text) {
        feedEventRepo.save(new FeedEvent(null, commentator, FeedEventType.COMMENTED,
                "commented on", receiver.getDisplayName() + "'s profile", text, Instant.now()));
    }

    public void recordLikePlaylist(User liker, Playlist playlist) {
        feedEventRepo.save(new FeedEvent(null, liker, FeedEventType.LIKED_PLAYLIST,
                "liked", playlist.getTitle(), null, Instant.now()));
    }
}
