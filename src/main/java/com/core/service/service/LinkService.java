package com.core.service.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.core.service.dto.CreateLinkRequest;
import com.core.service.dto.StatsResponse;
import com.core.service.entity.Link;
import com.core.service.repository.LinkRepository;
import com.core.service.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository repo;
    private final ShortCodeGenerator generator;
    private final StringRedisTemplate redis;

    public String create(CreateLinkRequest req) {
        // 1. Sanitize URL immediately
        String url = req.getUrl().trim();
        if (!url.startsWith("http")) url = "https://" + url;
        final String finalUrl = url;

        // 2. Fast Path: Check Redis for duplicate URL to avoid DB hit
        String existingCode = redis.opsForValue().get("url-map:" + finalUrl);
        if (existingCode != null) return existingCode;

        // 3. Generate Code
        String code = (req.getCustomCode() != null && !req.getCustomCode().isBlank())
                ? req.getCustomCode() : generator.generate();

        // 4. LIGHTNING STEP: Save to Redis first and return to user
        // This makes the 'Create' action feel instant
        redis.opsForValue().set(code, finalUrl, 24, TimeUnit.HOURS);
        redis.opsForValue().set("url-map:" + finalUrl, code, 24, TimeUnit.HOURS);

        // 5. Async Save: Push to MySQL in a separate thread so user doesn't wait
        CompletableFuture.runAsync(() -> {
            Link link = new Link();
            link.setOriginalUrl(finalUrl);
            link.setShortCode(code);
            link.setCreatedAt(LocalDateTime.now());
            if (!req.isAdmin() && req.getTtlMinutes() != null) {
                link.setExpiryAt(LocalDateTime.now().plusMinutes(req.getTtlMinutes()));
            }
            link.setAdminLink(req.isAdmin());
            repo.save(link);
        });

        return code;
    }
}