package com.core.service.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, Counter> map = new ConcurrentHashMap<>();

    private static final int LIMIT = 50;
    private static final long WINDOW_MS = 60000;

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();

        map.putIfAbsent(ip, new Counter(now));
        Counter counter = map.get(ip);

        if (now - counter.startTime > WINDOW_MS) {
            counter.startTime = now;
            counter.count.set(0);
        }

        return counter.count.incrementAndGet() <= LIMIT;
    }

    static class Counter {
        volatile long startTime;
        AtomicInteger count = new AtomicInteger(0);

        Counter(long startTime) {
            this.startTime = startTime;
        }
    }
}
