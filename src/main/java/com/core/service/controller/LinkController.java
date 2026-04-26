package com.core.service.controller;

import com.core.service.dto.CreateLinkRequest;
import com.core.service.dto.StatsResponse;
import com.core.service.service.LinkService;
import com.core.service.service.RateLimitService;
import com.core.service.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> create(@RequestBody CreateLinkRequest req, 
                                   @RequestHeader(value = "X-ADMIN-KEY", required = false) String adminKey) {
        if (req.isAdmin() && !"Akhil21706020".equals(adminKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        return ResponseEntity.ok(linkService.create(req));
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<?> redirect(@PathVariable String code, HttpServletRequest request) {
        // Fast-fail for static files/API
        if (code.contains(".") || code.equals("health") || code.startsWith("api")) {
            return ResponseEntity.notFound().build();
        }

        if (!rateLimitService.allow(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
        }

        try {
            String url = redirectService.resolve(code);
            
            // Fix for "Temporal White Page": Ensure protocol exists
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(url))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}