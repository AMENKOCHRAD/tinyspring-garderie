package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeocodingServiceImplTest {

    @Test
    void reverse_refuseSiLatOuLngNull() {
        GeocodingServiceImpl service = new GeocodingServiceImpl(new ObjectMapper());
        assertThatThrownBy(() -> service.reverse(null, 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("lat/lng");
    }

    @Test
    void reverse_refuseSiCoordonneesHorsBornes() {
        GeocodingServiceImpl service = new GeocodingServiceImpl(new ObjectMapper());
        assertThatThrownBy(() -> service.reverse(200.0, 10.0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalides");
    }
}

