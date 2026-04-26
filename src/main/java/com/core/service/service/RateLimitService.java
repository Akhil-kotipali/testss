package com.core.service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, List<Long>> map = new ConcurrentHashMap<>();

    private static final int LIMIT = 10;
    private static final long WINDOW = 60000;

    public boolean allow(String ip) {

        long now = System.currentTimeMillis();

        map.putIfAbsent(ip, new ArrayList<>());
        List<Long> requests = map.get(ip);

        requests.removeIf(t -> now - t > WINDOW);

        if (requests.size() >= LIMIT) return false;

        requests.add(now);
        return true;
    }
}
