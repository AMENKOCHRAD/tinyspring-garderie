# Détection de doublons d'observations (ML)

Objectif : détecter si une nouvelle observation (titre + description) est un **doublon** d'une observation déjà saisie **le même jour** pour **le même enfant**.

Ce module utilise une approche ML simple :
- vectorisation TF‑IDF du texte (`titre + description`)
- features supplémentaires (même type, écart de température, écart de temps…)
- modèle de classification sur des **paires** (duplicate / non‑duplicate)

## Contenu
- `notebook.ipynb` : notebook Jupyter (dataset exemple + entraînement + évaluation)
- `train.py` : entraîne et exporte les artefacts
- `infer.py` : exemple d'inférence (score + décision)

## Exécution (local)
Depuis `C:\Users\a\tinyspring-garderie` :

```bash
python -m pip install -r ml/duplicate_observation_detection/requirements.txt
python ml/duplicate_observation_detection/train.py
python ml/duplicate_observation_detection/infer.py
```

Artefacts générés :
- `ml/duplicate_observation_detection/artifacts/tfidf.pkl`
- `ml/duplicate_observation_detection/artifacts/dup_model.pkl`
- `ml/duplicate_observation_detection/artifacts/metadata.json`

