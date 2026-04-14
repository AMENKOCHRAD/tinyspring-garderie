import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import LabelEncoder
import joblib
import mysql.connector
from datetime import datetime, timedelta

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'garderie_db'
}

def get_training_data():
    """Récupère les données de commande_produit depuis MySQL"""
    conn = mysql.connector.connect(**DB_CONFIG)
    
    query = """
        SELECT 
            cp.produit_id,
            p.nom as produit_nom,
            p.stock as stock_actuel,
            cp.quantite,
            cp.prix_unitaire,
            c.date_commande,
            WEEK(c.date_commande) as semaine,
            MONTH(c.date_commande) as mois,
            YEAR(c.date_commande) as annee,
            DAYOFWEEK(c.date_commande) as jour_semaine,
            cat.nom as categorie
        FROM commande_produit cp
        JOIN commandes c ON c.id = cp.commande_id
        JOIN produits p ON p.id = cp.produit_id
        JOIN categories cat ON cat.id = p.categorie_id
        WHERE c.statut IN ('CONFIRMEE', 'EXPEDIEE', 'LIVREE')
        ORDER BY c.date_commande
    """
    
    df = pd.read_sql(query, conn)
    conn.close()
    return df


def build_features(df):
    """Construit les features pour le modèle"""
    
    # Agréger par produit + semaine
    weekly = df.groupby([
        'produit_id', 'produit_nom', 'categorie',
        'annee', 'semaine', 'mois'
    ]).agg(
        quantite_vendue=('quantite', 'sum'),
        nb_commandes=('quantite', 'count'),
        prix_moyen=('prix_unitaire', 'mean')
    ).reset_index()
    
    # Encoder la catégorie
    le = LabelEncoder()
    weekly['categorie_enc'] = le.fit_transform(weekly['categorie'])
    
    # Features de lag (semaines précédentes)
    weekly = weekly.sort_values(['produit_id', 'annee', 'semaine'])
    weekly['lag_1'] = weekly.groupby('produit_id')['quantite_vendue'].shift(1).fillna(0)
    weekly['lag_2'] = weekly.groupby('produit_id')['quantite_vendue'].shift(2).fillna(0)
    weekly['lag_4'] = weekly.groupby('produit_id')['quantite_vendue'].shift(4).fillna(0)
    
    # Moyenne mobile
    weekly['rolling_mean_3'] = weekly.groupby('produit_id')['quantite_vendue']\
        .transform(lambda x: x.rolling(3, min_periods=1).mean())
    
    # Tendance
    weekly['tendance'] = weekly.groupby('produit_id')['quantite_vendue']\
        .transform(lambda x: x.expanding().mean())
    
    return weekly, le


def train_model(df_features):
    """Entraîne le Random Forest"""
    
    feature_cols = [
        'semaine', 'mois', 'categorie_enc',
        'lag_1', 'lag_2', 'lag_4',
        'rolling_mean_3', 'tendance',
        'prix_moyen', 'nb_commandes'
    ]
    
    X = df_features[feature_cols].fillna(0)
    y = df_features['quantite_vendue']
    
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X, y)
    
    # Score du modèle
    score = model.score(X, y)
    print(f"Modèle entraîné — R² : {score:.3f}")
    
    # Sauvegarder
    joblib.dump(model, 'model.pkl')
    
    return model, feature_cols


def predict_next_weeks(produit_id, weeks=4):
    """Prédit les ventes pour les N prochaines semaines"""
    
    try:
        model = joblib.load('model.pkl')
    except:
        return None
    
    df = get_training_data()
    if df.empty:
        return generate_default_prediction(produit_id, weeks)
    
    df_features, le = build_features(df)
    
    # Données du produit
    produit_data = df_features[df_features['produit_id'] == produit_id]
    
    if produit_data.empty:
        return generate_default_prediction(produit_id, weeks)
    
    last_row = produit_data.iloc[-1]
    predictions = []
    
    now = datetime.now()
    
    for i in range(1, weeks + 1):
        future_date = now + timedelta(weeks=i)
        
        features = {
            'semaine': future_date.isocalendar()[1],
            'mois': future_date.month,
            'categorie_enc': last_row['categorie_enc'],
            'lag_1': last_row['quantite_vendue'] if i == 1 else predictions[-1]['quantite_prevue'],
            'lag_2': last_row['lag_1'] if i == 1 else (predictions[-2]['quantite_prevue'] if len(predictions) >= 2 else 0),
            'lag_4': last_row['lag_4'],
            'rolling_mean_3': last_row['rolling_mean_3'],
            'tendance': last_row['tendance'],
            'prix_moyen': last_row['prix_moyen'],
            'nb_commandes': last_row['nb_commandes']
        }
        
        X_pred = pd.DataFrame([features])
        quantite = max(0, round(model.predict(X_pred)[0]))
        
        # Score de confiance basé sur la variance des arbres
        tree_preds = [tree.predict(X_pred)[0] for tree in model.estimators_]
        confidence = max(0.5, 1 - (np.std(tree_preds) / (np.mean(tree_preds) + 1)))
        
        predictions.append({
            'semaine': i,
            'periode_debut': future_date.strftime('%Y-%m-%d'),
            'periode_fin': (future_date + timedelta(days=6)).strftime('%Y-%m-%d'),
            'quantite_prevue': int(quantite),
            'confiance': round(float(confidence), 2)
        })
    
    return predictions


def generate_default_prediction(produit_id, weeks):
    """Prédiction par défaut si pas assez de données"""
    now = datetime.now()
    return [{
        'semaine': i,
        'periode_debut': (now + timedelta(weeks=i)).strftime('%Y-%m-%d'),
        'periode_fin': (now + timedelta(weeks=i, days=6)).strftime('%Y-%m-%d'),
        'quantite_prevue': 5,
        'confiance': 0.3
    } for i in range(1, weeks + 1)]