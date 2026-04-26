package com.core.service.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, Counter> map = new ConcurrentHashMap<>();

    private static final int LIMIT = 50; // increase for testing
    private static final long WINDOW_MS = 60000;

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();

        map.putIfAbsent(ip, new Counter(now, 0));
        Counter counter = map.get(ip);

        synchronized (counter) {
            if (now - counter.startTime > WINDOW_MS) {
                counter.startTime = now;
                counter.count = 0;
            }

            if (counter.count >= LIMIT) return false;

            counter.count++;
            return true;
        }
    }

    static class Counter {
        long startTime;
        int count;

        Counter(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
