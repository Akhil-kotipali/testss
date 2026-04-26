package com.core.service.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class FastCacheService {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public String get(String code) {
        return cache.get(code);
    }

    public void put(String code, String url) {
        cache.put(code, url);
    }
}
