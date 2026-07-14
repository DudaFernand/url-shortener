package com.mariafernandes.urlshortener.dto;

import java.time.LocalDateTime;

public record ShortUrlStatsResponse(
    String code,
    String originalUrl,
    String shortUrl,
    LocalDateTime createdAt,
    Long clickCount,
    LocalDateTime expiresAt
) {}
