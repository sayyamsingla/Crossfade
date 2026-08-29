package com.crossfade.crossfade_backend.controller;

import com.crossfade.crossfade_backend.response.SyncStatusResponse;
import com.crossfade.crossfade_backend.security.CrossfadeOAuth2User;
import com.crossfade.crossfade_backend.spotify.SpotifySyncService;
import com.crossfade.crossfade_backend.spotify.SyncQuotaExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SpotifySyncController {

    @Autowired
    private SpotifySyncService spotifySyncService;

    @PostMapping("/spotify")
    public ResponseEntity<?> resyncSpotify(@AuthenticationPrincipal CrossfadeOAuth2User principal,
                                            Authentication authentication) {
        try {
            spotifySyncService.syncUser(principal.getLocalUserId(), authentication);
            return ResponseEntity.noContent().build();
        } catch (SyncQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("nextAvailableAt", e.getNextAvailableAt().toString()));
        }
    }

    @GetMapping("/spotify/status")
    public SyncStatusResponse syncStatus(@AuthenticationPrincipal CrossfadeOAuth2User principal) {
        return spotifySyncService.getSyncStatus(principal.getLocalUserId());
    }
}
