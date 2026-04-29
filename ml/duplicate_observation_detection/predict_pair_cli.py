from __future__ import annotations

import json
import sys
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
    type: str | None
    titre: str | None
    description: str | None
    creeLeIso: str | None
    temperature: float | None = None

    @property
    def text(self) -> str:
        return normalize_text(f"{self.titre or ''} {self.description or ''}")


def iso_to_dt(iso: str | None) -> datetime:
    if not iso:
        return datetime.now()
    return datetime.fromisoformat(iso)


def cosine_sim_sparse(a, b) -> np.ndarray:
    num = a.multiply(b).sum(axis=1).A.ravel()
    den = np.linalg.norm(a.toarray(), axis=1) * np.linalg.norm(b.toarray(), axis=1)
    den = np.where(den == 0, 1e-9, den)
    return num / den


def predict(new_obs: Candidate, existing: Candidate, threshold: float = 0.75) -> dict:
    tfidf = joblib.load(ARTIFACT_DIR / "tfidf.pkl")
    model = joblib.load(ARTIFACT_DIR / "dup_model.pkl")

    a_vec = tfidf.transform([new_obs.text])
    b_vec = tfidf.transform([existing.text])
    sim = float(cosine_sim_sparse(a_vec, b_vec)[0])

    minutes_diff = abs(int((iso_to_dt(new_obs.creeLeIso) - iso_to_dt(existing.creeLeIso)).total_seconds() // 60))
    temp_diff = (
        abs((new_obs.temperature or 0.0) - (existing.temperature or 0.0))
        if new_obs.temperature is not None and existing.temperature is not None
        else 999.0
    )
    same_type = int((new_obs.type or "") == (existing.type or ""))

    X = np.array([[sim, same_type, minutes_diff, temp_diff]], dtype=float)
    proba = float(model.predict_proba(X)[0, 1])
    return {"duplicate": proba >= threshold, "score": round(proba, 6)}


def main() -> None:
    raw = sys.stdin.read()
    if not raw:
        print(json.dumps({"duplicate": False, "score": 0.0}))
        return

    payload = json.loads(raw)
    new_obs = Candidate(**(payload.get("new") or {}))
    existing = Candidate(**(payload.get("existing") or {}))

    res = predict(new_obs, existing, threshold=0.75)
    sys.stdout.write(json.dumps(res))


if __name__ == "__main__":
    main()

