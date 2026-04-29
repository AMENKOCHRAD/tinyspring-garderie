package com.tinyspring.garderie.exception.Events;

public class CapacityExceededException  extends RuntimeException {
    public CapacityExceededException(String message) {
        super(message);
    }
}
