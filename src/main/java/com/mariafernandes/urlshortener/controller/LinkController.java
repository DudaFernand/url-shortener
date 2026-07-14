package com.mariafernandes.urlshortener.controller;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.dto.CreateShortUrlRequest;
import com.mariafernandes.urlshortener.dto.ShortUrlResponse;
import com.mariafernandes.urlshortener.dto.ShortUrlStatsResponse;
import com.mariafernandes.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/links")
public class LinkController {

    private final ShortUrlService service;
    private final String baseUrl;

    public LinkController(ShortUrlService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
            @AuthenticationPrincipal User user) {
        ShortUrl shortUrl = service.create(request.originalUrl(), user, request.expiresInDays());
        ShortUrlResponse response = new ShortUrlResponse(
                shortUrl.getCode(),
                shortUrl.getOriginalUrl(),
                baseUrl + "/" + shortUrl.getCode(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ShortUrlStatsResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ShortUrlStatsResponse> page = service.getLinksForOwner(user, search, pageable)
            .map(this::toStatsResponse);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{code}")
    public ResponseEntity<ShortUrlStatsResponse> stats(
            @PathVariable String code,
            @AuthenticationPrincipal User user) {

        ShortUrl shortUrl = service.getByCodeForOwner(code, user);
        return ResponseEntity.ok(toStatsResponse(shortUrl));
    }

    private ShortUrlStatsResponse toStatsResponse(ShortUrl shortUrl) {
        return new ShortUrlStatsResponse(
                shortUrl.getCode(),
                shortUrl.getOriginalUrl(),
                baseUrl + "/" + shortUrl.getCode(),
                shortUrl.getCreatedAt(),
                shortUrl.getClickCount(),
                shortUrl.getExpiresAt());
    }
}
