package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.exception.LinkExpiredException;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final CodeGenerator codeGenerator;
    private final int defaultExpirationDays;
    private final int maxExpirationDays;

    public ShortUrlService(ShortUrlRepository repository,
                           CodeGenerator codeGenerator,
                           @Value("${app.link.default-expiration-days}") int defaultExpirationDays,
                           @Value("${app.link.max-expiration-days}") int maxExpirationDays) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.defaultExpirationDays = defaultExpirationDays;
        this.maxExpirationDays = maxExpirationDays;
    }

    public ShortUrl create(String originalUrl, User owner, Integer expiresInDays) {
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(code, originalUrl, owner);
        shortUrl.setExpiresAt(calculateExpiresAt(expiresInDays));
        return repository.save(shortUrl);
    }

    @Cacheable(value = "shortUrls", key = "#code")
    public String findOriginalUrlByCode(String code) {
        ShortUrl shortUrl = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));
        assertNotExpired(shortUrl);
        return shortUrl.getOriginalUrl();
    }

    @Async
    public void incrementClickCount(String code) {
        repository.findByCode(code).ifPresent(shortUrl -> {
            if (isExpired(shortUrl)) {
                return;
            }
            shortUrl.setClickCount(shortUrl.getClickCount() + 1);
            repository.save(shortUrl);
        });
    }

    public Page<ShortUrl> getLinksForOwner(User owner, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByOwnerAndOriginalUrlContainingIgnoreCase(owner, search, pageable);
        }
        return repository.findByOwner(owner, pageable);
    }

    public ShortUrl getByCodeForOwner(String code, User owner) {
        ShortUrl shortUrl = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));

        if (shortUrl.getOwner() == null || !shortUrl.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Link não encontrado: " + code);
        }
        assertNotExpired(shortUrl);
        return shortUrl;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = codeGenerator.generateCode();
            if (repository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Falha ao gerar código único após 10 tentativas");
    }

    private LocalDateTime calculateExpiresAt(Integer expiresInDays) {
        int days = expiresInDays != null ? expiresInDays : defaultExpirationDays;
        if (days <= 0) {
            return null;
        }
        if (days > maxExpirationDays) {
            throw new IllegalArgumentException(
                "A expiração máxima permitida é de " + maxExpirationDays + " dias");
        }
        return LocalDateTime.now().plusDays(days);
    }

    private void assertNotExpired(ShortUrl shortUrl) {
        if (isExpired(shortUrl)) {
            throw new LinkExpiredException(shortUrl.getCode());
        }
    }

    private boolean isExpired(ShortUrl shortUrl) {
        return shortUrl.getExpiresAt() != null
            && shortUrl.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
