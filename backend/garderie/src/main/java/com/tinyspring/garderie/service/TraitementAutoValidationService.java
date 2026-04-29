package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.Traitement;

import java.util.ArrayList;
import java.util.List;

public interface TraitementAutoValidationService {

    enum Decision {
        ACCEPTE,
        REFUSE,
        A_VERIFIER
    }

    class Result {
        public Decision decision;
        public double confiance;
        public List<String> facteurs = new ArrayList<>();
        public String note;
        public String source; // RULES / ML / FALLBACK
    }

    Result evaluer(Traitement traitement);
}

