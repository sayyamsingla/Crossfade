package com.crossfade.crossfade_backend.controller;


import com.crossfade.crossfade_backend.response.FollowingResponse;
import com.crossfade.crossfade_backend.security.CrossfadeOAuth2User;
import com.crossfade.crossfade_backend.service.FriendsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class FriendsController {

    @Autowired
    FriendsService service;

    @GetMapping("/{id}/following")
    public List<FollowingResponse> fetchfollowing(@PathVariable Long id) {
        return service.getfollowing(id);
    }

    @GetMapping("/search")
    public List<FollowingResponse> searchUsers(@RequestParam String q, @RequestParam(required = false) Long viewerId) {
        return service.searchUsers(q, viewerId == null ? -1L : viewerId);
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<?> follow(@PathVariable Long id, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        service.follow(principal.getLocalUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<?> unfollow(@PathVariable Long id, @AuthenticationPrincipal CrossfadeOAuth2User principal) {
        service.unfollow(principal.getLocalUserId(), id);
        return ResponseEntity.noContent().build();
    }

}
