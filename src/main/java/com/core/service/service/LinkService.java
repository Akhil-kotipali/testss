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
    private final Executor asyncExecutor; // ✅ custom executor

    public String create(CreateLinkRequest req) {

        // 1️⃣ Sanitize URL
        String url = req.getUrl().trim();
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        final String finalUrl = url;

        // 2️⃣ Check Redis for duplicate (fast path)
        String existingCode = redis.opsForValue().get("url-map:" + finalUrl);
        if (existingCode != null) {
            return existingCode;
        }

        // 3️⃣ Generate short code
        String code = (req.getCustomCode() != null && !req.getCustomCode().isBlank())
                ? req.getCustomCode()
                : generator.generate();

        // 4️⃣ Store in Redis (instant response path)
        redis.opsForValue().set(code, finalUrl, 24, TimeUnit.HOURS);
        redis.opsForValue().set("url-map:" + finalUrl, code, 24, TimeUnit.HOURS);

        // 5️⃣ Async DB save (non-blocking)
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
                // 🔥 prevent silent failure
                e.printStackTrace();
            }
        }, asyncExecutor);

        return code;
    }
}
