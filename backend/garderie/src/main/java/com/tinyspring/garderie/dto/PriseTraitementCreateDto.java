package com.tinyspring.garderie.dto;

import java.time.LocalDate;

public class PriseTraitementCreateDto {
    private LocalDate datePrise;
    private String heurePrevue;
    private String note;

    public LocalDate getDatePrise() {
        return datePrise;
    }

    public void setDatePrise(LocalDate datePrise) {
        this.datePrise = datePrise;
    }

    public String getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(String heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

