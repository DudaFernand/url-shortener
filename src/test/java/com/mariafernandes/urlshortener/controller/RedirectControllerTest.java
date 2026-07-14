package com.mariafernandes.urlshortener.controller;

import com.mariafernandes.urlshortener.exception.GlobalExceptionHandler;
import com.mariafernandes.urlshortener.exception.LinkExpiredException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void redirect_deveRetornar410QuandoLinkExpirado() throws Exception {
        when(service.findOriginalUrlByCode("abc123"))
            .thenThrow(new LinkExpiredException("abc123"));

        mockMvc.perform(get("/abc123"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.message").value("Link expirado: abc123"));
    }
}
