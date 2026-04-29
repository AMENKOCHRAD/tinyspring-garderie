from __future__ import annotations

import json
import os
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split


ARTIFACT_DIR = Path(__file__).parent / "artifacts"
ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)


def normalize_text(s: str) -> str:
    s = (s or "").strip().lower()
    return " ".join(s.replace("\n", " ").split())


@dataclass(frozen=True)
class Observation:
    enfant_id: int
    type: str
    titre: str
    description: str
    cree_le_iso: str  # ISO string
    temperature: float | None = None

    @property
    def text(self) -> str:
        return normalize_text(f"{self.titre} {self.description}")


def build_example_observations() -> list[Observation]:
    # Dataset d'exemple (à remplacer ensuite par un export DB si tu veux).
    return [
        Observation(1, "SANTE", "Fièvre 38.5", "Enfant chaud, fatigue.", "2026-04-28T10:00:00", 38.5),
        Observation(1, "SANTE", "Température 38.7", "Fatigue + frissons.", "2026-04-28T11:30:00", 38.7),
        Observation(1, "SANTE", "Chute légère", "Petit choc au genou, pas de saignement.", "2026-04-28T15:10:00", None),
        Observation(1, "COMPORTEMENT", "Agitation", "Pleurs fréquents, veut rester seul.", "2026-04-28T09:20:00", None),
        Observation(2, "SANTE", "Toux", "Toux sèche, pas de fièvre.", "2026-04-28T08:30:00", None),
        Observation(2, "SOMMEIL", "Sommeil", "Sieste courte, réveils.", "2026-04-28T13:00:00", None),
        Observation(1, "SANTE", "Fièvre", "Température 38.6, hydratation OK.", "2026-04-27T10:00:00", 38.6),
        Observation(1, "SANTE", "Fièvre 38.6", "Fièvre, fatigue.", "2026-04-27T10:10:00", 38.6),
    ]


def iso_to_dt(iso: str) -> datetime:
    return datetime.fromisoformat(iso)


def make_pairs(observations: list[Observation]) -> pd.DataFrame:
    # Paires sur même enfant + même jour
    rows = []
    for i, a in enumerate(observations):
        for j, b in enumerate(observations):
            if j <= i:
                continue
            if a.enfant_id != b.enfant_id:
                continue
            day_a = iso_to_dt(a.cree_le_iso).date()
            day_b = iso_to_dt(b.cree_le_iso).date()
            if day_a != day_b:
                continue

            # Label simple pour l'exemple :
            # - même type et textes proches et température proche -> doublon
            # (dans ton projet, tu pourras labelliser depuis des validations admin ou règles)
            temp_diff = (
                abs((a.temperature or 0.0) - (b.temperature or 0.0))
                if a.temperature is not None and b.temperature is not None
                else None
            )
            minutes_diff = abs(int((iso_to_dt(a.cree_le_iso) - iso_to_dt(b.cree_le_iso)).total_seconds() // 60))

            is_dup = 0
            if a.type == b.type and minutes_diff <= 180:
                # heuristique de label pour bootstrap
                if ("fièvre" in a.text and "fièvre" in b.text) or ("temp" in a.text and "temp" in b.text):
                    if temp_diff is None or temp_diff <= 0.4:
                        is_dup = 1

            rows.append(
                {
                    "a_text": a.text,
                    "b_text": b.text,
                    "same_type": int(a.type == b.type),
                    "minutes_diff": minutes_diff,
                    "temp_diff": float(temp_diff) if temp_diff is not None else 999.0,
                    "label_dup": is_dup,
                }
            )

    return pd.DataFrame(rows)


def cosine_sim_sparse(a, b) -> np.ndarray:
    # a, b: sparse matrices with same shape
    # cosine = (a·b) / (||a|| ||b||)
    num = a.multiply(b).sum(axis=1).A.ravel()
    den = np.linalg.norm(a.toarray(), axis=1) * np.linalg.norm(b.toarray(), axis=1)
    den = np.where(den == 0, 1e-9, den)
    return num / den


def main() -> None:
    obs = build_example_observations()
    pairs = make_pairs(obs)

    if pairs.empty or pairs["label_dup"].nunique() < 2:
        raise RuntimeError(
            "Dataset d'exemple insuffisant (besoin de doublons et non-doublons). "
            "Ajoute plus d'observations dans build_example_observations()."
        )

    # TF‑IDF sur tous les textes des paires
    all_texts = pd.concat([pairs["a_text"], pairs["b_text"]], ignore_index=True)
    tfidf = TfidfVectorizer(ngram_range=(1, 2), min_df=1)
    tfidf.fit(all_texts)

    a_vec = tfidf.transform(pairs["a_text"])
    b_vec = tfidf.transform(pairs["b_text"])
    sim = cosine_sim_sparse(a_vec, b_vec)

    X = np.column_stack(
        [
            sim,
            pairs["same_type"].to_numpy(),
            pairs["minutes_diff"].to_numpy(),
            pairs["temp_diff"].to_numpy(),
        ]
    )
    y = pairs["label_dup"].to_numpy()

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.3, random_state=42, stratify=y
    )

    model = LogisticRegression(max_iter=1000)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    print(classification_report(y_test, y_pred, digits=3))

    joblib.dump(tfidf, ARTIFACT_DIR / "tfidf.pkl")
    joblib.dump(model, ARTIFACT_DIR / "dup_model.pkl")

    metadata = {
        "trained_at": datetime.now().isoformat(timespec="seconds"),
        "features": ["cosine_sim_tfidf", "same_type", "minutes_diff", "temp_diff"],
        "model": "LogisticRegression",
        "notes": "Bootstrap dataset d'exemple; à améliorer avec données réelles.",
    }
    (ARTIFACT_DIR / "metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print(f"OK: artefacts dans {ARTIFACT_DIR}")


if __name__ == "__main__":
    main()

