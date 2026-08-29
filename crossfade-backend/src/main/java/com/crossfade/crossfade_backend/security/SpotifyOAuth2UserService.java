package com.crossfade.crossfade_backend.security;

import com.crossfade.crossfade_backend.model.User;
import com.crossfade.crossfade_backend.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SpotifyOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    UserRepository userRepo;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // steps 1-5 go here
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String spotifyId = (String) oauth2User.getAttributes().get("id");
        User user = userRepo.findBySpotifyUserId(spotifyId).orElseGet(() -> {
            User newUser = new User();
            newUser.setSpotifyUserId(spotifyId);
            newUser.setDisplayName((String) oauth2User.getAttributes().get("display_name"));

            List<Map<String, Object>> images = (List<Map<String, Object>>) oauth2User.getAttributes().get("images");
            String avatarUrl = (images == null || images.isEmpty()) ? null : (String) images.get(0).get("url");
            newUser.setAvatarUrl(avatarUrl);

            return userRepo.save(newUser);
        });

        return new CrossfadeOAuth2User(oauth2User, user.getId());
    }
}