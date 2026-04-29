import pandas as pd
import numpy as np
import os
from sklearn.cluster import KMeans
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score

# Chemin vers le dossier contenant les datasets
BASE_DIR = os.path.dirname(os.path.abspath(__file__))


class ChildClusterer:
    """
    Modèle 1 : Apprentissage Non-Supervisé (K-Means Clustering)
    Regroupe les enfants par tranche d'âge pour créer des classes homogènes.
    """
    def cluster(self, children_data: list, num_groups: int):
        if not children_data or len(children_data) < num_groups:
            return [children_data]
            
        X = np.array([child['age'] for child in children_data]).reshape(-1, 1)
        
        kmeans = KMeans(n_clusters=num_groups, random_state=42, n_init=10)
        labels = kmeans.fit_predict(X)
        
        clusters = {i: [] for i in range(num_groups)}
        for child, label in zip(children_data, labels):
            clusters[label].append(child)
            
        return list(clusters.values())


class RoomRecommender:
    """
    Modèle 2 : Decision Tree Classifier
    Dataset : room_dataset.csv (50 lignes)
    Features : capacité, âge moyen, surface, climatisation, équipement sportif, fournitures art, instruments musique
    Target : type de salle recommandé
    """
    def __init__(self):
        self.model = DecisionTreeClassifier(random_state=42, max_depth=6)
        self.label_encoder = LabelEncoder()
        self.trained = False
        self.accuracy = 0.0
        self._train()

    def _train(self):
        try:
            csv_path = os.path.join(BASE_DIR, 'room_dataset.csv')
            df = pd.read_csv(csv_path)
            
            X = df[['capacity', 'avg_age', 'surface_m2', 'is_climatise', 'has_sports_equipment', 'has_art_supplies', 'has_music_tools']]
            y = self.label_encoder.fit_transform(df['room_type'])
            
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
            
            self.model.fit(X_train, y_train)
            
            y_pred = self.model.predict(X_test)
            self.accuracy = round(accuracy_score(y_test, y_pred) * 100, 1)
            self.trained = True
            
            print(f"[ML] RoomRecommender entraîné sur {len(df)} lignes. Accuracy = {self.accuracy}%")
        except Exception as e:
            print(f"[ML] Erreur lors de l'entraînement RoomRecommender : {e}")

    def recommend(self, capacity: int, avg_age: int):
        if not self.trained:
            return "Modèle non entraîné"
        
        surface = capacity * 3
        is_climatise = 1
        has_sports = 1 if avg_age >= 4 else 0
        has_art = 1 if avg_age >= 3 else 0
        has_music = 0
        
        X_input = np.array([[capacity, avg_age, surface, is_climatise, has_sports, has_art, has_music]])
        prediction = self.model.predict(X_input)[0]
        room_type = self.label_encoder.inverse_transform([prediction])[0]
        
        probas = self.model.predict_proba(X_input)[0]
        max_proba = round(float(max(probas)) * 100, 1)
        
        return {
            "room_type": room_type,
            "confidence": max_proba,
            "accuracy_model": self.accuracy,
            "dataset_size": 50
        }
