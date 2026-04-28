from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import uvicorn
from ml_model import ActivityRecommender
from advanced_ml import ChildClusterer, RoomRecommender

app = FastAPI()

# Initialisation des modèles ML
print("=" * 60)
print("🧠 Initialisation des modèles Machine Learning...")
print("=" * 60)

recommender = ActivityRecommender()
clusterer = ChildClusterer()
room_recommender = RoomRecommender()

print("=" * 60)
print("✅ Tous les modèles ML sont prêts !")
print("=" * 60)

# --- DTOs ---
class ActivityRequest(BaseModel):
    ageMin: int
    ageMax: int
    capacite: int

class ChildData(BaseModel):
    id: int
    age: int
    nom: str = "Enfant"

class ClusterRequest(BaseModel):
    children: List[ChildData]
    numGroups: int

class RoomRequest(BaseModel):
    capacite: int
    ageMoyen: int

# --- ROUTES ---

# Route 1 : K-Nearest Neighbors (Recommandation Activités - basé sur activity_dataset.csv)
@app.post("/recommend-activities")
async def recommend_activities(request: ActivityRequest):
    try:
        result_text = recommender.recommend(request.ageMin, request.ageMax, request.capacite)
        return {"response": result_text}
    except Exception as e:
        return {"response": f"⚠️ Erreur lors de la recommandation ML : {str(e)}"}

# Route 2 : K-Means Clustering (Répartition des enfants)
@app.post("/ml/cluster-children")
async def cluster_children(request: ClusterRequest):
    try:
        children_dict = [{"id": c.id, "age": c.age, "nom": c.nom} for c in request.children]
        clusters = clusterer.cluster(children_dict, request.numGroups)
        return {"clusters": clusters}
    except Exception as e:
        return {"error": str(e)}

# Route 3 : Decision Tree (Recommandation de salle - basé sur room_dataset.csv)
@app.post("/ml/recommend-room")
async def recommend_room(request: RoomRequest):
    try:
        result = room_recommender.recommend(request.capacite, request.ageMoyen)
        return result
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
