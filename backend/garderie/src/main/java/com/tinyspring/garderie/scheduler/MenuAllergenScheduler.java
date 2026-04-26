package com.tinyspring.garderie.scheduler;

import com.tinyspring.garderie.service.Events.MenuAllergenScheduledNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class MenuAllergenScheduler {

    private final MenuAllergenScheduledNotificationService service;

    // Chaque lundi à 08:00 : résumé de la semaine
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklySummary() {
        service.sendWeeklyAllergenSummary();
    }

    // Chaque jour à 08:00 : menu de demain
    @Scheduled(cron = "0 0 8 * * *")
    public void sendTomorrowAlert() {
        service.sendTomorrowAllergenAlerts();
    }
    @Scheduled(cron = "0 */1 * * * *")
    public void testEveryMinute() {
        service.sendTomorrowAllergenAlerts();
    }
}
