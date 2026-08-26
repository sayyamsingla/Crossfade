package com.crossfade.crossfade_backend.controller;

import com.crossfade.crossfade_backend.model.Track;
import com.crossfade.crossfade_backend.response.PlaylistResponse;
import com.crossfade.crossfade_backend.response.TopArtistResponse;
import com.crossfade.crossfade_backend.response.TopGenreResponse;
import com.crossfade.crossfade_backend.response.TopTrackResponse;
import com.crossfade.crossfade_backend.response.UserProfileResponse;
import com.crossfade.crossfade_backend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    ProfileService service;

    @GetMapping("/users/{id}")
    public UserProfileResponse getUserById(@PathVariable Long id ) {
        return service.getUserProfile(id);

    }

    @GetMapping("/users/{id}/now-playing")
    public Track getNowPlaying(@PathVariable Long id) {
        return service.getNowPlayingTrack(id);
    }

    @GetMapping("/users/{id}/top-tracks")
    public List<TopTrackResponse> getTopTracks(@PathVariable Long id,
                                               @RequestParam String range,
                                               @RequestParam int limit) {
        return service.getUserTopTracks(id, range, limit);
    }

    @GetMapping("/users/{id}/top-artists")
    public List<TopArtistResponse> getTopArtists(@PathVariable Long id,
                                                  @RequestParam String range,
                                                  @RequestParam int limit) {
        return service.getUserTopArtists(id, range, limit);
    }

    @GetMapping("/users/{id}/top-genres")
    public List<TopGenreResponse> getTopGenres(@PathVariable Long id,
                                                @RequestParam int limit) {
        return service.getUserTopGenres(id, limit);
    }

    @GetMapping("/users/{id}/playlists")
    public List<PlaylistResponse> getPlaylists(@PathVariable Long id) {
        return service.getUserPlaylists(id);
    }

}
