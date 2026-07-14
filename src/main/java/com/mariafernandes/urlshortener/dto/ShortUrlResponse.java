package com.mariafernandes.urlshortener.dto;

import java.time.LocalDateTime;

public record ShortUrlResponse(
    String code,
    String originalUrl,
    String shortUrl,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
