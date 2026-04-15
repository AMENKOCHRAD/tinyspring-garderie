import os
import pandas as pd
import joblib

from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, LabelEncoder, StandardScaler
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score

DATA_PATH = "data/event_training_dataset.csv"
MODEL_PATH = "model/event_recommender_pipeline.pkl"
LABEL_ENCODER_PATH = "model/label_encoder.pkl"


def clean_text(value):
    if pd.isna(value):
        return ""
    return str(value).strip()


def main():
    print("Loading dataset...")
    df = pd.read_csv(DATA_PATH)

    required_columns = [
        "season",
        "month",
        "age_group",
        "budget_level",
        "outdoor_preferred",
        "city_context",
        "event_type",
        "indoor_outdoor",
        "title",
        "description",
        "suggested_location",
        "requires_authorization",
        "price",
        "relevance_label",
    ]

    missing = [col for col in required_columns if col not in df.columns]
    if missing:
        raise ValueError(f"Missing columns in dataset: {missing}")

    df = df.dropna(subset=["relevance_label"]).copy()

    text_columns = ["season", "month", "age_group", "budget_level", "city_context", "event_type",
                    "indoor_outdoor", "title", "description", "suggested_location"]
    for col in text_columns:
        df[col] = df[col].apply(clean_text)

    df["outdoor_preferred"] = df["outdoor_preferred"].astype(str).str.lower()
    df["requires_authorization"] = df["requires_authorization"].astype(str).str.lower()
    df["price"] = pd.to_numeric(df["price"], errors="coerce").fillna(0.0)

    df["text"] = (
        df["title"] + " " +
        df["description"] + " " +
        df["suggested_location"] + " " +
        df["city_context"] + " " +
        df["event_type"]
    )

    categorical_features = [
        "season",
        "month",
        "age_group",
        "budget_level",
        "outdoor_preferred",
        "city_context",
        "event_type",
        "indoor_outdoor",
        "requires_authorization",
    ]
    numeric_features = ["price"]
    text_feature = "text"

    X = df[categorical_features + numeric_features + [text_feature]]
    y = df["relevance_label"]

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
                ("scale", StandardScaler(with_mean=False))
            ]), numeric_features),
            ("txt", TfidfVectorizer(max_features=1200, ngram_range=(1, 2)), text_feature),
        ]
    )

    model = LogisticRegression(
        max_iter=2000,
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

    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    joblib.dump(label_encoder, LABEL_ENCODER_PATH)

    print(f"Model saved to {MODEL_PATH}")
    print(f"Label encoder saved to {LABEL_ENCODER_PATH}")


if __name__ == "__main__":
    main()
