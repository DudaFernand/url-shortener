package com.mariafernandes.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.dto.CreateShortUrlRequest;
import com.mariafernandes.urlshortener.exception.GlobalExceptionHandler;
import com.mariafernandes.urlshortener.security.CustomUserDetailsService;
import com.mariafernandes.urlshortener.security.JwtAuthFilter;
import com.mariafernandes.urlshortener.security.JwtService;
import com.mariafernandes.urlshortener.security.RateLimitFilter;
import com.mariafernandes.urlshortener.security.SecurityConfig;
import com.mariafernandes.urlshortener.service.ShortUrlService;
import com.mariafernandes.urlshortener.support.MockFilterSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LinkController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ShortUrlService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void configureFilters() {
        MockFilterSupport.passThrough(rateLimitFilter, jwtAuthFilter);
    }

    @Test
    void create_deveRetornar401SemToken() throws Exception {
        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com", null));

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void create_deveRetornar201ComUsuarioAutenticado() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", user);

        when(service.create(eq("https://google.com"), any(User.class), isNull()))
            .thenReturn(shortUrl);

        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com", null));

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(user(user)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("abc123"))
            .andExpect(jsonPath("$.originalUrl").value("https://google.com"))
            .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"));
    }

    @Test
    void create_deveRetornar400QuandoExpiresInDaysInvalido() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);

        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com", 0));

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(user(user)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_deveRetornarExpiresAtQuandoInformado() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", user);
        shortUrl.setExpiresAt(java.time.LocalDateTime.of(2026, 7, 21, 10, 0));

        when(service.create(eq("https://google.com"), any(User.class), eq(7)))
            .thenReturn(shortUrl);

        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com", 7));

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(user(user)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void list_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/links"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void list_deveRetornarPaginaDeLinksDoUsuario() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);

        ShortUrl link1 = new ShortUrl("abc123", "https://google.com", user);
        link1.setClickCount(10L);
        ShortUrl link2 = new ShortUrl("xyz789", "https://github.com", user);
        link2.setClickCount(5L);

        Page<ShortUrl> page = new PageImpl<>(List.of(link1, link2));
        when(service.getLinksForOwner(any(User.class), isNull(), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/links")
                .with(user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].code").value("abc123"))
            .andExpect(jsonPath("$.content[1].code").value("xyz789"))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void list_deveAceitarParametroDeBusca() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);

        ShortUrl link = new ShortUrl("abc123", "https://google.com", user);
        link.setClickCount(10L);

        Page<ShortUrl> page = new PageImpl<>(List.of(link));
        when(service.getLinksForOwner(any(User.class), eq("google"), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/links")
                .param("search", "google")
                .with(user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].originalUrl").value("https://google.com"));
    }

    @Test
    void stats_deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/links/abc123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void stats_deveRetornarDadosComUsuarioAutenticado() throws Exception {
        User user = new User("maria@email.com", "123456");
        user.setId(1L);
        ShortUrl shortUrl = new ShortUrl("abc123", "https://google.com", user);
        shortUrl.setClickCount(42L);

        when(service.getByCodeForOwner(eq("abc123"), any(User.class)))
            .thenReturn(shortUrl);

        mockMvc.perform(get("/links/abc123")
                .with(user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("abc123"))
            .andExpect(jsonPath("$.originalUrl").value("https://google.com"))
            .andExpect(jsonPath("$.clickCount").value(42));
    }
}
