package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.exception.InvalidExpirationException;
import com.mariafernandes.urlshortener.exception.LinkExpiredException;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ShortUrlService {

    private static final String CACHE_NAME = "shortUrls";

    private final ShortUrlRepository repository;
    private final CodeGenerator codeGenerator;
    private final CacheManager cacheManager;
    private final int defaultExpirationDays;
    private final int maxExpirationDays;

    public ShortUrlService(ShortUrlRepository repository,
                           CodeGenerator codeGenerator,
                           @Autowired(required = false) CacheManager cacheManager,
                           @Value("${app.link.default-expiration-days}") int defaultExpirationDays,
                           @Value("${app.link.max-expiration-days}") int maxExpirationDays) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.cacheManager = cacheManager;
        this.defaultExpirationDays = defaultExpirationDays;
        this.maxExpirationDays = maxExpirationDays;
    }

    public ShortUrl create(String originalUrl, User owner, Integer expiresInDays) {
        LocalDateTime expiresAt = calculateExpiresAt(expiresInDays);
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(code, originalUrl, owner);
        shortUrl.setExpiresAt(expiresAt);
        return repository.save(shortUrl);
    }

    public String findOriginalUrlByCode(String code) {
        CachedUrl cached = getFromCache(code);
        if (cached != null) {
            assertNotExpired(code, cached.expiresAt());
            return cached.originalUrl();
        }

        ShortUrl shortUrl = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));
        assertNotExpired(shortUrl);
        putInCache(code, shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
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
            throw new InvalidExpirationException(maxExpirationDays);
        }
        return LocalDateTime.now().plusDays(days);
    }

    private void assertNotExpired(ShortUrl shortUrl) {
        assertNotExpired(shortUrl.getCode(), shortUrl.getExpiresAt());
    }

    private void assertNotExpired(String code, LocalDateTime expiresAt) {
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            evictFromCache(code);
            throw new LinkExpiredException(code);
        }
    }

    private boolean isExpired(ShortUrl shortUrl) {
        return shortUrl.getExpiresAt() != null
            && shortUrl.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private CachedUrl getFromCache(String code) {
        Cache cache = cache();
        if (cache == null) {
            return null;
        }
        return cache.get(code, CachedUrl.class);
    }

    private void putInCache(String code, String originalUrl, LocalDateTime expiresAt) {
        Cache cache = cache();
        if (cache != null) {
            cache.put(code, new CachedUrl(originalUrl, expiresAt));
        }
    }

    private void evictFromCache(String code) {
        Cache cache = cache();
        if (cache != null) {
            cache.evict(code);
        }
    }

    private Cache cache() {
        return cacheManager != null ? cacheManager.getCache(CACHE_NAME) : null;
    }

    public record CachedUrl(String originalUrl, LocalDateTime expiresAt) {}
}
