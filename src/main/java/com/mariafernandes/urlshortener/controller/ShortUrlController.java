package com.mariafernandes.urlshortener.controller;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.dto.CreateShortUrlRequest;
import com.mariafernandes.urlshortener.dto.ShortUrlResponse;
import com.mariafernandes.urlshortener.dto.ShortUrlStatsResponse;
import com.mariafernandes.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;

@RestController
public class ShortUrlController {

    private final ShortUrlService service;
    private final String baseUrl;

    public ShortUrlController(ShortUrlService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl;
    }

    @PostMapping("/links")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
            @AuthenticationPrincipal User user) {
        ShortUrl shortUrl = service.create(request.originalUrl(), user);
        ShortUrlResponse response = new ShortUrlResponse(
                shortUrl.getCode(),
                shortUrl.getOriginalUrl(),
                baseUrl + "/" + shortUrl.getCode(),
                shortUrl.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String originalUrl = service.findOriginalUrlByCode(code);
        service.incrementClickCount(code);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/links/{code}")
    public ResponseEntity<ShortUrlStatsResponse> stats(
            @PathVariable String code,
            @AuthenticationPrincipal User user) {

        ShortUrl shortUrl = service.getByCodeForOwner(code, user);

        return ResponseEntity.ok(new ShortUrlStatsResponse(
                shortUrl.getCode(),
                shortUrl.getOriginalUrl(),
                baseUrl + "/" + shortUrl.getCode(),
                shortUrl.getCreatedAt(),
                shortUrl.getClickCount()));
    }
}