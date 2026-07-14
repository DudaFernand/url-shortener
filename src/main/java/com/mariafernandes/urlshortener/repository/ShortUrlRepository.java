package com.mariafernandes.urlshortener.repository;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByCode(String code);
    Page<ShortUrl> findByOwner(User owner, Pageable pageable);
    Page<ShortUrl> findByOwnerAndOriginalUrlContainingIgnoreCase(User owner, String originalUrl, Pageable pageable);
    List<ShortUrl> findByExpiresAtBefore(LocalDateTime now);
    long deleteByExpiresAtBefore(LocalDateTime now);
}