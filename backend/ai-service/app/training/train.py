from pathlib import Path

import joblib
import pandas as pd
from sklearn.ensemble import IsolationForest, RandomForestRegressor


def train_models() -> None:
    base_dir = Path(__file__).resolve().parents[2]
    data_dir = base_dir / "data"
    models_dir = base_dir / "models"
    models_dir.mkdir(parents=True, exist_ok=True)

    demand_df = pd.read_csv(data_dir / "transport_demand_history.csv")
    anomaly_df = pd.read_csv(data_dir / "transport_requests_history.csv")

    demand_features = [
        "day_of_week",
        "month",
        "hour",
        "active_children_count",
        "available_transport_count",
        "avg_route_distance_km",
        "rain_flag",
        "school_break_flag",
    ]
    demand_target = "demand_count"

    demand_model = RandomForestRegressor(
        n_estimators=250,
        random_state=42,
        max_depth=10,
        min_samples_split=4,
        min_samples_leaf=2,
    )
    demand_model.fit(demand_df[demand_features], demand_df[demand_target])

    anomaly_features = [
        "requested_hour",
        "route_distance_km",
        "seats_requested",
        "same_address_flag",
        "is_round_trip",
        "week_day",
    ]

    anomaly_model = IsolationForest(
        n_estimators=200,
        contamination=0.12,
        random_state=42,
    )
    anomaly_model.fit(anomaly_df[anomaly_features])

    joblib.dump(demand_model, models_dir / "demand_model.joblib")
    joblib.dump(anomaly_model, models_dir / "anomaly_model.joblib")

    print("Demand model saved to:", models_dir / "demand_model.joblib")
    print("Anomaly model saved to:", models_dir / "anomaly_model.joblib")


if __name__ == "__main__":
    train_models()
