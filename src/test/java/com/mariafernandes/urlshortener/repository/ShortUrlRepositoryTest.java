package com.mariafernandes.urlshortener.repository;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.support.AbstractPostgresContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShortUrlRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    private ShortUrlRepository repository;

    @Test
    void findByCode_deveRetornarShortUrlQuandoExiste() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        repository.save(shortUrl);

        Optional<ShortUrl> result = repository.findByCode("abc123");

        assertTrue(result.isPresent());
        assertEquals("https://google.com", result.get().getOriginalUrl());
        assertEquals("abc123", result.get().getCode());
    }

    @Test
    void findByCode_deveRetornarVazioQuandoNaoExiste() {
        Optional<ShortUrl> result = repository.findByCode("naoexiste");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCode_deveDistinguirEntreCodigosDiferentes() {
        repository.save(new ShortUrl("abc123", "https://google.com", null));
        repository.save(new ShortUrl("xyz789", "https://github.com", null));

        Optional<ShortUrl> result1 = repository.findByCode("abc123");
        Optional<ShortUrl> result2 = repository.findByCode("xyz789");

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("https://google.com", result1.get().getOriginalUrl());
        assertEquals("https://github.com", result2.get().getOriginalUrl());
    }

    @Test
    void save_devePreencherIdAutomaticamente() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        assertNull(shortUrl.getId());

        ShortUrl saved = repository.save(shortUrl);

        assertNotNull(saved.getId());
    }

    @Test
    void save_devePreencherClickCountComZero() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        ShortUrl saved = repository.save(shortUrl);

        assertEquals(0L, saved.getClickCount());
    }
}
