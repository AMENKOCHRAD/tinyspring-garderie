package com.tinyspring.garderie.service.transport.scheduler;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TransportMonitoringSchedulerTest {

    @Mock
    private DemandeTransportRepository demandeTransportRepository;

    @InjectMocks
    private TransportMonitoringScheduler scheduler;

    @Test
    void shouldLogPendingDemandesSummary(CapturedOutput output) {
        when(demandeTransportRepository.findDemandesByStatutOrderByOldestFirst(eq(StatutDemandeTransport.EN_ATTENTE)))
                .thenReturn(List.of());
        when(demandeTransportRepository.countSuspiciousDemandesByStatut(eq(StatutDemandeTransport.EN_ATTENTE)))
                .thenReturn(2L);

        scheduler.monitorPendingDemandes();

        verify(demandeTransportRepository).findDemandesByStatutOrderByOldestFirst(StatutDemandeTransport.EN_ATTENTE);
        verify(demandeTransportRepository).countSuspiciousDemandesByStatut(StatutDemandeTransport.EN_ATTENTE);
        assertTrue(output.getOut().contains("Scheduler transport: 0 demandes en attente, 2 suspectes."));
    }

    @Test
    void shouldWarnWhenOldestPendingDemandeIsFromPreviousDay(CapturedOutput output) {
        DemandeTransport overdueDemande = buildDemande(11L, 21L, 31L, LocalDate.now().minusDays(1));

        when(demandeTransportRepository.findDemandesByStatutOrderByOldestFirst(eq(StatutDemandeTransport.EN_ATTENTE)))
                .thenReturn(List.of(overdueDemande));
        when(demandeTransportRepository.countSuspiciousDemandesByStatut(eq(StatutDemandeTransport.EN_ATTENTE)))
                .thenReturn(1L);

        scheduler.monitorPendingDemandes();

        assertTrue(output.getOut().contains("Scheduler transport: 1 demandes en attente, 1 suspectes."));
        assertTrue(output.getOut().contains("demandeId=11"));
        assertTrue(output.getOut().contains("enfantId=21"));
        assertTrue(output.getOut().contains("parentId=31"));
    }

    private DemandeTransport buildDemande(Long demandeId, Long enfantId, Long parentId, LocalDate dateDemande) {
        Role role = new Role(RoleName.PARENT);
        User parent = new User("Parent test", "parent" + parentId + "@test.tn", "secret", true, role);
        setId(parent, parentId);

        Enfant enfant = new Enfant("Doe", "John", parent);
        setId(enfant, enfantId);

        DemandeTransport demande = new DemandeTransport();
        demande.setParent(parent);
        demande.setEnfant(enfant);
        demande.setDateDemande(dateDemande);
        demande.setStatut(StatutDemandeTransport.EN_ATTENTE);
        setId(demande, demandeId);
        return demande;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
