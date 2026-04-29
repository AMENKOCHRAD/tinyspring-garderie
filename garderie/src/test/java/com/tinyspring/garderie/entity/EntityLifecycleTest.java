package com.tinyspring.garderie.entity;

import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;
import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import com.tinyspring.garderie.entity.enums.SmartPriorityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntityLifecycleTest {

    @Test
    void reclamationPrePersistSetsDefaultBusinessValues() {
        Reclamation reclamation = new Reclamation();

        reclamation.prePersist();

        assertNotNull(reclamation.getCreatedAt());
        assertNotNull(reclamation.getUpdatedAt());
        assertEquals(ReclamationStatus.OPEN, reclamation.getStatus());
        assertEquals(ReclamationPriority.MEDIUM, reclamation.getPriority());
        assertEquals(ReclamationCategory.AUTRE, reclamation.getCategory());
        assertFalse(reclamation.getAutoClassified());
        assertFalse(reclamation.getRecurring());
        assertEquals(0, reclamation.getRecurrenceCount());
        assertEquals(0, reclamation.getSmartPriorityScore());
        assertEquals(SmartPriorityLevel.LOW, reclamation.getSmartPriorityLevel());
        assertFalse(reclamation.getAutoEscalated());
    }

    @Test
    void conversationPrePersistSetsDatesAndDefaults() {
        Conversation conversation = new Conversation();

        conversation.prePersist();

        assertNotNull(conversation.getCreatedAt());
        assertNotNull(conversation.getUpdatedAt());
        assertEquals(ConversationStatus.OPEN, conversation.getStatus());
        assertEquals(ConversationType.NORMAL, conversation.getType());
    }

    @Test
    void messagePrePersistSetsSentAtAndReadStatus() {
        Message message = new Message();

        message.prePersist();

        assertNotNull(message.getSentAt());
        assertFalse(message.getIsRead());
    }

    @Test
    void reclamationHistoryPrePersistSetsCreatedAt() {
        ReclamationHistory history = new ReclamationHistory();

        history.prePersist();

        assertNotNull(history.getCreatedAt());
    }
}
