package com.crossfade.crossfade_backend.spotify;

import com.crossfade.crossfade_backend.spotify.dto.SpotifyArtistDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyPagingDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyPlaylistDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyPlaylistTrackItemDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyProfileDto;
import com.crossfade.crossfade_backend.spotify.dto.SpotifyTrackDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

@Component
public class SpotifyApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyApiClient.class);

    private static final int MAX_RETRIES = 2;
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 2;
    private static final long MAX_RETRY_AFTER_SECONDS = 5;

    private final RestClient restClient;

    public SpotifyApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }

    private RestClient.RequestHeadersSpec<?> authorized(String path, OAuth2AuthorizedClient authorizedClient) {
        String token = authorizedClient.getAccessToken().getTokenValue();
        return restClient.get()
                .uri(path)
                .headers(headers -> headers.setBearerAuth(token));
    }

    /**
     * Retries a Spotify call on 429 Too Many Requests, honoring the Retry-After
     * header Spotify sends, up to MAX_RETRIES attempts.
     */
    private <T> T withRateLimitRetry(Supplier<T> request) {
        int attempt = 0;
        while (true) {
            try {
                return request.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                attempt++;
                if (attempt > MAX_RETRIES) {
                    log.error("Spotify rate limit still exceeded after {} retries, giving up", MAX_RETRIES);
                    throw e;
                }
                long waitSeconds = parseRetryAfterSeconds(e);
                log.warn("Spotify rate limited (429 QUOTA_EXCEEDED). Retrying in {}s (attempt {}/{})",
                        waitSeconds, attempt, MAX_RETRIES);
                sleep(waitSeconds);
            }
        }
    }

    private long parseRetryAfterSeconds(HttpClientErrorException.TooManyRequests e) {
        String header = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (header != null) {
            try {
                return Math.min(MAX_RETRY_AFTER_SECONDS, Math.max(1, Long.parseLong(header)));
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Spotify request", interrupted);
        }
    }

    public SpotifyProfileDto fetchProfile(OAuth2AuthorizedClient authorizedClient) {
        return withRateLimitRetry(() -> authorized("/me", authorizedClient)
                .retrieve()
                .body(SpotifyProfileDto.class));
    }

    public SpotifyPagingDto<SpotifyArtistDto> fetchTopArtists(OAuth2AuthorizedClient authorizedClient, String timeRange, int limit) {
        return withRateLimitRetry(() -> authorized("/me/top/artists?time_range=" + timeRange + "&limit=" + limit, authorizedClient)
                .retrieve()
                .body(new ParameterizedTypeReference<SpotifyPagingDto<SpotifyArtistDto>>() {}));
    }

    public SpotifyPagingDto<SpotifyTrackDto> fetchTopTracks(OAuth2AuthorizedClient authorizedClient, String timeRange, int limit) {
        return withRateLimitRetry(() -> authorized("/me/top/tracks?time_range=" + timeRange + "&limit=" + limit, authorizedClient)
                .retrieve()
                .body(new ParameterizedTypeReference<SpotifyPagingDto<SpotifyTrackDto>>() {}));
    }

    public SpotifyPagingDto<SpotifyPlaylistDto> fetchOwnPlaylists(OAuth2AuthorizedClient authorizedClient) {
        return withRateLimitRetry(() -> authorized("/me/playlists?limit=20", authorizedClient)
                .retrieve()
                .body(new ParameterizedTypeReference<SpotifyPagingDto<SpotifyPlaylistDto>>() {}));
    }

    public SpotifyPagingDto<SpotifyPlaylistTrackItemDto> fetchPlaylistTracks(OAuth2AuthorizedClient authorizedClient, String playlistId) {
        return withRateLimitRetry(() -> authorized("/playlists/" + playlistId + "/items?limit=100&market=from_token", authorizedClient)
                .retrieve()
                .body(new ParameterizedTypeReference<SpotifyPagingDto<SpotifyPlaylistTrackItemDto>>() {}));
    }
}
