package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
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

    public ShortUrl create(String originalUrl) {
        String code = generateUniqueCode();
        ShortUrl shortUrl = new ShortUrl(code, originalUrl);
        return repository.save(shortUrl);
    }

    public ShortUrl findByCodeOrThrow(String code) {
        ShortUrl shortUrl = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));
        shortUrl.setClickCount(shortUrl.getClickCount() + 1);
        return repository.save(shortUrl);
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