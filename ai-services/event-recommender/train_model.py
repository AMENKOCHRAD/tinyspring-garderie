import os
import pickle
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.impute import SimpleImputer
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder, OneHotEncoder, StandardScaler
from sklearn.linear_model import LogisticRegression

DATA_PATH = os.getenv("EVENT_DATASET_PATH", "data/event_templates_tunisia_enriched.csv")
MODEL_DIR = os.getenv("MODEL_DIR", "models")
MODEL_PATH = os.path.join(MODEL_DIR, "pipeline.pkl")
LABEL_ENCODER_PATH = os.path.join(MODEL_DIR, "label_encoder.pkl")


def clean_text(value):
    if pd.isna(value):
        return ""
    return str(value).strip()


def normalize_boolean(value):
    if pd.isna(value):
        return 0
    text = str(value).strip().lower()
    return 1 if text in {"true", "1", "yes", "oui"} else 0


def encode_educational_value(value):
    mapping = {
        "LOW": 0,
        "MEDIUM": 1,
        "HIGH": 2,
        "VERY_HIGH": 3
    }
    if pd.isna(value):
        return 1
    return mapping.get(str(value).strip().upper(), 1)


def main():
    print(f"Loading dataset from: {DATA_PATH}")
    df = pd.read_csv(DATA_PATH)

    required_columns = [
        "title",
        "description",
        "type",
        "location",
        "city",
        "venue_type",
        "indoor_outdoor",
        "season",
        "age_min",
        "age_max",
        "duration_minutes",
        "price_dt",
        "budget_level",
        "requires_authorization",
        "tags",
        "educational_value",
        "recommended_label",
    ]

    missing = [col for col in required_columns if col not in df.columns]
    if missing:
        raise ValueError(f"Missing columns in dataset: {missing}")

    df = df.dropna(subset=["recommended_label"]).copy()

    text_columns = ["title", "description", "location", "city", "venue_type", "tags", "season", "budget_level"]
    for col in text_columns:
        df[col] = df[col].apply(clean_text)

    df["requires_authorization_encoded"] = df["requires_authorization"].apply(normalize_boolean)
    df["educational_value_encoded"] = df["educational_value"].apply(encode_educational_value)
    df["price_dt"] = pd.to_numeric(df["price_dt"], errors="coerce").fillna(0.0)
    df["age_min"] = pd.to_numeric(df["age_min"], errors="coerce").fillna(2)
    df["age_max"] = pd.to_numeric(df["age_max"], errors="coerce").fillna(6)
    df["duration_minutes"] = pd.to_numeric(df["duration_minutes"], errors="coerce").fillna(60)

    df["combined_text"] = (
        df["title"] + " " +
        df["description"] + " " +
        df["location"] + " " +
        df["city"] + " " +
        df["venue_type"] + " " +
        df["tags"].str.replace(";", " ", regex=False)
    )

    categorical_features = [
        "type",
        "venue_type",
        "indoor_outdoor",
        "season",
        "budget_level",
        "city",
    ]
    numeric_features = [
        "price_dt",
        "age_min",
        "age_max",
        "duration_minutes",
        "educational_value_encoded",
        "requires_authorization_encoded",
    ]
    text_feature = "combined_text"

    X = df[categorical_features + numeric_features + [text_feature]]
    y = df["recommended_label"]

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)

    preprocessor = ColumnTransformer(
        transformers=[
            ("cat", Pipeline(steps=[
                ("imputer", SimpleImputer(strategy="most_frequent")),
                ("onehot", OneHotEncoder(handle_unknown="ignore"))
            ]), categorical_features),
            ("num", Pipeline(steps=[
                ("imputer", SimpleImputer(strategy="median")),
                ("scale", StandardScaler())
            ]), numeric_features),
            ("txt", TfidfVectorizer(max_features=1200, ngram_range=(1, 2)), text_feature),
        ]
    )

    model = LogisticRegression(
        max_iter=3000,
        class_weight="balanced"
    )

    pipeline = Pipeline(steps=[
        ("preprocessor", preprocessor),
        ("classifier", model)
    ])

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y_encoded,
        test_size=0.2,
        random_state=42,
        stratify=y_encoded
    )

    print("Training model...")
    pipeline.fit(X_train, y_train)

    print("Evaluating model...")
    y_pred = pipeline.predict(X_test)

    print("Accuracy:", round(accuracy_score(y_test, y_pred), 4))
    labels = sorted(set(y_test) | set(y_pred))
    target_names = label_encoder.inverse_transform(labels)
    print("=== Classification Report ===")
    print(classification_report(y_test, y_pred, labels=labels, target_names=target_names))

    os.makedirs(MODEL_DIR, exist_ok=True)
    with open(MODEL_PATH, "wb") as f:
        pickle.dump(pipeline, f)
    with open(LABEL_ENCODER_PATH, "wb") as f:
        pickle.dump(label_encoder, f)

    print(f"Model saved to {MODEL_PATH}")
    print(f"Label encoder saved to {LABEL_ENCODER_PATH}")


if __name__ == "__main__":
    main()
