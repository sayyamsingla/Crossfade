package com.crossfade.crossfade_backend.redis;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager l1;
    private final CacheManager l2;
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager l1, CacheManager l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> {
            Cache l1Cache = l1.getCache(n);
            Cache l2Cache = l2.getCache(n);
            return new TwoLevelCache(n, l1Cache, l2Cache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return l1.getCacheNames();
    }
}
