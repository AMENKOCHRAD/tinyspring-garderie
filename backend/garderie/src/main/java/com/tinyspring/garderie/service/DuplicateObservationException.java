package com.tinyspring.garderie.service;

public class DuplicateObservationException extends RuntimeException {
    private final Long duplicateOfId;
    private final Double score;

    public DuplicateObservationException(String message, Long duplicateOfId, Double score) {
        super(message);
        this.duplicateOfId = duplicateOfId;
        this.score = score;
    }

    public Long getDuplicateOfId() {
        return duplicateOfId;
    }

    public Double getScore() {
        return score;
    }
}

