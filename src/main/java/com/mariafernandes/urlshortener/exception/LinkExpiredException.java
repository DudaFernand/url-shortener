package com.mariafernandes.urlshortener.exception;

public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String code) {
        super("Link expirado: " + code);
    }
}
