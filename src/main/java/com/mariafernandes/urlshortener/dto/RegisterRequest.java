package com.mariafernandes.urlshortener.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 6, message = "A senha precisa ter no mínimo 6 caracteres")
    String password
) {}