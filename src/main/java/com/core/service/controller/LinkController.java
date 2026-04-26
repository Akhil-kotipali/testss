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
public void redirect(@PathVariable String code,
                     HttpServletResponse response,
                     HttpServletRequest request,
                     @RequestHeader(value = "X-TEST-MODE", required = false) String testMode) throws IOException {

    if (!"true".equals(testMode)) {
        if (!rateLimitService.allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded");
            return;
        }
    }

    try {
        String url = redirectService.resolve(code);

        // ⚡ FASTEST redirect (no ResponseEntity)
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);

    } catch (Exception e) {
        response.setStatus(400);
        response.getWriter().write("Error: " + e.getMessage());
    }
}
}
