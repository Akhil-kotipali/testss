package com.core.service.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.core.service.entity.Link;
import com.core.service.repository.LinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private final LinkRepository repo;
    private final StringRedisTemplate redis;
    private final Executor asyncExecutor;

    // 🔥 L1 CACHE (fastest possible)
    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    public String resolve(String code) {

        // 🥇 1. Local cache (NO NETWORK)
        String url = localCache.get(code);
        if (url != null) {
            asyncIncrement(code);
            return url;
        }

        // 🥈 2. Redis
        String cachedUrl = redis.opsForValue().get(code);
        if (cachedUrl != null) {
            localCache.put(code, cachedUrl); // promote to L1
            asyncIncrement(code);
            return cachedUrl;
        }

        // 🥉 3. Database fallback
        Link link = repo.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (!link.isAdminLink() &&
                link.getExpiryAt() != null &&
                link.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired");
        }

        url = link.getOriginalUrl();

        // store in both caches
        redis.opsForValue().set(code, url);
        localCache.put(code, url);

        asyncIncrement(code);

        return url;
    }

    private void asyncIncrement(String code) {
        CompletableFuture.runAsync(() -> {
            try {
                redis.opsForValue().increment("clicks:" + code);
            } catch (Exception ignored) {}
        }, asyncExecutor);
    }
}
