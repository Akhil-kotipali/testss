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

    // 1. Check cache first
    String cached = redisTemplate.opsForValue().get(code);
    if (cached != null) {
        return cached; // ⚡ FAST PATH
    }

    // 2. DB lookup
    Link link = repo.findByShortCode(code)
            .orElseThrow(() -> new RuntimeException("Not found"));

    // 3. Check if expired
    if (!link.isAdminLink() && link.getExpiryAt() != null && link.getExpiryAt().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Link expired");
    }

    // 4. Store in cache
    if (link.isAdminLink()) {
        redisTemplate.opsForValue().set(code, link.getOriginalUrl());
    } else {
        long ttl = Duration.between(LocalDateTime.now(), link.getExpiryAt()).getSeconds();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(code, link.getOriginalUrl(), ttl, TimeUnit.SECONDS);
        }
    }

    return link.getOriginalUrl();
}
}
