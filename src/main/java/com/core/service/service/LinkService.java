package com.core.service.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.core.service.dto.CreateLinkRequest;
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
    private final Executor asyncExecutor;

    public String create(CreateLinkRequest req) {

        // 1. Sanitize URL
        String url = req.getUrl().trim();
        if (!url.startsWith("http")) url = "https://" + url;
        final String finalUrl = url;

        // 2. Redis duplicate check
        String existingCode = redis.opsForValue().get("url-map:" + finalUrl);
        if (existingCode != null) return existingCode;

        // 3. Generate code
        String code = (req.getCustomCode() != null && !req.getCustomCode().isBlank())
                ? req.getCustomCode()
                : generator.generate();

        // 4. Instant response via Redis
        redis.opsForValue().set(code, finalUrl, 24, TimeUnit.HOURS);
        redis.opsForValue().set("url-map:" + finalUrl, code, 24, TimeUnit.HOURS);

        // 5. Async DB save (TRUE async now)
        CompletableFuture.runAsync(() -> {
            try {
                Link link = new Link();
                link.setOriginalUrl(finalUrl);
                link.setShortCode(code);
                link.setCreatedAt(LocalDateTime.now());

                if (!req.isAdmin() && req.getTtlMinutes() != null) {
                    link.setExpiryAt(LocalDateTime.now().plusMinutes(req.getTtlMinutes()));
                }

                link.setAdminLink(req.isAdmin());

                repo.save(link);

            } catch (Exception e) {
                // DO NOT break main flow
                System.out.println("Async DB save failed: " + e.getMessage());
            }
        }, asyncExecutor);

        return code;
    }
}
