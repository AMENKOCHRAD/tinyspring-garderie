from datetime import datetime
from typing import List, Optional
import os
import pickle

import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


MODEL_DIR = os.getenv("MODEL_DIR", "models")
DATA_PATH = os.getenv("EVENT_DATASET_PATH", "data/event_templates_tunisia_enriched.csv")
MODEL_PATH = os.path.join(MODEL_DIR, "pipeline.pkl")
LABEL_ENCODER_PATH = os.path.join(MODEL_DIR, "label_encoder.pkl")

with open(MODEL_PATH, "rb") as f:
    PIPELINE = pickle.load(f)

with open(LABEL_ENCODER_PATH, "rb") as f:
    LABEL_ENCODER = pickle.load(f)

DATASET = pd.read_csv(DATA_PATH)

app = FastAPI(title="TinySpring Event Recommendation API")


class RecommendationRequest(BaseModel):
    season: Optional[str] = None
    month: Optional[str] = None
    age_group: str
    budget_level: str
    outdoor_preferred: bool
    city_context: str
    top_n: int = 8


class RecommendationItem(BaseModel):
    title: str
    description: str
    event_type: str
    suggested_location: str
    indoor_outdoor: str
    requires_authorization: bool
    suggested_price: float
    predicted_label: str
    relevance_score: float
    venue_type: str
    season: str
    tags: str
    match_reason: str


def normalize_text(value) -> str:
    if value is None:
        return ""
    return str(value).strip().lower()


def extract_age_target(age_group: str) -> int:
    text = normalize_text(age_group)
    digits = []
    current = ""
    for ch in text:
        if ch.isdigit():
            current += ch
        else:
            if current:
                digits.append(int(current))
                current = ""
    if current:
        digits.append(int(current))
    if not digits:
        return 4
    return digits[0]


def budget_limit(budget_level: str) -> float:
    level = normalize_text(budget_level)
    if level in {"low", "faible"}:
        return 20.0
    if level in {"medium", "moyen"}:
        return 40.0
    return 9999.0


def normalize_requires_authorization(value) -> bool:
    text = normalize_text(value)
    return text in {"true", "1", "yes", "oui"}


def encode_educational_value(value) -> int:
    mapping = {
        "low": 0,
        "medium": 1,
        "high": 2,
        "very_high": 3
    }
    return mapping.get(normalize_text(value), 1)


def get_current_season_from_month(month_str: Optional[str]) -> str:
    if month_str:
        text = normalize_text(month_str)
        month_map = {
            "january": 1, "janvier": 1,
            "february": 2, "fevrier": 2, "février": 2,
            "march": 3, "mars": 3,
            "april": 4, "avril": 4,
            "may": 5, "mai": 5,
            "june": 6, "juin": 6,
            "july": 7, "juillet": 7,
            "august": 8, "aout": 8, "août": 8,
            "september": 9, "septembre": 9,
            "october": 10, "octobre": 10,
            "november": 11, "novembre": 11,
            "december": 12, "decembre": 12, "décembre": 12,
        }
        month_num = month_map.get(text)
        if month_num is None and text.isdigit():
            month_num = int(text)
    else:
        month_num = datetime.now().month

    if month_num in {3, 4, 5}:
        return "PRINTEMPS"
    if month_num in {6, 7, 8}:
        return "ETE"
    if month_num in {9, 10, 11}:
        return "AUTOMNE"
    return "HIVER"


def filter_candidates(request: RecommendationRequest) -> pd.DataFrame:
    df = DATASET.copy()

    city = normalize_text(request.city_context)
    season = normalize_text(request.season) if request.season else normalize_text(get_current_season_from_month(request.month))
    age_target = extract_age_target(request.age_group)
    max_budget = budget_limit(request.budget_level)

    df["city_norm"] = df["city"].fillna("").astype(str).str.lower()
    df["city_aliases_norm"] = df.get("city_aliases", "").fillna("").astype(str).str.lower()
    df["season_norm"] = df["season"].fillna("").astype(str).str.lower()
    df["indoor_outdoor_norm"] = df["indoor_outdoor"].fillna("").astype(str).str.lower()

    city_mask = df["city_norm"].str.contains(city, na=False) | df["city_aliases_norm"].str.contains(city, na=False)
    season_mask = (df["season_norm"] == season) | (df["season_norm"] == "all")
    budget_mask = pd.to_numeric(df["price_dt"], errors="coerce").fillna(0) <= max_budget
    age_mask = (
        pd.to_numeric(df["age_min"], errors="coerce").fillna(2) <= age_target
    ) & (
        pd.to_numeric(df["age_max"], errors="coerce").fillna(6) >= age_target
    )

    if request.outdoor_preferred:
        pref_mask = df["indoor_outdoor_norm"].isin(["outdoor", "both"])
    else:
        pref_mask = df["indoor_outdoor_norm"].isin(["indoor", "both"])

    strict = df[city_mask & season_mask & budget_mask & age_mask & pref_mask]
    if len(strict) >= 5:
        return strict.reset_index(drop=True)

    relaxed = df[city_mask & season_mask & budget_mask & age_mask]
    if len(relaxed) >= 5:
        return relaxed.reset_index(drop=True)

    wider = df[season_mask & budget_mask & age_mask]
    if len(wider) >= 5:
        return wider.reset_index(drop=True)

    return df[budget_mask & age_mask].reset_index(drop=True)


def prepare_features(candidates: pd.DataFrame) -> pd.DataFrame:
    df = candidates.copy()
    df["requires_authorization_encoded"] = df["requires_authorization"].apply(normalize_requires_authorization).astype(int)
    df["educational_value_encoded"] = df["educational_value"].apply(encode_educational_value)
    df["price_dt"] = pd.to_numeric(df["price_dt"], errors="coerce").fillna(0.0)
    df["age_min"] = pd.to_numeric(df["age_min"], errors="coerce").fillna(2)
    df["age_max"] = pd.to_numeric(df["age_max"], errors="coerce").fillna(6)
    df["duration_minutes"] = pd.to_numeric(df["duration_minutes"], errors="coerce").fillna(60)
    df["combined_text"] = (
        df["title"].fillna("").astype(str) + " " +
        df["description"].fillna("").astype(str) + " " +
        df["location"].fillna("").astype(str) + " " +
        df["city"].fillna("").astype(str) + " " +
        df["venue_type"].fillna("").astype(str) + " " +
        df["tags"].fillna("").astype(str).str.replace(";", " ", regex=False)
    )
    return df[[
        "type",
        "venue_type",
        "indoor_outdoor",
        "season",
        "budget_level",
        "city",
        "price_dt",
        "age_min",
        "age_max",
        "duration_minutes",
        "educational_value_encoded",
        "requires_authorization_encoded",
        "combined_text"
    ]]


def compute_bonus(row, request: RecommendationRequest) -> float:
    bonus = 0.0
    season = normalize_text(request.season) if request.season else normalize_text(get_current_season_from_month(request.month))
    row_season = normalize_text(row.get("season"))
    if row_season == season:
        bonus += 0.12
    elif row_season == "all":
        bonus += 0.05

    if request.outdoor_preferred and normalize_text(row.get("indoor_outdoor")) == "outdoor":
        bonus += 0.10
    if (not request.outdoor_preferred) and normalize_text(row.get("indoor_outdoor")) == "indoor":
        bonus += 0.10

    if float(row.get("price_dt", 0)) <= budget_limit(request.budget_level) * 0.6:
        bonus += 0.05

    if encode_educational_value(row.get("educational_value")) >= 2:
        bonus += 0.05

    return min(bonus, 0.25)


def build_reason(row, request: RecommendationRequest) -> str:
    reasons = []
    season = normalize_text(request.season) if request.season else normalize_text(get_current_season_from_month(request.month))
    if normalize_text(row.get("season")) in {season, "all"}:
        reasons.append("bonne adéquation saisonnière")
    if request.outdoor_preferred and normalize_text(row.get("indoor_outdoor")) == "outdoor":
        reasons.append("correspond à la préférence extérieur")
    if (not request.outdoor_preferred) and normalize_text(row.get("indoor_outdoor")) == "indoor":
        reasons.append("correspond à la préférence intérieur")
    if float(row.get("price_dt", 0)) <= budget_limit(request.budget_level):
        reasons.append("compatible avec le budget")
    if encode_educational_value(row.get("educational_value")) >= 2:
        reasons.append("bonne valeur éducative")

    if not reasons:
        return "activité globalement compatible avec le contexte"
    return ", ".join(reasons)


@app.get("/health")
def health():
    return {"status": "ok", "dataset_size": len(DATASET)}


@app.post("/recommend-events", response_model=List[RecommendationItem])
def recommend_events(request: RecommendationRequest):
    try:
        candidates = filter_candidates(request)
        if candidates.empty:
            raise HTTPException(status_code=404, detail="No matching events found")

        X = prepare_features(candidates)

        predicted_labels_encoded = PIPELINE.predict(X)
        predicted_probabilities = PIPELINE.predict_proba(X)

        classes = list(LABEL_ENCODER.classes_)
        highly_recommended_index = classes.index("HIGHLY_RECOMMENDED") if "HIGHLY_RECOMMENDED" in classes else 0

        results = []
        for i, row in candidates.reset_index(drop=True).iterrows():
            predicted_label = LABEL_ENCODER.inverse_transform([predicted_labels_encoded[i]])[0]
            ml_score = float(predicted_probabilities[i][highly_recommended_index])
            final_score = min(ml_score + compute_bonus(row, request), 1.0)

            results.append({
                "title": row["title"],
                "description": row["description"],
                "event_type": row["type"],
                "suggested_location": row["location"],
                "indoor_outdoor": row["indoor_outdoor"],
                "requires_authorization": normalize_requires_authorization(row["requires_authorization"]),
                "suggested_price": float(row["price_dt"]),
                "predicted_label": predicted_label,
                "relevance_score": round(final_score, 4),
                "venue_type": row["venue_type"],
                "season": row["season"],
                "tags": row["tags"],
                "match_reason": build_reason(row, request),
            })

        results.sort(key=lambda item: item["relevance_score"], reverse=True)
        return results[:request.top_n]

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
