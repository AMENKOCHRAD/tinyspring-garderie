package com.tinyspring.garderie.dto.transport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateDemandeTransportRequest {

    @NotNull(message = "L'identifiant de l'enfant est obligatoire")
    private Long enfantId;

    @NotNull(message = "L'identifiant du trajet est obligatoire")
    private Long trajetId;

    @NotBlank(message = "Le point de ramassage est obligatoire")
    private String pointRamassage;

    public Long getEnfantId() {
        return enfantId;
    }

    public void setEnfantId(Long enfantId) {
        this.enfantId = enfantId;
    }

    public Long getTrajetId() {
        return trajetId;
    }

    public void setTrajetId(Long trajetId) {
        this.trajetId = trajetId;
    }

    public String getPointRamassage() {
        return pointRamassage;
    }

    public void setPointRamassage(String pointRamassage) {
        this.pointRamassage = pointRamassage;
    }
}
