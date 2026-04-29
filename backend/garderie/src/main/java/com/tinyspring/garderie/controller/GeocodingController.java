package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.ReverseGeocodeDto;
import com.tinyspring.garderie.service.GeocodingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/geocode")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping(value = "/reverse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReverseGeocodeDto> reverse(@RequestParam("lat") Double lat,
                                                     @RequestParam("lng") Double lng) {
        return ResponseEntity.ok(geocodingService.reverse(lat, lng));
    }
}

