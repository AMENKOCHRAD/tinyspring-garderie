package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentObservationsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    EnfantRepository enfantRepository;

    @Mock
    ObservationEnfantRepository observationEnfantRepository;

    @Mock
    RealtimeNotificationService realtimeNotificationService;

    @InjectMocks
    ParentObservationsServiceImpl service;

    @Test
    void listerObservationsPourParent_refuseSiParentNePossedePasEnfant() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));

        Enfant enfant = new Enfant();
        User autreParent = new User();
        autreParent.setEmail("autre@test.com");
        enfant.setParent(autreParent);
        when(enfantRepository.findById(3L)).thenReturn(Optional.of(enfant));

        assertThatThrownBy(() -> service.listerObservationsPourParent("p@test.com", 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Acces interdit");
    }

    @Test
    void listerObservationsParent_unreadOnly_choisitBonneRequete() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));
        when(observationEnfantRepository.findTop200ByEnfantParentEmailIgnoreCaseAndLuParentFalseOrderByCreeLeDesc("p@test.com"))
                .thenReturn(List.of(new ObservationEnfant(), new ObservationEnfant()));

        List<ObservationEnfant> out = service.listerObservationsParent("p@test.com", true);

        assertThat(out).hasSize(2);
        verify(observationEnfantRepository).findTop200ByEnfantParentEmailIgnoreCaseAndLuParentFalseOrderByCreeLeDesc("p@test.com");
        verify(observationEnfantRepository, never()).findTop200ByEnfantParentEmailIgnoreCaseOrderByCreeLeDesc(any());
    }

    @Test
    void marquerLue_metLuParentEtNotifieSiPasDejaLu() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));

        ObservationEnfant obs = new ObservationEnfant();
        obs.setLuParent(false);
        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("p@test.com");
        enfant.setParent(parent);
        obs.setEnfant(enfant);

        when(observationEnfantRepository.findById(5L)).thenReturn(Optional.of(obs));
        when(observationEnfantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ObservationEnfant out = service.marquerLue("p@test.com", 5L);

        assertThat(out.isLuParent()).isTrue();
        verify(observationEnfantRepository).save(obs);
        verify(realtimeNotificationService).notifyParentUnreadCountChanged("p@test.com");
    }
}

