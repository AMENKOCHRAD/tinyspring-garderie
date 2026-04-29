import pandas as pd
from sklearn.neighbors import NearestNeighbors
import os

class ActivityRecommender:
    def __init__(self):
        # Charger le dataset
        dataset_path = os.path.join(os.path.dirname(__file__), 'activity_dataset.csv')
        self.df = pd.read_csv(dataset_path)
        
        # Caractéristiques pour l'entraînement (min_age, max_age, capacity_required)
        self.features = self.df[['min_age', 'max_age', 'capacity_required']]
        
        # Initialiser le modèle KNN
        self.model = NearestNeighbors(n_neighbors=3, algorithm='auto')
        self.model.fit(self.features)

    def recommend(self, min_age: int, max_age: int, capacity: int):
        # Le modèle cherche les activités les plus "proches" de la requête
        query = [[min_age, max_age, capacity]]
        distances, indices = self.model.kneighbors(query)
        
        recommendations = []
        for i in indices[0]:
            activity = self.df.iloc[i]
            # Formater la recommandation en texte clair sans markdown
            text = f"{activity['name']} ({activity['theme']})\n"
            text += f"• Âge adapté : {activity['min_age']} à {activity['max_age']} ans\n"
            text += f"• Capacité idéale : jusqu'à {activity['capacity_required']} enfants\n"
            text += f"• Description : {activity['description']}"
            recommendations.append(text)
            
        return "\n\n".join(recommendations)
