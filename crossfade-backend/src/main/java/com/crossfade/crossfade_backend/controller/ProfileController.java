package com.crossfade.crossfade_backend.controller;

import com.crossfade.crossfade_backend.model.Comment;
import com.crossfade.crossfade_backend.model.Track;
import com.crossfade.crossfade_backend.request.CommentRequest;
import com.crossfade.crossfade_backend.request.SetHandleRequest;
import com.crossfade.crossfade_backend.response.*;
import com.crossfade.crossfade_backend.security.CrossfadeOAuth2User;
import com.crossfade.crossfade_backend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    ProfileService service;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal CrossfadeOAuth2User principal) {
        return service.getUserProfile(principal.getLocalUserId());
    }

    @GetMapping("/users/{id}")
    public UserProfileResponse getUserById(@PathVariable Long id ) {
        return service.getUserProfile(id);

    }

    @PostMapping("/me/handle")
    public ResponseEntity<?> setHandle(@RequestBody SetHandleRequest request,
                                        @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            return ResponseEntity.ok(service.setHandle(principal.getLocalUserId(), request.getHandle()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    public List<PlaylistResponse> getPlaylists(@PathVariable Long id,
                                                @RequestParam(required = false) Long viewerId) {
        return service.getUserPlaylists(id, viewerId);
    }

    @GetMapping("/playlists/{playlistId}/tracks")
    public List<PlaylistTrackResponse> getPlaylistTracks(@PathVariable Long playlistId) {
        return service.getPlaylistTracks(playlistId);
    }

    @PostMapping("/users/{id}/comments")
    public ResponseEntity<?> addCommentToProfile(@PathVariable Long id,
                                        @RequestBody CommentRequest request,
                                        @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            PostCommentResponse response = service.addCommentToProfile(principal.getLocalUserId(), request.getText(), id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @GetMapping("/users/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable Long id,
                                              @RequestParam(required = false) Long viewerId) {
        return service.getCommentsForProfile(id, viewerId);
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> likeComment(@PathVariable Long commentId, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            return new ResponseEntity<>(service.likeComment(commentId, principal.getLocalUserId()), HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<?> unlikeComment(@PathVariable Long commentId, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            return new ResponseEntity<>(service.unlikeComment(commentId, principal.getLocalUserId()), HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/playlists/{playlistId}/like")
    public ResponseEntity<?> likePlaylist(@PathVariable Long playlistId, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            return new ResponseEntity<>(service.likePlaylist(playlistId, principal.getLocalUserId()), HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/playlists/{playlistId}/like")
    public ResponseEntity<?> unlikePlaylist(@PathVariable Long playlistId, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        try {
            return new ResponseEntity<>(service.unlikePlaylist(playlistId, principal.getLocalUserId()), HttpStatus.OK);
        }
        catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
