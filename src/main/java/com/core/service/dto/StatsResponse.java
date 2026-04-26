package com.core.service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsResponse {
    private Long clicks;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;
}
