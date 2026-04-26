package com.core.service.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
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
    private final FastCacheService fastCache;
    private final Executor asyncExecutor;

    public String resolve(String code) {

        // ⚡ 1. L1 CACHE (nanoseconds)
        String url = fastCache.get(code);
        if (url != null) {
            asyncClick(code);
            return url;
        }

        // ⚡ 2. REDIS
        url = redis.opsForValue().get(code);
        if (url != null) {
            fastCache.put(code, url); // promote to L1
            asyncClick(code);
            return url;
        }

        // ⚡ 3. DB
        Link link = repo.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (!link.isAdminLink() && link.getExpiryAt() != null &&
                link.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Expired");
        }

        url = link.getOriginalUrl();

        // store in caches
        fastCache.put(code, url);
        redis.opsForValue().set(code, url);

        asyncClick(code);

        return url;
    }

    private void asyncClick(String code) {
        CompletableFuture.runAsync(() ->
                redis.opsForValue().increment("clicks:" + code),
                asyncExecutor
        );
    }
}
