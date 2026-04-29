package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.TraitementValidationEventDto;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.service.TraitementService;
import com.tinyspring.garderie.service.TraitementValidationHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementControllerTest {

    @Mock
    TraitementService traitementService;

    @Mock
    TraitementValidationHistoryService validationHistoryService;

    @InjectMocks
    TraitementController controller;

    @Test
    void latestEvents_retourneListeDuService() {
        when(validationHistoryService.getLatest(10)).thenReturn(List.of(new TraitementValidationEventDto()));
        var resp = controller.latestEvents(10);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void telechargerOrdonnance_admin_utiliseEndpointAdmin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn((java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        ByteArrayResource res = new ByteArrayResource(new byte[]{1, 2});
        when(traitementService.chargerOrdonnanceResourceAdmin(5L)).thenReturn(res);

        var resp = controller.telechargerOrdonnance(5L, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(traitementService).chargerOrdonnanceResourceAdmin(5L);
        verify(traitementService, never()).chargerOrdonnanceResourceParParent(any(), anyLong());
    }

    @Test
    void telechargerOrdonnance_parent_utiliseEndpointParent() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn((java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
        when(auth.getName()).thenReturn("p@test.com");

        ByteArrayResource res = new ByteArrayResource(new byte[]{1});
        when(traitementService.chargerOrdonnanceResourceParParent("p@test.com", 6L)).thenReturn(res);

        var resp = controller.telechargerOrdonnance(6L, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(traitementService).chargerOrdonnanceResourceParParent("p@test.com", 6L);
        verify(traitementService, never()).chargerOrdonnanceResourceAdmin(anyLong());
    }

    @Test
    void refuser_prendEmailDepuisAuthentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(traitementService.refuserTraitementAdmin(2L, "admin@test.com", "n")).thenReturn(new Traitement());

        var resp = controller.refuser(2L, "n", auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(traitementService).refuserTraitementAdmin(2L, "admin@test.com", "n");
    }
}
