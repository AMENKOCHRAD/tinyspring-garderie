package com.tinyspring.garderie.exception.Events;

public class AuthorizationRequiredException extends RuntimeException {
    public AuthorizationRequiredException(String message) {
        super(message);
    }

}
