package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.kafka.KafkaFeedProducer;
import com.crossfade.crossfade_backend.model.Follow;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.repo.FollowRepository;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopGenreRepository;
import com.crossfade.crossfade_backend.response.FollowingResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FriendsService {
    @Autowired
    FollowRepository repo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    UserTopGenreRepository userTopGenreRepo;

    @Autowired
    KafkaFeedProducer kafkaFeedProducer;

    public List<FollowingResponse> getfollowing(Long id) {
        List<Follow> follows = repo.findByFollower_Id(id);

        List<FollowingResponse> result = new ArrayList<>();
        for (Follow follow : follows) {
            User followee = follow.getFollowee();
            List<UserTopGenre> topGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(followee.getId());

            FollowingResponse response = new FollowingResponse();
            response.setUserId(followee.getId());
            response.setDisplayName(followee.getDisplayName());
            response.setHandle(followee.getHandle());
            response.setAvatarUrl(followee.getAvatarUrl());
            response.setTopGenre(topGenres.isEmpty() ? null : topGenres.get(0).getGenreName());

            result.add(response);
        }

        return result;
    }

    public List<FollowingResponse> searchUsers(String query, Long excludeUserId) {
        List<User> users = userRepo.searchByDisplayNameOrHandle(query, excludeUserId);

        List<FollowingResponse> result = new ArrayList<>();
        for (User user : users) {
            List<UserTopGenre> topGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(user.getId());

            FollowingResponse response = new FollowingResponse();
            response.setUserId(user.getId());
            response.setDisplayName(user.getDisplayName());
            response.setHandle(user.getHandle());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setTopGenre(topGenres.isEmpty() ? null : topGenres.get(0).getGenreName());

            result.add(response);
        }

        return result;
    }

    @Caching(evict = {
            @CacheEvict(value = "userProfile", key = "#followerId"),
            @CacheEvict(value = "userProfile", key = "#followeeId")
    })
    public void follow(Long followerId, Long followeeId) {
        if (repo.existsByFollower_IdAndFollowee_Id(followerId, followeeId)) return;

        User follower = userRepo.findById(followerId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + followerId));
        User followee = userRepo.findById(followeeId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + followeeId));

        Follow f = new Follow();
        f.setFollower(follower);
        f.setFollowee(followee);
        f.setCreatedAt(Instant.now());
        repo.save(f);

        kafkaFeedProducer.publishUserFollowed(followerId, followeeId);
    }

    @Caching(evict = {
            @CacheEvict(value = "userProfile", key = "#followerId"),
            @CacheEvict(value = "userProfile", key = "#followeeId")
    })
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        repo.deleteByFollower_IdAndFollowee_Id(followerId, followeeId);
    }
}
