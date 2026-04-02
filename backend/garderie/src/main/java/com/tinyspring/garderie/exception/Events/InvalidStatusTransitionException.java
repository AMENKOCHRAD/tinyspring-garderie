package com.tinyspring.garderie.exception.Events;

public class InvalidStatusTransitionException extends RuntimeException{
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
