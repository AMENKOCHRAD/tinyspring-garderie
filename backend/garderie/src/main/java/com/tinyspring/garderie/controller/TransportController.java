package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.transport.AccepterDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.TrajetDetailsResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.service.TransportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = "http://localhost:4200")
public class TransportController {

    private final TransportService transportService;

    public TransportController(TransportService transportService) {
        this.transportService = transportService;
    }

    @GetMapping("/demandes")
    public List<DemandeTransportResponse> getDemandes() {
        return transportService.listerToutesLesDemandes();
    }

    @PutMapping("/accepter/{id}")
    public TraitementDemandeTransportResponse accepterDemande(@PathVariable Long id,
                                                              @Valid @RequestBody AccepterDemandeTransportRequest request) {
        return transportService.accepterDemande(id, request.getTransportId());
    }

    @PutMapping("/refuser/{id}")
    public TraitementDemandeTransportResponse refuserDemande(@PathVariable Long id) {
        return transportService.refuserDemande(id);
    }

    @GetMapping("/trajet/{id}")
    public TrajetDetailsResponse getEnfantsParTrajet(@PathVariable Long id) {
        return transportService.listerEnfantsParTrajet(id);
    }
}
