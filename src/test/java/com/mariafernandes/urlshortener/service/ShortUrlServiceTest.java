package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private CodeGenerator codeGenerator;

    @InjectMocks
    private ShortUrlService service;

    @Test
    void create_deveGerarCodigoESalvar() {
        User owner = new User("maria@email.com", "123456");
        when(codeGenerator.generateCode()).thenReturn("abc123");
        when(repository.findByCode("abc123")).thenReturn(Optional.empty());
        when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortUrl result = service.create("https://google.com", owner);

        assertEquals("abc123", result.getCode());
        assertEquals("https://google.com", result.getOriginalUrl());
        assertEquals(owner, result.getOwner());
        verify(repository).save(any(ShortUrl.class));
        verify(codeGenerator).generateCode();
    }

    @Test
    void create_deveRegerarCodigoQuandoHouverColisao() {
        User owner = new User("maria@email.com", "123456");
        when(codeGenerator.generateCode())
            .thenReturn("abc123")
            .thenReturn("xyz789");
        when(repository.findByCode("abc123")).thenReturn(Optional.of(new ShortUrl()));
        when(repository.findByCode("xyz789")).thenReturn(Optional.empty());
        when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));

        ShortUrl result = service.create("https://google.com", owner);

        assertEquals("xyz789", result.getCode());
        verify(codeGenerator, times(2)).generateCode();
    }

    @Test
    void create_deveLancarExcecaoApos10Tentativas() {
        User owner = new User("maria@email.com", "123456");
        when(codeGenerator.generateCode()).thenReturn("abc123");
        when(repository.findByCode("abc123")).thenReturn(Optional.of(new ShortUrl()));

        assertThrows(RuntimeException.class,
            () -> service.create("https://google.com", owner));

        verify(codeGenerator, times(10)).generateCode();
    }

    @Test
    void findOriginalUrlByCode_deveRetornarUrl() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(shortUrl));

        String url = service.findOriginalUrlByCode("abc123");

        assertEquals("https://google.com", url);
    }

    @Test
    void findOriginalUrlByCode_deveLancarExcecaoQuandoNaoEncontrar() {
        when(repository.findByCode("naoexiste")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.findOriginalUrlByCode("naoexiste"));

        assertTrue(ex.getMessage().contains("naoexiste"));
    }

    @Test
    void incrementClickCount_deveIncrementarEPersistir() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        shortUrl.setClickCount(5L);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(shortUrl));

        service.incrementClickCount("abc123");

        assertEquals(6L, shortUrl.getClickCount());
        verify(repository).save(shortUrl);
    }

    @Test
    void incrementClickCount_deveIgnorarQuandoCodigoNaoExiste() {
        when(repository.findByCode("naoexiste")).thenReturn(Optional.empty());

        service.incrementClickCount("naoexiste");

        verify(repository, never()).save(any());
    }

    @Test
    void getByCodeForOwner_deveRetornarQuandoDonoCorreto() {
        User owner = new User("maria@email.com", "123456");
        owner.setId(1L);
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", owner);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(shortUrl));

        ShortUrl result = service.getByCodeForOwner("abc123", owner);

        assertEquals("abc123", result.getCode());
        assertEquals("https://google.com", result.getOriginalUrl());
    }

    @Test
    void getByCodeForOwner_deveLancarExcecaoQuandoDonoErrado() {
        User owner = new User("maria@email.com", "123456");
        owner.setId(1L);
        User outro = new User("outro@email.com", "123456");
        outro.setId(2L);
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", owner);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(shortUrl));

        assertThrows(IllegalArgumentException.class,
            () -> service.getByCodeForOwner("abc123", outro));
    }

    @Test
    void getByCodeForOwner_deveLancarExcecaoQuandoOwnerNulo() {
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", null);
        when(repository.findByCode("abc123")).thenReturn(Optional.of(shortUrl));

        User requestor = new User("maria@email.com", "123456");
        requestor.setId(1L);

        assertThrows(IllegalArgumentException.class,
            () -> service.getByCodeForOwner("abc123", requestor));
    }

    @Test
    void getByCodeForOwner_deveLancarExcecaoQuandoCodigoNaoExiste() {
        User owner = new User("maria@email.com", "123456");
        when(repository.findByCode("naoexiste")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.getByCodeForOwner("naoexiste", owner));
    }
}
