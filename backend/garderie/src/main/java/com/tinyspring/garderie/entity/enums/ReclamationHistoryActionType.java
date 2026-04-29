package com.tinyspring.garderie.entity.enums;

public enum ReclamationHistoryActionType {
    CREATED,
    TITLE_CHANGED,
    DESCRIPTION_UPDATED,
    CATEGORY_CHANGED,
    PRIORITY_CHANGED,
    STATUS_CHANGED,
    DECISION_RECOMMENDED,
    ADMIN_COMMENT_ADDED,
    ADMIN_COMMENT_UPDATED,
    ADMIN_COMMENT_REMOVED,
    RECURRENCE_DETECTED,
    SMART_PRIORITY_CALCULATED,

    // Escalade automatique intelligente
    ESCALATION_TRIGGERED
}