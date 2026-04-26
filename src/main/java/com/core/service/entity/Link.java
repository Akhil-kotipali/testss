package com.core.service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Link {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalUrl;
    private String shortCode;

    private Long clickCount = 0L;

    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;

    private LocalDateTime expiryAt;  
    private boolean adminLink = false;
}
