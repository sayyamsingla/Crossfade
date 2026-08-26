package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.model.Playlist;
import com.crossfade.crossfade_backend.model.Track;
import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.model.UserTopTrack;
import com.crossfade.crossfade_backend.repo.FollowRepository;
import com.crossfade.crossfade_backend.repo.PlaylistRepository;
import com.crossfade.crossfade_backend.repo.PlaylistTrackRepository;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopArtistRepository;
import com.crossfade.crossfade_backend.repo.UserTopGenreRepository;
import com.crossfade.crossfade_backend.repo.UserTopTrackRepository;
import com.crossfade.crossfade_backend.response.PlaylistResponse;
import com.crossfade.crossfade_backend.response.TopArtistResponse;
import com.crossfade.crossfade_backend.response.TopGenreResponse;
import com.crossfade.crossfade_backend.response.TopTrackResponse;
import com.crossfade.crossfade_backend.response.UserProfileResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        return response;
    }

    public Track getNowPlayingTrack(Long id) {
        User user = userRepo.findById(id).orElseThrow(() ->
                new EntityNotFoundException("User Not Found " + id));
       return user.getNowPlayingTrack();

    }

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

    public List<PlaylistResponse> getUserPlaylists(Long id) {
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
}
