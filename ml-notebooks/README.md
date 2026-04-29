# Notebooks ML (TinySpring)

## Traitement – validation intelligente

- Notebook : `ml-notebooks/traitement_validation.ipynb`
- Objectif : entraîner un `DecisionTreeClassifier` (scikit-learn) et **exporter** le modèle en JSON pour le backend.
- Fichier exporté (utilisé par Spring Boot) :
  - `backend/garderie/models/traitement_validation_tree.json`

## Pré-requis

- Python 3.10+ recommandé
- Packages :
  - `pandas`
  - `numpy`
  - `scikit-learn`

## Utilisation

1) Ouvrir `ml-notebooks/traitement_validation.ipynb`
2) Exécuter toutes les cellules
3) Vérifier que `backend/garderie/models/traitement_validation_tree.json` est généré

