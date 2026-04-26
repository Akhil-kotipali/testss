package com.core.service.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveScheduler {

    @Value("${app.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void ping() {
        try {
            restTemplate.getForObject(baseUrl + "/health", String.class);
        } catch (Exception ignored) {}
    }
}
