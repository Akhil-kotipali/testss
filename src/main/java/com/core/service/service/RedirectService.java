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
    private final StringRedisTemplate redisTemplate;
    private final Executor asyncExecutor;

    // 🔥 L1 CACHE (ULTRA FAST)
    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_HOURS = 24;

    public String resolve(String code) {

        // 🥇 STEP 1: LOCAL CACHE (FASTEST)
        String url = localCache.get(code);
        if (url != null) {
            asyncClickIncrement(code);
            return url;
        }

        // 🥈 STEP 2: REDIS
        String cachedUrl = redisTemplate.opsForValue().get(code);
        if (cachedUrl != null) {
            localCache.put(code, cachedUrl); // promote to L1
            asyncClickIncrement(code);
            return cachedUrl;
        }

        // 🥉 STEP 3: DATABASE
        Link link = repo.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (!link.isAdminLink() && link.getExpiryAt() != null &&
                link.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired");
        }

        url = link.getOriginalUrl();

        // store in both caches
        redisTemplate.opsForValue().set(code, url, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);
        localCache.put(code, url);

        asyncClickIncrement(code);

        return url;
    }

    // 🔥 NON-BLOCKING CLICK COUNT
    private void asyncClickIncrement(String code) {
        CompletableFuture.runAsync(() ->
                redisTemplate.opsForValue().increment("clicks:" + code),
                asyncExecutor
        );
    }
}
