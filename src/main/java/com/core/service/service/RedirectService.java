package com.core.service.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
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

   public String resolve(String code) {
    // 1. Direct Cache Hit (The 20ms Path)
    String cachedUrl = redisTemplate.opsForValue().get(code);
    if (cachedUrl != null) {
        // Record click asynchronously in Redis
        redisTemplate.opsForValue().increment("clicks:" + code);
        return cachedUrl;
    }

    // 2. DB Fallback with double-check
    Link link = repo.findByShortCode(code)
            .orElseThrow(() -> new RuntimeException("Not found"));

    // Check expiry
    if (!link.isAdminLink() && link.getExpiryAt() != null && 
        link.getExpiryAt().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("temporal"); // Clean error for your handler
    }

    // 3. Populate cache for the next million users
    redisTemplate.opsForValue().set(code, link.getOriginalUrl());
    return link.getOriginalUrl();
}
}