package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.ReverseGeocodeDto;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingServiceImpl implements GeocodingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeocodingServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public ReverseGeocodeDto reverse(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new RuntimeException("lat/lng obligatoires.");
        }

        // Tunisia bounds sanity check (optional): keep generic, don't reject hard.
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new RuntimeException("Coordonnees invalides.");
        }

        try {
            String url = "https://nominatim.openstreetmap.org/reverse"
                    + "?format=jsonv2"
                    + "&lat=" + lat
                    + "&lon=" + lng
                    + "&zoom=18"
                    + "&addressdetails=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    // Nominatim requires a valid User-Agent
                    .header("User-Agent", "TinySpring-Garderie/1.0 (reverse-geocode)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Reverse geocoding indisponible (HTTP " + response.statusCode() + ").");
            }

            String raw = new String(response.body() != null ? response.body() : new byte[0], StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(raw);

            ReverseGeocodeDto out = new ReverseGeocodeDto();
            out.lat = lat;
            out.lng = lng;
            out.adresse = root.path("display_name").asText(null);

            if (out.adresse == null || out.adresse.isBlank()) {
                out.adresse = "Lat " + lat + ", Lng " + lng;
            }

            return out;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Reverse geocoding indisponible.");
        }
    }
}
