package com.core.service.controller;

import com.core.service.dto.CreateLinkRequest;
import com.core.service.dto.StatsResponse;
import com.core.service.service.LinkService;
import com.core.service.service.RateLimitService;
import com.core.service.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    private final RedirectService redirectService;
    private final RateLimitService rateLimitService;

    @PostMapping("/api/links")
public ResponseEntity<?> create(
        @RequestBody CreateLinkRequest req,
        @RequestHeader(value = "X-ADMIN-KEY", required = false) String adminKey
) {

    // hardcoded key 
    String SECRET = "Akhil21706020";

    if (req.isAdmin()) {
        if (adminKey == null || !adminKey.equals(SECRET)) {
            return ResponseEntity.status(403).body("Unauthorized admin access");
        }
    }

    String code = linkService.create(req);
    return ResponseEntity.ok(code);
}

    @GetMapping("/r/{code}")
public ResponseEntity<?> redirect(
        @PathVariable String code,
        HttpServletRequest request) {

    // ignore static files
    if (code.contains(".") || code.equals("health") || code.startsWith("api")) {
        return ResponseEntity.notFound().build();
    }

    String ip = request.getRemoteAddr();

    if (!rateLimitService.allow(ip)) {
        return ResponseEntity.status(429).body("Too many requests");
    }

    String url = redirectService.resolve(code);

    return ResponseEntity.status(302)
            .location(URI.create(url))
            .build();
}

    @GetMapping("/api/links/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return linkService.getStats(code);
    }
}

