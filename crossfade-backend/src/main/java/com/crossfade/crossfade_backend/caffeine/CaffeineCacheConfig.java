package com.crossfade.crossfade_backend.caffeine;


import com.crossfade.crossfade_backend.redis.TwoLevelCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineCacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userProfile", "topTracks", "topArtists", "topGenres", "playlists", "playlistTracks");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.HOURS)
                .maximumSize(500)
        );
        return manager;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CacheManager caffeineCacheManager, RedisCacheManager redisCacheManager) {
        return new TwoLevelCacheManager(caffeineCacheManager, redisCacheManager);
    }

}
