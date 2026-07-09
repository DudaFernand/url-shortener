package com.mariafernandes.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlRequest(
    @NotBlank(message = "A URL original é obrigatória")
    String originalUrl
) {}