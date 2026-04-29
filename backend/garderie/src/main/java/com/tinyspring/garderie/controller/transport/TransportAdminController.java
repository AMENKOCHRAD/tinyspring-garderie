package com.tinyspring.garderie.controller.transport;

import com.tinyspring.garderie.dto.transport.admin.AffectationTransportResponse;
import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.admin.DemandeAffectationRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.NouveauTrajetRecommendationResponse;
import com.tinyspring.garderie.dto.transport.admin.TrajetRequest;
import com.tinyspring.garderie.dto.transport.admin.TrajetResponse;
import com.tinyspring.garderie.dto.transport.admin.TransportRequest;
import com.tinyspring.garderie.dto.transport.admin.TransportResponse;
import com.tinyspring.garderie.service.transport.admin.TransportAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class TransportAdminController {

    private final TransportAdminService transportAdminService;

    public TransportAdminController(TransportAdminService transportAdminService) {
        this.transportAdminService = transportAdminService;
    }

    @GetMapping("/transports")
    public List<TransportResponse> getTransports() {
        return transportAdminService.listerTransports();
    }

    @PostMapping("/transports")
    public TransportResponse createTransport(@Valid @RequestBody TransportRequest request) {
        return transportAdminService.creerTransport(request);
    }

    @PutMapping("/transports/{id}")
    public TransportResponse updateTransport(@PathVariable Long id, @Valid @RequestBody TransportRequest request) {
        return transportAdminService.modifierTransport(id, request);
    }

    @DeleteMapping("/transports/{id}")
    public void deleteTransport(@PathVariable Long id) {
        transportAdminService.supprimerTransport(id);
    }

    @GetMapping("/trajets")
    public List<TrajetResponse> getTrajets() {
        return transportAdminService.listerTrajets();
    }

    @PostMapping("/trajets")
    public TrajetResponse createTrajet(@Valid @RequestBody TrajetRequest request) {
        return transportAdminService.creerTrajet(request);
    }

    @PutMapping("/trajets/{id}")
    public TrajetResponse updateTrajet(@PathVariable Long id, @Valid @RequestBody TrajetRequest request) {
        return transportAdminService.modifierTrajet(id, request);
    }

    @DeleteMapping("/trajets/{id}")
    public void deleteTrajet(@PathVariable Long id) {
        transportAdminService.supprimerTrajet(id);
    }

    @GetMapping("/affectations")
    public List<AffectationTransportResponse> getAffectations() {
        return transportAdminService.listerAffectations();
    }

    @GetMapping("/recommandations/affectations")
    public List<DemandeAffectationRecommendationResponse> getRecommandationsAffectation() {
        return transportAdminService.listerRecommandationsAffectation();
    }

    @GetMapping("/recommandations/nouveaux-trajets")
    public List<NouveauTrajetRecommendationResponse> getRecommandationsNouveauxTrajets() {
        return transportAdminService.listerRecommandationsNouveauxTrajets();
    }

    @GetMapping("/recommandations/prediction-demande")
    public AdminDemandPredictionResponse getPredictionDemande(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam(required = false) Integer hour,
            @RequestParam(defaultValue = "false") boolean rainFlag,
            @RequestParam(defaultValue = "false") boolean schoolBreakFlag
    ) {
        return transportAdminService.predireDemandeAdmin(targetDate, hour, rainFlag, schoolBreakFlag);
    }
}
