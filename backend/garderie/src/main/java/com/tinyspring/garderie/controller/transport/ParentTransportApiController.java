package com.tinyspring.garderie.controller.transport;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.UpdateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.parent.ParentEnfantResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentTrajetResponse;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.exception.ResourceNotFoundException;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.transport.TransportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ParentTransportApiController {

    private final TransportService transportService;
    private final UserRepository userRepository;

    public ParentTransportApiController(TransportService transportService, UserRepository userRepository) {
        this.transportService = transportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/demandes")
    public List<DemandeTransportResponse> getParentDemandes(Authentication authentication) {
        return transportService.listerDemandesParParent(getCurrentUser(authentication).getId());
    }

    @PostMapping("/demandes")
    public DemandeTransportResponse createDemande(@Valid @RequestBody CreateDemandeTransportRequest request,
                                                  Authentication authentication) {
        return transportService.creerDemandeTransport(getCurrentUser(authentication).getId(), request);
    }

    @PutMapping("/demandes/{id}")
    public DemandeTransportResponse updateDemande(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateDemandeTransportRequest request,
                                                  Authentication authentication) {
        return transportService.modifierDemandeTransport(getCurrentUser(authentication).getId(), id, request);
    }

    @DeleteMapping("/demandes/{id}")
    public void deleteDemande(@PathVariable Long id, Authentication authentication) {
        transportService.supprimerDemandeTransport(getCurrentUser(authentication).getId(), id);
    }

    @GetMapping("/trajets")
    public List<ParentTrajetResponse> getTrajets() {
        return transportService.listerTrajetsDisponibles();
    }

    @GetMapping("/parent/enfants")
    public List<ParentEnfantResponse> getEnfants(Authentication authentication) {
        return transportService.listerEnfantsParParent(getCurrentUser(authentication).getId());
    }

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable pour l'email " + email));
    }
}
