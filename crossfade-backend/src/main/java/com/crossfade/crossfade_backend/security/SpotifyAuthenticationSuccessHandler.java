package com.crossfade.crossfade_backend.security;

import com.crossfade.crossfade_backend.spotify.SpotifySyncService;
import com.crossfade.crossfade_backend.spotify.SyncQuotaExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SpotifyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAuthenticationSuccessHandler.class);

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Autowired
    private SpotifySyncService spotifySyncService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CrossfadeOAuth2User principal = (CrossfadeOAuth2User) authentication.getPrincipal();
        try {
            spotifySyncService.syncUser(principal.getLocalUserId(), authentication);
        } catch (SyncQuotaExceededException e) {
            log.info("Skipping login sync for user {} — quota exceeded, next available at {}",
                    principal.getLocalUserId(), e.getNextAvailableAt());
        } catch (Exception e) {
            log.error("Spotify sync failed for user {}", principal.getLocalUserId(), e);
        }
        response.sendRedirect(frontendBaseUrl);
    }
}
