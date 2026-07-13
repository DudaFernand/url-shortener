package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ShortUrlService {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final ShortUrlRepository repository;

    public ShortUrlService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    public ShortUrl create(String originalUrl, User owner) {
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(code, originalUrl, owner);
        return repository.save(shortUrl);
    }

    @Cacheable(value = "shortUrls", key = "#code")
    public String findOriginalUrlByCode(String code) {
        return repository.findByCode(code)
            .map(ShortUrl::getOriginalUrl)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));
    }

    @Async
    public void incrementClickCount(String code) {
        repository.findByCode(code).ifPresent(shortUrl -> {
            shortUrl.setClickCount(shortUrl.getClickCount() + 1);
            repository.save(shortUrl);
        });
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (repository.findByCode(code).isPresent());
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}