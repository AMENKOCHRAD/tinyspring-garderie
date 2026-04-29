from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import joblib
import pandas as pd

from app.models.schemas import (
    AnomalyDetectionRequest,
    AnomalyDetectionResponse,
    DemandPredictionRequest,
    DemandPredictionResponse,
)


class ModelServiceError(Exception):
    pass


@dataclass(frozen=True)
class LoadedModels:
    demand_model: object
    anomaly_model: object


class ModelService:
    def __init__(self) -> None:
        self.base_dir = Path(__file__).resolve().parents[2]
        self.models_dir = self.base_dir / "models"
        self.demand_model_path = self.models_dir / "demand_model.joblib"
        self.anomaly_model_path = self.models_dir / "anomaly_model.joblib"
        self.model_version = "2026.04"
        self._models = self._load_models()

    def predict_demand(self, payload: DemandPredictionRequest) -> DemandPredictionResponse:
        features = pd.DataFrame(
            [
                {
                    "day_of_week": payload.target_date.weekday(),
                    "month": payload.target_date.month,
                    "hour": payload.hour,
                    "active_children_count": payload.active_children_count,
                    "available_transport_count": payload.available_transport_count,
                    "avg_route_distance_km": payload.avg_route_distance_km,
                    "rain_flag": int(payload.rain_flag),
                    "school_break_flag": int(payload.school_break_flag),
                }
            ]
        )

        prediction = self._models.demand_model.predict(features)[0]
        predicted_count = max(0, int(round(float(prediction))))

        return DemandPredictionResponse(
            predicted_demand_count=predicted_count,
            demand_level=self._classify_demand_level(predicted_count),
            model_version=self.model_version,
        )

    def detect_anomaly(self, payload: AnomalyDetectionRequest) -> AnomalyDetectionResponse:
        pickup = payload.pickup_address.strip().lower()
        dropoff = payload.dropoff_address.strip().lower()

        duplicate_found = any(
            existing.child_id == payload.child_id
            and existing.request_date == payload.request_date
            and existing.requested_hour == payload.requested_hour
            and existing.pickup_address.strip().lower() == pickup
            and existing.dropoff_address.strip().lower() == dropoff
            for existing in payload.recent_requests
        )

        reasons: list[str] = []
        score = 0.0

        if pickup == dropoff:
            reasons.append("Adresse de depart identique a l adresse d arrivee.")
            score += 0.45

        if payload.requested_hour < 6 or payload.requested_hour > 21:
            reasons.append("Heure inhabituelle pour une demande de transport.")
            score += 0.25

        if duplicate_found:
            reasons.append("Doublon detecte par rapport aux demandes recentes.")
            score += 0.35

        if payload.seats_requested > 3:
            reasons.append("Nombre de places demande inhabituel pour un enfant.")
            score += 0.15

        if payload.route_distance_km > 80:
            reasons.append("Distance declaree anormalement elevee pour une demande locale.")
            score += 0.20

        if payload.is_round_trip and pickup == dropoff:
            reasons.append("Demande incoherente: aller-retour avec le meme point de depart et d arrivee.")
            score += 0.20

        ml_features = pd.DataFrame(
            [
                {
                    "requested_hour": payload.requested_hour,
                    "route_distance_km": payload.route_distance_km,
                    "seats_requested": payload.seats_requested,
                    "same_address_flag": int(pickup == dropoff),
                    "is_round_trip": int(payload.is_round_trip),
                    "week_day": payload.request_date.weekday(),
                }
            ]
        )

        ml_prediction = int(self._models.anomaly_model.predict(ml_features)[0])
        raw_ml_score = float(-self._models.anomaly_model.score_samples(ml_features)[0])
        normalized_ml_score = min(max(raw_ml_score / 0.7, 0.0), 1.0)

        if ml_prediction == -1:
            reasons.append("Le modele a classe la demande comme atypique.")
            score += 0.20

        final_score = min(max(score + normalized_ml_score * 0.35, 0.0), 1.0)
        is_anomaly = final_score >= 0.40 or len(reasons) > 0

        if not reasons:
            reasons.append("Aucune anomalie evidente detectee.")

        return AnomalyDetectionResponse(
            is_anomaly=is_anomaly,
            anomaly_score=round(final_score, 3),
            anomaly_level=self._classify_anomaly_level(final_score),
            reasons=reasons,
            duplicate_found=duplicate_found,
            model_version=self.model_version,
        )

    def _load_models(self) -> LoadedModels:
        if not self.demand_model_path.exists() or not self.anomaly_model_path.exists():
            raise ModelServiceError(
                "Modeles introuvables. Lancez d abord le script d entrainement: python -m app.training.train"
            )

        try:
            return LoadedModels(
                demand_model=joblib.load(self.demand_model_path),
                anomaly_model=joblib.load(self.anomaly_model_path),
            )
        except Exception as exc:
            raise ModelServiceError(f"Impossible de charger les modeles: {exc}") from exc

    @staticmethod
    def _classify_demand_level(predicted_count: int) -> str:
        if predicted_count < 12:
            return "LOW"
        if predicted_count < 25:
            return "MEDIUM"
        return "HIGH"

    @staticmethod
    def _classify_anomaly_level(score: float) -> str:
        if score < 0.30:
            return "LOW"
        if score < 0.60:
            return "MEDIUM"
        return "HIGH"
