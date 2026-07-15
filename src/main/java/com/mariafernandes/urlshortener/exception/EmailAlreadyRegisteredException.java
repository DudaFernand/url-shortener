package com.mariafernandes.urlshortener.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Email já cadastrado");
    }
}
