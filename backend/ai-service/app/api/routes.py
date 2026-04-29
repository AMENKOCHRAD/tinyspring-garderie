from fastapi import APIRouter, HTTPException

from app.models.schemas import (
    AnomalyDetectionRequest,
    AnomalyDetectionResponse,
    DemandPredictionRequest,
    DemandPredictionResponse,
)
from app.services.model_service import ModelService, ModelServiceError

router = APIRouter()
model_service = ModelService()


@router.post("/predict-demand", response_model=DemandPredictionResponse, tags=["prediction"])
def predict_demand(payload: DemandPredictionRequest) -> DemandPredictionResponse:
    try:
        return model_service.predict_demand(payload)
    except ModelServiceError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Erreur inattendue lors de la prediction de la demande.") from exc


@router.post("/detect-anomaly", response_model=AnomalyDetectionResponse, tags=["anomaly"])
def detect_anomaly(payload: AnomalyDetectionRequest) -> AnomalyDetectionResponse:
    try:
        return model_service.detect_anomaly(payload)
    except ModelServiceError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Erreur inattendue lors de la detection d anomalie.") from exc
