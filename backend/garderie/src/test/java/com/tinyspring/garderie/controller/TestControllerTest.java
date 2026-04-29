package com.tinyspring.garderie.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestControllerTest {

    private final TestController controller = new TestController();

    @Test
    void shouldReturnPublicMessage() {
        assertEquals("API publique OK", controller.publicTest());
    }

    @Test
    void shouldReturnAdminMessage() {
        assertEquals("Bienvenue Admin", controller.adminTest());
    }

    @Test
    void shouldReturnParentMessage() {
        assertEquals("Bienvenue Parent", controller.parentTest());
    }

    @Test
    void shouldReturnAnimatriceMessage() {
        assertEquals("Bienvenue Animatrice", controller.animatriceTest());
    }
}
