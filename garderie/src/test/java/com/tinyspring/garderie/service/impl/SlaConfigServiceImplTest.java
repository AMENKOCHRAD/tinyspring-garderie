package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlaConfigServiceImplTest {

    private final SlaConfigServiceImpl service = new SlaConfigServiceImpl();

    @Test
    void getSlaHoursReturnsBusinessDelayByCategory() {
        assertEquals(4, service.getSlaHours(ReclamationCategory.SECURITE));
        assertEquals(8, service.getSlaHours(ReclamationCategory.TRANSPORT));
        assertEquals(96, service.getSlaHours(ReclamationCategory.AUTRE));
        assertEquals(96, service.getSlaHours(null));
    }

    @Test
    void getSlaProgressPercentCalculatesRoundedProgress() {
        assertEquals(50.0, service.getSlaProgressPercent(ReclamationCategory.SECURITE, 120));
        assertEquals(100.0, service.getSlaProgressPercent(ReclamationCategory.SECURITE, 240));
        assertEquals(125.0, service.getSlaProgressPercent(ReclamationCategory.SECURITE, 300));
    }

    @Test
    void isSlaBreachedDetectsExceededDelay() {
        assertFalse(service.isSlaBreached(ReclamationCategory.REPAS, 360));
        assertTrue(service.isSlaBreached(ReclamationCategory.REPAS, 720));
    }

    @Test
    void getSlaStatusLabelCoversMainSlaStates() {
        assertTrue(service.getSlaStatusLabel(ReclamationCategory.SECURITE, 30).startsWith("SLA OK"));
        assertTrue(service.getSlaStatusLabel(ReclamationCategory.SECURITE, 130).startsWith("SLA EN DANGER"));
        assertTrue(service.getSlaStatusLabel(ReclamationCategory.SECURITE, 190).startsWith("SLA CRITIQUE"));
        assertTrue(service.getSlaStatusLabel(ReclamationCategory.SECURITE, 250).startsWith("SLA D"));
    }
}
