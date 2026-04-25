import os
import pickle
import warnings

import numpy as np
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer, CountVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder, OneHotEncoder, StandardScaler
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold, GroupKFold
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score

warnings.filterwarnings("ignore")

DATA_PATH = "data/tunisia_daycare_events_140.csv"
MODEL_DIR = "model"


def prepare_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()

    required_text_cols = [
        "tags", "title", "short_description", "educational_value",
        "event_type", "venue_type", "indoor_outdoor", "season",
        "budget_band", "city"
    ]

    for col in required_text_cols:
        if col not in df.columns:
            df[col] = ""
        df[col] = df[col].fillna("").astype(str)

    for col, default in {
        "price_dt": 0,
        "age_min": 2,
        "age_max": 6,
        "duration_minutes": 60,
    }.items():
        if col not in df.columns:
            df[col] = default
        df[col] = pd.to_numeric(df[col], errors="coerce").fillna(default)

    edu_map = {"LOW": 0, "MEDIUM": 1, "HIGH": 2, "VERY_HIGH": 3}

    df["edu_score"] = df["educational_value"].str.upper().map(edu_map).fillna(1)
    df["age_range_width"] = df["age_max"] - df["age_min"]
    df["is_free"] = (df["price_dt"] == 0).astype(int)
    df["value_score"] = df["edu_score"] / (df["price_dt"] + 1)
    df["combined_text"] = df["title"] + " " + df["short_description"]
    df["tags_clean"] = df["tags"].str.replace(";", " ", regex=False)

    df["is_fete"] = (df["event_type"].str.upper() == "FETE").astype(int)
    df["is_sortie"] = (df["event_type"].str.upper() == "SORTIE").astype(int)

    df["price_tier"] = pd.cut(
        df["price_dt"],
        bins=[-1, 0, 8, 12, 20, 100000],
        labels=[0, 1, 2, 3, 4]
    ).astype(float)

    df["budget_is_medium"] = (df["budget_band"].str.upper() == "MEDIUM").astype(int)
    df["price_x_edu"] = df["price_dt"] * df["edu_score"]

    return df


def main():
    df = pd.read_csv(DATA_PATH)

    if "recommended_label" not in df.columns:
        raise ValueError("La colonne recommended_label est obligatoire dans le CSV.")

    df = prepare_features(df)

    print(f"Dataset chargé : {len(df)} lignes, {df['title'].nunique()} titres uniques")

    categorical_features = [
        "event_type", "venue_type", "indoor_outdoor",
        "season", "budget_band", "city",
    ]

    numeric_features = [
        "price_dt", "age_min", "age_max", "duration_minutes",
        "age_range_width", "is_free", "edu_score", "value_score",
        "is_fete", "is_sortie", "price_tier",
        "budget_is_medium", "price_x_edu",
    ]

    text_feature = "combined_text"
    tags_feature = "tags_clean"

    feature_cols = categorical_features + numeric_features + [text_feature, tags_feature]

    preprocessor = ColumnTransformer(
        transformers=[
            (
                "cat",
                OneHotEncoder(handle_unknown="ignore", sparse_output=False),
                categorical_features,
            ),
            (
                "num",
                StandardScaler(),
                numeric_features,
            ),
            (
                "txt",
                TfidfVectorizer(
                    max_features=200,
                    ngram_range=(1, 2),
                    sublinear_tf=True,
                ),
                text_feature,
            ),
            (
                "tags",
                CountVectorizer(
                    max_features=80,
                    token_pattern=r"[^\s]+",
                ),
                tags_feature,
            ),
        ],
        remainder="drop",
    )

    le = LabelEncoder()
    y = le.fit_transform(df["recommended_label"])
    X = df[feature_cols]

    print(f"Classes : {list(le.classes_)}")
    print(f"Distribution : {dict(zip(*np.unique(y, return_counts=True)))}")

    classifier = LogisticRegression(
        C=0.5,
        class_weight="balanced",
        max_iter=1000,
        random_state=42,
        solver="lbfgs",
    )

    pipeline = Pipeline([
        ("preprocessor", preprocessor),
        ("classifier", classifier),
    ])

    print("\nGroupKFold anti-leakage")
    groups = df["title"]
    gkf = GroupKFold(n_splits=5)

    gkf_f1 = cross_val_score(
        pipeline, X, y, cv=gkf, scoring="f1_weighted", groups=groups, n_jobs=-1
    )

    print(f"F1-weighted réel : {gkf_f1.mean():.3f} ± {gkf_f1.std():.3f}")

    if len(np.unique(y)) == 2:
        gkf_auc = cross_val_score(
            pipeline, X, y, cv=gkf, scoring="roc_auc", groups=groups, n_jobs=-1
        )
        print(f"ROC-AUC réel : {gkf_auc.mean():.3f} ± {gkf_auc.std():.3f}")

    print("\nStratifiedKFold comparatif seulement")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    skf_f1 = cross_val_score(pipeline, X, y, cv=skf, scoring="f1_weighted", n_jobs=-1)
    print(f"F1-weighted optimiste : {skf_f1.mean():.3f} ± {skf_f1.std():.3f}")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print("\nEntraînement final...")
    pipeline.fit(X_train, y_train)

    y_pred = pipeline.predict(X_test)

    print("\nRapport de classification :")
    print(classification_report(y_test, y_pred, target_names=le.classes_))

    if len(np.unique(y)) == 2:
        y_proba = pipeline.predict_proba(X_test)[:, 1]
        print("ROC-AUC test :", round(roc_auc_score(y_test, y_proba), 4))

    print("\nMatrice de confusion :")
    cm = confusion_matrix(y_test, y_pred)
    print(pd.DataFrame(cm, index=le.classes_, columns=le.classes_))

    try:
        feature_names = pipeline.named_steps["preprocessor"].get_feature_names_out()
        coefs = pipeline.named_steps["classifier"].coef_[0]
        top_idx = np.argsort(np.abs(coefs))[-15:][::-1]

        print("\nTop 15 features :")
        for i in top_idx:
            print(f"{feature_names[i]:50s} coef={coefs[i]:+.4f}")
    except Exception:
        pass

    os.makedirs(MODEL_DIR, exist_ok=True)

    with open(f"{MODEL_DIR}/pipeline.pkl", "wb") as file:
        pickle.dump(pipeline, file)

    with open(f"{MODEL_DIR}/label_encoder.pkl", "wb") as file:
        pickle.dump(le, file)

    with open(f"{MODEL_DIR}/feature_cols.pkl", "wb") as file:
        pickle.dump(feature_cols, file)

    print(f"\nModèle sauvegardé dans {MODEL_DIR}/")
    print(f"Classes : {list(le.classes_)}")
    print(f"Features : {len(feature_cols)}")
    print(f"Modèle : {type(classifier).__name__}")
    print(f"F1 réel GroupKFold : {gkf_f1.mean():.3f}")


if __name__ == "__main__":
    main()