from datetime import date
from typing import Literal

from pydantic import BaseModel, Field, field_validator, model_validator


class DemandPredictionRequest(BaseModel):
    target_date: date = Field(..., description="Date a predire au format YYYY-MM-DD")
    hour: int = Field(..., ge=0, le=23, description="Heure principale de la plage de demande")
    active_children_count: int = Field(..., ge=0, description="Nombre d enfants actifs")
    available_transport_count: int = Field(..., ge=0, description="Nombre de transports disponibles")
    avg_route_distance_km: float = Field(..., ge=0, le=200, description="Distance moyenne des trajets")
    rain_flag: bool = Field(default=False, description="Indique si la meteo annonce de la pluie")
    school_break_flag: bool = Field(default=False, description="Indique une periode de vacances scolaires")


class DemandPredictionResponse(BaseModel):
    predicted_demand_count: int
    demand_level: Literal["LOW", "MEDIUM", "HIGH"]
    model_version: str


class ExistingRequest(BaseModel):
    child_id: int = Field(..., ge=1)
    request_date: date
    requested_hour: int = Field(..., ge=0, le=23)
    pickup_address: str = Field(..., min_length=3, max_length=255)
    dropoff_address: str = Field(..., min_length=3, max_length=255)

    @field_validator("pickup_address", "dropoff_address")
    @classmethod
    def normalize_addresses(cls, value: str) -> str:
        return " ".join(value.strip().split())


class AnomalyDetectionRequest(BaseModel):
    child_id: int = Field(..., ge=1)
    request_date: date
    requested_hour: int = Field(..., ge=0, le=23)
    pickup_address: str = Field(..., min_length=3, max_length=255)
    dropoff_address: str = Field(..., min_length=3, max_length=255)
    seats_requested: int = Field(default=1, ge=1, le=10)
    route_distance_km: float = Field(..., ge=0, le=300)
    is_round_trip: bool = Field(default=False)
    recent_requests: list[ExistingRequest] = Field(default_factory=list)

    @field_validator("pickup_address", "dropoff_address")
    @classmethod
    def cleanup_addresses(cls, value: str) -> str:
        return " ".join(value.strip().split())

    @model_validator(mode="after")
    def validate_consistency(self) -> "AnomalyDetectionRequest":
        if self.route_distance_km == 0 and self.pickup_address.lower() != self.dropoff_address.lower():
            raise ValueError("La distance ne peut pas etre nulle si les adresses sont differentes.")
        return self


class AnomalyDetectionResponse(BaseModel):
    is_anomaly: bool
    anomaly_score: float
    anomaly_level: Literal["LOW", "MEDIUM", "HIGH"]
    reasons: list[str]
    duplicate_found: bool
    model_version: str
