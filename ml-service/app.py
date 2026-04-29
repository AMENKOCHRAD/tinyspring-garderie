from flask import Flask, jsonify, request
from model import get_training_data, build_features, train_model, predict_next_weeks
import mysql.connector
import pandas as pd
from datetime import datetime

app = Flask(__name__)

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'garderie_db'
}


@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'timestamp': datetime.now().isoformat()})


@app.route('/train', methods=['POST'])
def train():
    """Entraîner le modèle avec les données actuelles"""
    try:
        df = get_training_data()
        
        if len(df) < 10:
            return jsonify({
                'success': False,
                'message': f'Pas assez de données ({len(df)} lignes). Minimum 10 requis.'
            }), 400
        
        df_features, le = build_features(df)
        model, feature_cols = train_model(df_features)
        
        return jsonify({
            'success': True,
            'message': 'Modèle entraîné avec succès',
            'nb_echantillons': len(df),
            'nb_produits': df['produit_id'].nunique()
        })
    
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/predict/<int:produit_id>', methods=['GET'])
def predict(produit_id):
    """Prédire les ventes pour un produit"""
    weeks = request.args.get('weeks', 4, type=int)
    
    try:
        predictions = predict_next_weeks(produit_id, weeks)
        
        if not predictions:
            return jsonify({'error': 'Impossible de prédire'}), 400
        
        # Récupérer le stock actuel
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        cursor.execute(
            "SELECT id, nom, stock, seuil_alerte FROM produits WHERE id = %s",
            (produit_id,)
        )
        produit = cursor.fetchone()
        conn.close()
        
        if not produit:
            return jsonify({'error': 'Produit introuvable'}), 404
        
        # Calcul des alertes
        total_prevu_4sem = sum(p['quantite_prevue'] for p in predictions)
        stock_suffisant = produit['stock'] >= total_prevu_4sem
        
        return jsonify({
            'produit_id': produit_id,
            'produit_nom': produit['nom'],
            'stock_actuel': produit['stock'],
            'seuil_alerte': produit['seuil_alerte'],
            'total_prevu_4_semaines': total_prevu_4sem,
            'stock_suffisant': stock_suffisant,
            'alerte_rupture': not stock_suffisant,
            'predictions': predictions
        })
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/predict/all', methods=['GET'])
def predict_all():
    """Prédire pour tous les produits"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT id, nom, stock, seuil_alerte FROM produits")
        produits = cursor.fetchall()
        conn.close()
        
        results = []
        for produit in produits:
            preds = predict_next_weeks(produit['id'], 4)
            total = sum(p['quantite_prevue'] for p in preds) if preds else 0
            
            results.append({
                'produit_id': produit['id'],
                'produit_nom': produit['nom'],
                'stock_actuel': produit['stock'],
                'total_prevu_4_semaines': total,
                'alerte_rupture': produit['stock'] < total,
                'semaines': preds or []
            })
        
        # Trier par priorité (rupture en premier)
        results.sort(key=lambda x: (not x['alerte_rupture'], x['stock_actuel']))
        
        return jsonify({
            'timestamp': datetime.now().isoformat(),
            'nb_produits': len(results),
            'alertes_rupture': sum(1 for r in results if r['alerte_rupture']),
            'produits': results
        })
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)