package com.core.service.service;

import java.time.LocalDateTime;
import java.util.Optional;

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

    // 1. Check existing URL
    Optional<Link> existing = repo.findByOriginalUrl(req.getUrl());

    if (existing.isPresent()) {
        return existing.get().getShortCode();
    }

    // 2. Generate or use custom
    String code = (req.getCustomCode() != null && !req.getCustomCode().isBlank())
            ? req.getCustomCode()
            : generator.generate();

    // 3. Ensure uniqueness
    while (repo.findByShortCode(code).isPresent()) {
        code = generator.generate();
    }

    Link link = new Link();
    link.setOriginalUrl(req.getUrl());
    link.setShortCode(code);
    link.setCreatedAt(LocalDateTime.now());

    if (!req.isAdmin() && req.getTtlMinutes() != null) {
        link.setExpiryAt(LocalDateTime.now().plusMinutes(req.getTtlMinutes()));
    }

    link.setAdminLink(req.isAdmin());

    repo.save(link);

    return code;
}

    public StatsResponse getStats(String code) {
        Link link = repo.findByShortCode(code)
                .orElseThrow(() -> new RuntimeException("Not found"));

        return new StatsResponse(
                link.getClickCount(),
                link.getCreatedAt(),
                link.getLastAccessed()
        );
    }
}
