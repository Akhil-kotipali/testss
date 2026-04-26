package com.core.service.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

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

    private static final long CACHE_TTL_HOURS = 24;

    public String resolve(String code) {

        // 🔹 1. Try Redis first
        String cachedUrl = redisTemplate.opsForValue().get(code);

        if (cachedUrl != null) {

            // 🔥 Async increment (non-blocking)
            CompletableFuture.runAsync(() ->
                redisTemplate.opsForValue().increment("clicks:" + code)
            );

            return cachedUrl;
        }

        // 🔹 2. Fallback to DB
        Link link = repo.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // 🔹 3. Expiry check
        if (!link.isAdminLink() && link.getExpiryAt() != null &&
                link.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired");
        }

        String url = link.getOriginalUrl();

        // 🔥 Store in Redis WITH TTL (important)
        redisTemplate.opsForValue().set(code, url, CACHE_TTL_HOURS, java.util.concurrent.TimeUnit.HOURS);

        // 🔥 Async click increment here too
        CompletableFuture.runAsync(() ->
            redisTemplate.opsForValue().increment("clicks:" + code)
        );

        return url;
    }
}
