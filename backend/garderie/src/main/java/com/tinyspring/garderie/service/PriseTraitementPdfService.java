package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.PriseTraitement;

public interface PriseTraitementPdfService {

    byte[] genererPdf(PriseTraitement prise);
}

