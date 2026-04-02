package com.tinyspring.garderie.dto.transport;

import jakarta.validation.constraints.NotNull;

public class AccepterDemandeTransportRequest {

    @NotNull(message = "L'identifiant du transport est obligatoire")
    private Long transportId;

    public Long getTransportId() {
        return transportId;
    }

    public void setTransportId(Long transportId) {
        this.transportId = transportId;
    }
}
