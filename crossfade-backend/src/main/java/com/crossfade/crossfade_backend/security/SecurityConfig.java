package com.crossfade.crossfade_backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    SpotifyOAuth2UserService spotifyOAuth2UserService;

    @Autowired
    SpotifyAuthenticationSuccessHandler successHandler;

    @Autowired
    SpotifyAuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/users/*", "/api/users/*/now-playing", "/api/users/*/top-tracks",
                                "/api/users/*/top-artists", "/api/users/*/top-genres", "/api/users/*/playlists",
                                "/api/users/*/comments", "/api/users/*/following", "/api/users/*/feed",
                                "/api/users/search", "/api/compatibility", "/api/playlists/*/tracks").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(o -> o.userInfoEndpoint(u -> u.userService(spotifyOAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
                .logout(l -> l.logoutUrl("/api/logout")
                        .logoutSuccessHandler((req, res, a) -> res.setStatus(204)));
        return http.build();
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://127.0.0.1:5173"));
            config.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", config);
            return source;
        }
    }



