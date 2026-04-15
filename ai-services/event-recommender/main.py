from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
import joblib
import traceback

MODEL_PATH = "model/event_recommender_pipeline.pkl"
LABEL_ENCODER_PATH = "model/label_encoder.pkl"

pipeline = joblib.load(MODEL_PATH)
label_encoder = joblib.load(LABEL_ENCODER_PATH)

app = FastAPI(title="TinySpring Event Recommendation API")


class CandidateEvent(BaseModel):
    title: str
    description: str
    event_type: str
    suggested_location: str
    indoor_outdoor: str
    requires_authorization: bool
    suggested_price: float


class RecommendationRequest(BaseModel):
    season: str
    month: str
    age_group: str
    budget_level: str
    outdoor_preferred: bool
    city_context: str
    candidates: List[CandidateEvent]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/recommend-events")
def recommend_events(request: RecommendationRequest):
    try:
        rows = []

        for candidate in request.candidates:
            row = {
                "season": request.season,
                "month": request.month,
                "age_group": request.age_group,
                "budget_level": request.budget_level,
                "outdoor_preferred": request.outdoor_preferred,
                "city_context": request.city_context,
                "event_type": candidate.event_type,
                "indoor_outdoor": candidate.indoor_outdoor,
                "requires_authorization": candidate.requires_authorization,
                "price": candidate.suggested_price,
                "text": f"{candidate.title} {candidate.description} {candidate.suggested_location}"
            }
            rows.append(row)

        X = pd.DataFrame(rows)

        print("=== DATAFRAME SENT TO MODEL ===")
        print(X.columns.tolist())
        print(X.head())

        predicted_labels_encoded = pipeline.predict(X)
        predicted_probabilities = pipeline.predict_proba(X)

        results = []
        for i, candidate in enumerate(request.candidates):
            label = label_encoder.inverse_transform([predicted_labels_encoded[i]])[0]
            score = float(max(predicted_probabilities[i]))

            results.append({
                "title": candidate.title,
                "description": candidate.description,
                "event_type": candidate.event_type,
                "suggested_location": candidate.suggested_location,
                "indoor_outdoor": candidate.indoor_outdoor,
                "requires_authorization": candidate.requires_authorization,
                "suggested_price": candidate.suggested_price,
                "predicted_label": label,
                "relevance_score": round(score, 4)
            })

        results.sort(key=lambda x: x["relevance_score"], reverse=True)
        return results

    except Exception as e:
        print("=== PYTHON ERROR ===")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))