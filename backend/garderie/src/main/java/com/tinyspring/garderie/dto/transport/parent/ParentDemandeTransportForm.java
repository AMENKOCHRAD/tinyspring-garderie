package com.tinyspring.garderie.dto.transport.parent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ParentDemandeTransportForm {

    @NotNull(message = "Veuillez selectionner un enfant")
    private Long enfantId;

    @NotNull(message = "Veuillez selectionner un trajet")
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
