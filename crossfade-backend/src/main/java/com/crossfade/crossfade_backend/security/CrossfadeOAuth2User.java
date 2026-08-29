package com.crossfade.crossfade_backend.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CrossfadeOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final Long localUserId;

    public CrossfadeOAuth2User(OAuth2User delegate, Long localUserId) {
        this.delegate = delegate;
        this.localUserId = localUserId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    public Long getLocalUserId() {
        return localUserId;
    }
}
