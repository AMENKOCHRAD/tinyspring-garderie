import pickle
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional

app = FastAPI()

pipeline = pickle.load(open("model/pipeline.pkl", "rb"))
le = pickle.load(open("model/label_encoder.pkl", "rb"))

df = pd.read_csv("data/tunisia_daycare_events_140.csv")


class RecommendationRequest(BaseModel):
    season: Optional[str] = None
    month: Optional[str] = None
    age_group: str
    budget_level: str
    outdoor_preferred: bool
    city_context: str
    top_n: int = 8


def normalize_text(value):
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

    return digits[0] if digits else 4


def budget_limit(budget_level: str) -> float:
    level = normalize_text(budget_level)
    if level in {"low", "faible"}:
        return 20.0
    if level in {"medium", "moyen"}:
        return 40.0
    return 9999.0


def safe_float(value):
    if pd.isna(value):
        return None
    try:
        return float(value)
    except Exception:
        return None


@app.get("/health")
def health():
    return {"status": "ok", "rows": len(df)}


@app.post("/recommend-events")
def recommend(request: RecommendationRequest):
    df_copy = df.copy()

    # Nettoyage colonnes
    df_copy["city"] = df_copy["city"].fillna("").astype(str)
    df_copy["city_aliases"] = df_copy["city_aliases"].fillna("").astype(str)
    df_copy["indoor_outdoor"] = df_copy["indoor_outdoor"].fillna("").astype(str)
    df_copy["season"] = df_copy["season"].fillna("").astype(str)
    df_copy["price_dt"] = pd.to_numeric(df_copy["price_dt"], errors="coerce").fillna(0)
    df_copy["age_min"] = pd.to_numeric(df_copy["age_min"], errors="coerce").fillna(2)
    df_copy["age_max"] = pd.to_numeric(df_copy["age_max"], errors="coerce").fillna(6)
    df_copy["duration_minutes"] = pd.to_numeric(df_copy["duration_minutes"], errors="coerce").fillna(60)

    requested_city = normalize_text(request.city_context)
    requested_season = normalize_text(request.season)
    age_target = extract_age_target(request.age_group)
    max_budget = budget_limit(request.budget_level)

    # Filtres progressifs, pas trop stricts
    city_mask = (
        df_copy["city"].str.lower().str.contains(requested_city, na=False)
        | df_copy["city_aliases"].str.lower().str.contains(requested_city, na=False)
    ) if requested_city else pd.Series([True] * len(df_copy))

    season_mask = (
        df_copy["season"].str.lower().isin([requested_season, "all"])
    ) if requested_season else pd.Series([True] * len(df_copy))

    budget_mask = df_copy["price_dt"] <= max_budget

    age_mask = (
        (df_copy["age_min"] <= age_target) &
        (df_copy["age_max"] >= age_target)
    )

    if request.outdoor_preferred:
        pref_mask = df_copy["indoor_outdoor"].str.upper().eq("OUTDOOR")
    else:
        pref_mask = df_copy["indoor_outdoor"].str.upper().eq("INDOOR")

    # 1. strict
    strict = df_copy[city_mask & season_mask & budget_mask & age_mask & pref_mask]

    # 2. relax city
    if strict.empty:
        strict = df_copy[season_mask & budget_mask & age_mask & pref_mask]

    # 3. relax preference
    if strict.empty:
        strict = df_copy[season_mask & budget_mask & age_mask]

    # 4. relax season
    if strict.empty:
        strict = df_copy[budget_mask & age_mask]

    # 5. last fallback
    if strict.empty:
        strict = df_copy.copy()

    if strict.empty:
        raise HTTPException(status_code=404, detail="No events found")

    strict["combined_text"] = (
        strict["title"].fillna("").astype(str) + " " +
        strict["short_description"].fillna("").astype(str) + " " +
        strict["tags"].fillna("").astype(str)
    )

    X = strict[[
        "event_type",
        "venue_type",
        "indoor_outdoor",
        "season",
        "budget_band",
        "city",
        "price_dt",
        "age_min",
        "age_max",
        "duration_minutes",
        "combined_text"
    ]]

    if X.empty:
        raise HTTPException(status_code=404, detail="No candidate rows after filtering")

    preds = pipeline.predict(X)
    probas = pipeline.predict_proba(X)

    classes = list(le.classes_)
    best_idx = classes.index("HIGHLY_RECOMMENDED") if "HIGHLY_RECOMMENDED" in classes else 0

    strict = strict.reset_index(drop=True)
    results = []

    for i, row in strict.iterrows():
        label = le.inverse_transform([preds[i]])[0]
        score = float(probas[i][best_idx])

        results.append({
            "title": row["title"],
            "description": row["short_description"],
            "event_type": row["event_type"],
            "suggested_location": row["address"],
            "location_name": row.get("location_name"),
            "city": row.get("city"),
            "country": row.get("country", "Tunisie"),
            "latitude": safe_float(row.get("latitude")),
            "longitude": safe_float(row.get("longitude")),
            "indoor_outdoor": row["indoor_outdoor"],
            "requires_authorization": str(row.get("requires_authorization", "false")).lower() == "true",
            "suggested_price": float(row["price_dt"]),
            "predicted_label": label,
            "relevance_score": round(score, 4),
            "venue_type": row["venue_type"],
            "season": row["season"],
            "tags": row["tags"],
            "match_reason": "activité compatible avec le contexte"
        })

    results.sort(key=lambda x: x["relevance_score"], reverse=True)
    return results[:request.top_n]