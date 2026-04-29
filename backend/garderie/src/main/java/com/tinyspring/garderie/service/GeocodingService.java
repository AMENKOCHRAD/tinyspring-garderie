package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ReverseGeocodeDto;

public interface GeocodingService {

    ReverseGeocodeDto reverse(Double lat, Double lng);
}

