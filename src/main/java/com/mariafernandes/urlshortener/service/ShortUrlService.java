package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final CodeGenerator codeGenerator;

    public ShortUrlService(ShortUrlRepository repository, CodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
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
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = codeGenerator.generateCode();
            if (repository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Falha ao gerar código único após 10 tentativas");
    }


    public ShortUrl getByCodeForOwner(String code, User owner) {
        ShortUrl shortUrl = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Link não encontrado: " + code));
    
        if (shortUrl.getOwner() == null || !shortUrl.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Link não encontrado: " + code); // ou 403
        }
        return shortUrl;
    }
}