package com.mariafernandes.urlshortener.exception;

public class InvalidExpirationException extends RuntimeException {

    public InvalidExpirationException(int maxExpirationDays) {
        super("A expiração máxima permitida é de " + maxExpirationDays + " dias");
    }
}
