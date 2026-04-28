import pickle
from typing import Optional

import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()

DATA_PATH = "data/tunisia_daycare_events_140.csv"

with open("model/pipeline.pkl", "rb") as file:
    pipeline = pickle.load(file)

with open("model/label_encoder.pkl", "rb") as file:
    le = pickle.load(file)

with open("model/feature_cols.pkl", "rb") as file:
    feature_cols = pickle.load(file)

df = pd.read_csv(DATA_PATH)


class RecommendationRequest(BaseModel):
    season: Optional[str] = None
    month: Optional[str] = None
    age_group: str = "2-6 ans"
    budget_level: str = "MEDIUM"
    outdoor_preferred: Optional[bool] = None
    city_context: str = ""
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

    if len(digits) >= 2:
        return round((digits[0] + digits[1]) / 2)

    return digits[0] if digits else 4


def budget_limit(budget_level: str) -> float:
    level = normalize_text(budget_level)

    if level in {"low", "faible", "bas"}:
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


def prepare_features(dataframe: pd.DataFrame) -> pd.DataFrame:
    df_copy = dataframe.copy()

    text_cols = [
        "city", "city_aliases", "indoor_outdoor", "season",
        "event_type", "venue_type", "budget_band", "educational_value",
        "title", "short_description", "tags", "address",
        "location_name", "country"
    ]

    for col in text_cols:
        if col not in df_copy.columns:
            df_copy[col] = ""
        df_copy[col] = df_copy[col].fillna("").astype(str)

    numeric_defaults = {
        "price_dt": 0,
        "age_min": 2,
        "age_max": 6,
        "duration_minutes": 60,
    }

    for col, default in numeric_defaults.items():
        if col not in df_copy.columns:
            df_copy[col] = default
        df_copy[col] = pd.to_numeric(df_copy[col], errors="coerce").fillna(default)

    edu_map = {"LOW": 0, "MEDIUM": 1, "HIGH": 2, "VERY_HIGH": 3}

    df_copy["edu_score"] = df_copy["educational_value"].str.upper().map(edu_map).fillna(1)
    df_copy["age_range_width"] = df_copy["age_max"] - df_copy["age_min"]
    df_copy["is_free"] = (df_copy["price_dt"] == 0).astype(int)
    df_copy["value_score"] = df_copy["edu_score"] / (df_copy["price_dt"] + 1)
    df_copy["combined_text"] = df_copy["title"] + " " + df_copy["short_description"]
    df_copy["tags_clean"] = df_copy["tags"].str.replace(";", " ", regex=False)

    df_copy["is_fete"] = (df_copy["event_type"].str.upper() == "FETE").astype(int)
    df_copy["is_sortie"] = (df_copy["event_type"].str.upper() == "SORTIE").astype(int)

    df_copy["price_tier"] = pd.cut(
        df_copy["price_dt"],
        bins=[-1, 0, 8, 12, 20, 100000],
        labels=[0, 1, 2, 3, 4]
    ).astype(float)

    df_copy["budget_is_medium"] = (df_copy["budget_band"].str.upper() == "MEDIUM").astype(int)
    df_copy["price_x_edu"] = df_copy["price_dt"] * df_copy["edu_score"]

    return df_copy


def compute_context_bonus(row, request: RecommendationRequest) -> float:
    bonus = 0.0

    requested_city = normalize_text(request.city_context)
    requested_season = normalize_text(request.season)
    budget = budget_limit(request.budget_level)
    age = extract_age_target(request.age_group)

    if requested_city:
        city = normalize_text(row.get("city"))
        aliases = normalize_text(row.get("city_aliases"))
        if requested_city in city or requested_city in aliases:
            bonus += 0.12

    if requested_season:
        season = normalize_text(row.get("season"))
        if season in {requested_season, "all"}:
            bonus += 0.10

    try:
        if float(row.get("price_dt", 0)) <= budget:
            bonus += 0.08
    except Exception:
        pass

    try:
        if float(row.get("age_min", 2)) <= age <= float(row.get("age_max", 6)):
            bonus += 0.08
    except Exception:
        pass

    if request.outdoor_preferred is not None:
        expected = "OUTDOOR" if request.outdoor_preferred else "INDOOR"
        if str(row.get("indoor_outdoor", "")).upper() == expected:
            bonus += 0.06

    return bonus


@app.get("/health")
def health():
    return {
        "status": "ok",
        "rows": int(len(df)),
        "features": int(len(feature_cols)),
        "classes": [str(c) for c in le.classes_]
    }

@app.post("/recommend-events")
def recommend(request: RecommendationRequest):
    df_copy = prepare_features(df)

    requested_city = normalize_text(request.city_context)
    requested_season = normalize_text(request.season)
    age_target = extract_age_target(request.age_group)
    max_budget = budget_limit(request.budget_level)

    if requested_city:
        city_mask = (
            df_copy["city"].str.lower().str.contains(requested_city, na=False)
            | df_copy["city_aliases"].str.lower().str.contains(requested_city, na=False)
        )
    else:
        city_mask = pd.Series([True] * len(df_copy), index=df_copy.index)

    if requested_season:
        season_mask = df_copy["season"].str.lower().isin([requested_season, "all"])
    else:
        season_mask = pd.Series([True] * len(df_copy), index=df_copy.index)

    budget_mask = df_copy["price_dt"] <= max_budget
    age_mask = (df_copy["age_min"] <= age_target) & (df_copy["age_max"] >= age_target)

    if request.outdoor_preferred is None:
        pref_mask = pd.Series([True] * len(df_copy), index=df_copy.index)
    else:
        expected_place = "OUTDOOR" if request.outdoor_preferred else "INDOOR"
        pref_mask = df_copy["indoor_outdoor"].str.upper().eq(expected_place)

    fallback_masks = [
        city_mask & season_mask & budget_mask & age_mask & pref_mask,
        season_mask & budget_mask & age_mask & pref_mask,
        season_mask & budget_mask & age_mask,
        budget_mask & age_mask,
        age_mask,
        pd.Series([True] * len(df_copy), index=df_copy.index),
    ]

    subset = pd.DataFrame()

    for mask in fallback_masks:
        subset = df_copy[mask]
        if not subset.empty:
            break

    if subset.empty:
        raise HTTPException(status_code=404, detail="No events found")

    X = subset[feature_cols]

    preds = pipeline.predict(X)
    probas = pipeline.predict_proba(X)

    classes = list(le.classes_)
    highly_index = classes.index("HIGHLY_RECOMMENDED") if "HIGHLY_RECOMMENDED" in classes else 0

    subset = subset.reset_index(drop=True)
    results = []

    for i, row in subset.iterrows():
        predicted_label = le.inverse_transform([preds[i]])[0]
        model_score = float(probas[i][highly_index])
        bonus = compute_context_bonus(row, request)
        final_score = min(model_score + bonus, 1.0)

        results.append({
            "title": row.get("title"),
            "description": row.get("short_description"),
            "event_type": row.get("event_type"),
            "suggested_location": row.get("address"),
            "location_name": row.get("location_name"),
            "city": row.get("city"),
            "country": row.get("country", "Tunisie"),
            "latitude": safe_float(row.get("latitude")),
            "longitude": safe_float(row.get("longitude")),
            "indoor_outdoor": row.get("indoor_outdoor"),
            "requires_authorization": str(row.get("requires_authorization", "false")).lower() == "true",
            "suggested_price": float(row.get("price_dt", 0)),
            "educational_value": row.get("educational_value"),
            "predicted_label": predicted_label,
            "relevance_score": round(final_score, 4),
            "model_score": round(model_score, 4),
            "context_bonus": round(bonus, 4),
            "venue_type": row.get("venue_type"),
            "season": row.get("season"),
            "tags": row.get("tags"),
            "match_reason": build_match_reason(row, request, predicted_label, bonus)
        })

    results.sort(key=lambda item: item["relevance_score"], reverse=True)
    return results[: request.top_n]


def build_match_reason(row, request: RecommendationRequest, predicted_label: str, bonus: float) -> str:
    reasons = []

    if predicted_label == "HIGHLY_RECOMMENDED":
        reasons.append("fort potentiel de recommandation")

    requested_city = normalize_text(request.city_context)
    if requested_city and requested_city in normalize_text(row.get("city")):
        reasons.append("ville compatible")

    requested_season = normalize_text(request.season)
    if requested_season and normalize_text(row.get("season")) in {requested_season, "all"}:
        reasons.append("saison adaptée")

    age = extract_age_target(request.age_group)
    try:
        if float(row.get("age_min", 2)) <= age <= float(row.get("age_max", 6)):
            reasons.append("âge ciblé respecté")
    except Exception:
        pass

    try:
        if float(row.get("price_dt", 0)) <= budget_limit(request.budget_level):
            reasons.append("budget respecté")
    except Exception:
        pass

    if bonus > 0:
        reasons.append("bonus contexte appliqué")

    return ", ".join(reasons) if reasons else "activité compatible avec le contexte"