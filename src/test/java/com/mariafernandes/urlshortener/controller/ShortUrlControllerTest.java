package com.mariafernandes.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mariafernandes.urlshortener.domain.ShortUrl;
import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.dto.CreateShortUrlRequest;
import com.mariafernandes.urlshortener.security.CustomUserDetailsService;
import com.mariafernandes.urlshortener.security.JwtService;
import com.mariafernandes.urlshortener.service.ShortUrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShortUrlController.class)
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShortUrlService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void redirect_deveRetornar302ComLocationCorreto() throws Exception {
        when(service.findOriginalUrlByCode("abc123"))
            .thenReturn("https://google.com");

        mockMvc.perform(get("/abc123"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://google.com"));

        verify(service).incrementClickCount("abc123");
    }

    @Test
    void redirect_deveRetornar404QuandoCodigoNaoExiste() throws Exception {
        when(service.findOriginalUrlByCode("naoexiste"))
            .thenThrow(new IllegalArgumentException("Link não encontrado: naoexiste"));

        mockMvc.perform(get("/naoexiste"))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_deveRetornar401SemToken() throws Exception {
        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com"));

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

        when(service.create(eq("https://google.com"), any(User.class)))
            .thenReturn(shortUrl);

        String json = objectMapper.writeValueAsString(
            new CreateShortUrlRequest("https://google.com"));

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
