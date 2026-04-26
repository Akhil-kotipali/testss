package com.core.service.dto;

import lombok.Data;

@Data
public class CreateLinkRequest {
    private String url;

    private String customCode;

    private Long ttlMinutes;

    private boolean admin;
}

