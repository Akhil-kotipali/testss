package com.core.service.service;

import java.time.LocalDateTime;

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

    public String create(CreateLinkRequest req) {

        // 1. Sanitize URL
        String url = req.getUrl().trim();
        if (!url.startsWith("http")) url = "https://" + url;

        // 2. Generate code
        String code = (req.getCustomCode() != null && !req.getCustomCode().isBlank())
                ? req.getCustomCode()
                : generator.generate();

        // 3. Direct DB save (FAST for single user)
        Link link = new Link();
        link.setOriginalUrl(url);
        link.setShortCode(code);
        link.setCreatedAt(LocalDateTime.now());

        if (!req.isAdmin() && req.getTtlMinutes() != null) {
            link.setExpiryAt(LocalDateTime.now().plusMinutes(req.getTtlMinutes()));
        }

        link.setAdminLink(req.isAdmin());

        repo.save(link);

        return code;
    }
}
