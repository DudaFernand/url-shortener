package com.mariafernandes.urlshortener.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlRequest(
    @NotBlank(message = "A URL original é obrigatória")
    String originalUrl,

    @Min(value = 1, message = "A expiração deve ser de no mínimo 1 dia")
    Integer expiresInDays
) {}
