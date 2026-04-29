from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import joblib
import numpy as np


ARTIFACT_DIR = Path(__file__).parent / "artifacts"


def normalize_text(s: str) -> str:
    s = (s or "").strip().lower()
    return " ".join(s.replace("\n", " ").split())


@dataclass(frozen=True)
class Candidate:
    type: str
    titre: str
    description: str
    cree_le_iso: str
    temperature: float | None = None

    @property
    def text(self) -> str:
        return normalize_text(f"{self.titre} {self.description}")


def iso_to_dt(iso: str) -> datetime:
    return datetime.fromisoformat(iso)


def cosine_sim_sparse(a, b) -> np.ndarray:
    num = a.multiply(b).sum(axis=1).A.ravel()
    den = np.linalg.norm(a.toarray(), axis=1) * np.linalg.norm(b.toarray(), axis=1)
    den = np.where(den == 0, 1e-9, den)
    return num / den


def predict_duplicate(new_obs: Candidate, existing: Candidate) -> tuple[bool, float]:
    tfidf = joblib.load(ARTIFACT_DIR / "tfidf.pkl")
    model = joblib.load(ARTIFACT_DIR / "dup_model.pkl")

    a_vec = tfidf.transform([new_obs.text])
    b_vec = tfidf.transform([existing.text])
    sim = cosine_sim_sparse(a_vec, b_vec)[0]

    minutes_diff = abs(int((iso_to_dt(new_obs.cree_le_iso) - iso_to_dt(existing.cree_le_iso)).total_seconds() // 60))
    temp_diff = (
        abs((new_obs.temperature or 0.0) - (existing.temperature or 0.0))
        if new_obs.temperature is not None and existing.temperature is not None
        else 999.0
    )
    same_type = int(new_obs.type == existing.type)

    X = np.array([[sim, same_type, minutes_diff, temp_diff]])
    proba = float(model.predict_proba(X)[0, 1])
    is_dup = proba >= 0.75
    return is_dup, proba


def main() -> None:
    new_obs = Candidate(
        type="SANTE",
        titre="Température 38.7",
        description="Fatigue et frissons, enfant chaud.",
        cree_le_iso="2026-04-28T11:30:00",
        temperature=38.7,
    )
    existing = Candidate(
        type="SANTE",
        titre="Fièvre 38.5",
        description="Enfant chaud, fatigue.",
        cree_le_iso="2026-04-28T10:00:00",
        temperature=38.5,
    )

    dup, score = predict_duplicate(new_obs, existing)
    print({"duplicate": dup, "score": round(score, 3)})


if __name__ == "__main__":
    main()

