package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.kafka.KafkaFeedProducer;
import com.crossfade.crossfade_backend.model.Comment;
import com.crossfade.crossfade_backend.model.CommentLike;
import com.crossfade.crossfade_backend.model.Playlist;
import com.crossfade.crossfade_backend.model.PlaylistLike;
import com.crossfade.crossfade_backend.model.PlaylistTrack;
import com.crossfade.crossfade_backend.model.Track;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.model.UserTopTrack;
import com.crossfade.crossfade_backend.repo.CommentLikeRepository;
import com.crossfade.crossfade_backend.repo.CommentRepository;
import com.crossfade.crossfade_backend.repo.FollowRepository;
import com.crossfade.crossfade_backend.repo.PlaylistLikeRepository;
import com.crossfade.crossfade_backend.repo.PlaylistRepository;
import com.crossfade.crossfade_backend.repo.PlaylistTrackRepository;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopArtistRepository;
import com.crossfade.crossfade_backend.repo.UserTopGenreRepository;
import com.crossfade.crossfade_backend.repo.UserTopTrackRepository;
import com.crossfade.crossfade_backend.response.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileService {
    @Autowired
    UserRepository userRepo;
    @Autowired
    FollowRepository followRepo;

    @Autowired
    UserTopGenreRepository userTopGenreRepo;

    @Autowired
    UserTopTrackRepository userTopTrackRepo;

    @Autowired
    UserTopArtistRepository userTopArtistRepo;

    @Autowired
    PlaylistRepository playlistRepo;

    @Autowired
    PlaylistTrackRepository playlistTrackRepo;

    @Autowired
    CommentRepository commentRepo;

    @Autowired
    CommentLikeRepository commentLikeRepo;

    @Autowired
    PlaylistLikeRepository playlistLikeRepo;

    @Autowired
    KafkaFeedProducer kafkaFeedProducer;

    @Autowired
    @Lazy
    private ProfileService self;


    @Cacheable("userProfile")
    public UserProfileResponse getUserProfile(Long id) {
        User user = userRepo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + id));
        Long followerCount = followRepo.countByFollowee_Id(id);
        Long followingCount = followRepo.countByFollower_Id(id);
        List<UserTopGenre> topGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(id);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setDisplayName(user.getDisplayName());
        response.setHandle(user.getHandle());
        response.setBio(user.getBio());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setFollowersCount(followerCount);
        response.setFollowingCount(followingCount);
        response.setTopGenre(topGenres.isEmpty() ? null : topGenres.get(0).getGenreName());
        response.setHasChosenHandle(user.isHasChosenHandle());

        return response;
    }


    @CacheEvict(value = "userProfile", key = "#userId")
    @Transactional
    public UserProfileResponse setHandle(Long userId, String rawHandle) {
        String handle = rawHandle == null ? "" : rawHandle.trim();
        if (!handle.matches("^[A-Za-z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("Handle must be 3 to 20 letters, numbers, or underscores.");
        }
        String withAt = "@" + handle;
        if (userRepo.existsByHandleIgnoreCaseAndIdNot(withAt, userId)) {
            throw new IllegalArgumentException("That handle is already taken.");
        }
        User user = userRepo.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + userId));
        user.setHandle(withAt);
        user.setHasChosenHandle(true);
        userRepo.save(user);
        return getUserProfile(userId);
    }

    public Track getNowPlayingTrack(Long id) {
        User user = userRepo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + id));
       return user.getNowPlayingTrack();

    }

    @Cacheable("topTracks")
    public List<TopTrackResponse> getUserTopTracks(Long id, String range, int limit) {
        List<UserTopTrack> topTracks = userTopTrackRepo.findByUser_IdOrderByRankAsc(id);

        List<TopTrackResponse> result = new ArrayList<>();
        int count = 0;

        for (UserTopTrack utt : topTracks) {
            if (count >= limit) break;

            TopTrackResponse response = new TopTrackResponse();
            response.setRank(utt.getRank());
            response.setTrackId(utt.getTrack().getId());
            response.setTitle(utt.getTrack().getTitle());
            response.setArtist(utt.getTrack().getArtist().getName());
            response.setCoverUrl(utt.getTrack().getCoverUrl());
            response.setPlayCount(utt.getPlayCount());

            result.add(response);
            count++;
        }

        return result;


    }
    @Cacheable("topArtists")
    public List<TopArtistResponse> getUserTopArtists(Long id, String range, int limit) {
        List<UserTopArtist> topArtists = userTopArtistRepo.findByUser_IdOrderByRankAsc(id);

        List<TopArtistResponse> result = new ArrayList<>();
        int count = 0;

        for (UserTopArtist uta : topArtists) {
            if (count >= limit) break;

            TopArtistResponse response = new TopArtistResponse();
            response.setRank(uta.getRank());
            response.setArtistId(uta.getArtist().getId());
            response.setName(uta.getArtist().getName());
            response.setImageUrl(uta.getArtist().getImageUrl());

            result.add(response);
            count++;
        }

        return result;
    }

    @Cacheable("topGenres")
    public List<TopGenreResponse> getUserTopGenres(Long id, int limit) {
        List<UserTopGenre> topGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(id);

        List<TopGenreResponse> result = new ArrayList<>();
        int count = 0;

        for (UserTopGenre utg : topGenres) {
            if (count >= limit) break;

            TopGenreResponse response = new TopGenreResponse();
            response.setName(utg.getGenreName());
            response.setPercentage(utg.getPercentage());

            result.add(response);
            count++;
        }
        return result;
    }

    @Cacheable("playlists")
    public List<PlaylistResponse> getUserPlaylistsBase(Long id) {
        List<Playlist> playlists = playlistRepo.findByOwner_Id(id);

        List<PlaylistResponse> result = new ArrayList<>();

        for (Playlist playlist : playlists) {
            PlaylistResponse response = new PlaylistResponse();
            response.setPlaylistId(playlist.getId());
            response.setTitle(playlist.getTitle());
            response.setTrackCount(playlistTrackRepo.countByPlaylist_Id(playlist.getId()));
            response.setCoverUrl(playlist.getCoverUrl());

            result.add(response);
        }

        return result;
    }

    public List<PlaylistResponse> getUserPlaylists(Long id, Long viewerId) {
        List<PlaylistResponse> base = self.getUserPlaylistsBase(id);

        List<PlaylistResponse> result = new ArrayList<>();

        for (PlaylistResponse cached : base) {
            PlaylistResponse response = new PlaylistResponse();
            response.setPlaylistId(cached.getPlaylistId());
            response.setTitle(cached.getTitle());
            response.setTrackCount(cached.getTrackCount());
            response.setCoverUrl(cached.getCoverUrl());
            response.setLikeCount(playlistLikeRepo.countByPlaylist_Id(cached.getPlaylistId()));
            response.setLikedByCurrentUser(viewerId != null && playlistLikeRepo.existsByPlaylist_IdAndUser_Id(cached.getPlaylistId(), viewerId));

            result.add(response);
        }

        return result;
    }

    @Cacheable("playlistTracks")
    public List<PlaylistTrackResponse> getPlaylistTracks(Long playlistId) {
        List<PlaylistTrack> playlistTracks = playlistTrackRepo.findByPlaylist_IdOrderByPositionAsc(playlistId);

        List<PlaylistTrackResponse> result = new ArrayList<>();

        for (PlaylistTrack pt : playlistTracks) {
            PlaylistTrackResponse response = new PlaylistTrackResponse();
            response.setPosition(pt.getPosition());
            response.setTrackId(pt.getTrack().getId());
            response.setTitle(pt.getTrack().getTitle());
            response.setArtist(pt.getTrack().getArtist().getName());
            response.setCoverUrl(pt.getTrack().getCoverUrl());

            result.add(response);
        }

        return result;
    }

    public PostCommentResponse addCommentToProfile(Long authorId, String text, Long receiverId) {
        User author = userRepo.findById(authorId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + authorId));
        User receiver = userRepo.findById(receiverId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + receiverId));

        Comment comment = new Comment();
        comment.setText(text);
        comment.setCommentator(author);
        comment.setReceiver(receiver);
        comment.setCreatedAt(Instant.now());

        commentRepo.save(comment);

        kafkaFeedProducer.publishCommentPosted(authorId, receiverId, text);

        return new PostCommentResponse(authorId, text);
    }

    public List<CommentResponse> getCommentsForProfile(Long receiverId, Long viewerId) {
        List<Comment> comments = commentRepo.findByReceiver_IdOrderByCreatedAtDesc(receiverId);

        List<CommentResponse> result = new ArrayList<>();

        for (Comment comment : comments) {
            User author = comment.getCommentator();

            CommentAuthorResponse authorResponse = new CommentAuthorResponse();
            authorResponse.setUserId(author.getId());
            authorResponse.setDisplayName(author.getDisplayName());
            authorResponse.setAvatarUrl(author.getAvatarUrl());

            CommentResponse response = new CommentResponse();
            response.setCommentId(comment.getId());
            response.setAuthor(authorResponse);
            response.setText(comment.getText());
            response.setCreatedAt(comment.getCreatedAt());
            response.setLikeCount(commentLikeRepo.countByComment_Id(comment.getId()));
            response.setLikedByCurrentUser(viewerId != null && commentLikeRepo.existsByComment_IdAndUser_Id(comment.getId(), viewerId));

            result.add(response);
        }

        return result;
    }

    public LikeResponse likeComment(Long commentId, Long userId) {
        Comment comment = commentRepo.findById(commentId).orElseThrow(() ->
                new EntityNotFoundException("Comment Not Found " + commentId));
        User user = userRepo.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + userId));

        if (!commentLikeRepo.existsByComment_IdAndUser_Id(commentId, userId)) {
            CommentLike like = new CommentLike();
            like.setComment(comment);
            like.setUser(user);
            commentLikeRepo.save(like);
        }

        return new LikeResponse(commentLikeRepo.countByComment_Id(commentId), true);
    }

    @Transactional
    public LikeResponse unlikeComment(Long commentId, Long userId) {
        if (!commentRepo.existsById(commentId)) {
            throw new EntityNotFoundException("Comment Not Found " + commentId);
        }

        commentLikeRepo.deleteByComment_IdAndUser_Id(commentId, userId);

        return new LikeResponse(commentLikeRepo.countByComment_Id(commentId), false);
    }

    public LikeResponse likePlaylist(Long playlistId, Long userId) {
        Playlist playlist = playlistRepo.findById(playlistId).orElseThrow(() ->
                new EntityNotFoundException("Playlist Not Found " + playlistId));
        User user = userRepo.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + userId));

        if (!playlistLikeRepo.existsByPlaylist_IdAndUser_Id(playlistId, userId)) {
            PlaylistLike like = new PlaylistLike();
            like.setPlaylist(playlist);
            like.setUser(user);
            playlistLikeRepo.save(like);
            kafkaFeedProducer.publishPlaylistLiked(userId, playlistId);
        }

        return new LikeResponse(playlistLikeRepo.countByPlaylist_Id(playlistId), true);
    }

    @Transactional
    public LikeResponse unlikePlaylist(Long playlistId, Long userId) {
        if (!playlistRepo.existsById(playlistId)) {
            throw new EntityNotFoundException("Playlist Not Found " + playlistId);
        }

        playlistLikeRepo.deleteByPlaylist_IdAndUser_Id(playlistId, userId);

        return new LikeResponse(playlistLikeRepo.countByPlaylist_Id(playlistId), false);
    }
}
