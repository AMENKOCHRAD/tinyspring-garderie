package com.tinyspring.garderie.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BadWordFilterServiceImplTest {

    private final BadWordFilterServiceImpl service = new BadWordFilterServiceImpl();

    @Test
    void censorTextReplacesBadWordsCaseInsensitive() {
        assertEquals("Ce message est ***.", service.censorText("Ce message est idiot."));
        assertEquals("*** et ***", service.censorText("NUL et stupide"));
    }

    @Test
    void censorTextKeepsCleanOrBlankText() {
        assertEquals("Bonjour parent", service.censorText("Bonjour parent"));
        assertEquals("", service.censorText(""));
        assertEquals("   ", service.censorText("   "));
        assertNull(service.censorText(null));
    }

    @Test
    void containsBadWordDetectsWholeWordsOnly() {
        assertTrue(service.containsBadWord("message avec haine"));
        assertTrue(service.containsBadWord("message SALE"));
        assertFalse(service.containsBadWord("analyse propre"));
        assertFalse(service.containsBadWord("idiotie n'est pas le mot complet"));
        assertFalse(service.containsBadWord(null));
        assertFalse(service.containsBadWord("   "));
    }
}
