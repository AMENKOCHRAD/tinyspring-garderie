package com.tinyspring.garderie.config.transport;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.transport.recommandation")
public class TransportRecommendationProperties {

    private double distanceMaxKm = 5.0;
    private double rayonRegroupementKm = 2.0;
    private int nombreMinimalDemandesPourNouveauTrajet = 3;
    private int scoreTexteMinimal = 60;

    public double getDistanceMaxKm() {
        return distanceMaxKm;
    }

    public void setDistanceMaxKm(double distanceMaxKm) {
        this.distanceMaxKm = distanceMaxKm;
    }

    public double getRayonRegroupementKm() {
        return rayonRegroupementKm;
    }

    public void setRayonRegroupementKm(double rayonRegroupementKm) {
        this.rayonRegroupementKm = rayonRegroupementKm;
    }

    public int getNombreMinimalDemandesPourNouveauTrajet() {
        return nombreMinimalDemandesPourNouveauTrajet;
    }

    public void setNombreMinimalDemandesPourNouveauTrajet(int nombreMinimalDemandesPourNouveauTrajet) {
        this.nombreMinimalDemandesPourNouveauTrajet = nombreMinimalDemandesPourNouveauTrajet;
    }

    public int getScoreTexteMinimal() {
        return scoreTexteMinimal;
    }

    public void setScoreTexteMinimal(int scoreTexteMinimal) {
        this.scoreTexteMinimal = scoreTexteMinimal;
    }
}
