import os
import pickle
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder, OneHotEncoder, StandardScaler
from sklearn.model_selection import train_test_split

DATA_PATH = "data/tunisia_daycare_events_140.csv"
MODEL_DIR = "model"

df = pd.read_csv(DATA_PATH)

# Nettoyage
df["price_dt"] = pd.to_numeric(df["price_dt"], errors="coerce").fillna(0)
df["age_min"] = pd.to_numeric(df["age_min"], errors="coerce").fillna(2)
df["age_max"] = pd.to_numeric(df["age_max"], errors="coerce").fillna(6)

df["combined_text"] = (
    df["title"] + " " +
    df["short_description"] + " " +
    df["tags"]
)

categorical = ["event_type","venue_type","indoor_outdoor","season","budget_band","city"]
numeric = ["price_dt","age_min","age_max","duration_minutes"]
text = "combined_text"

X = df[categorical + numeric + [text]]
y = df["recommended_label"]

le = LabelEncoder()
y_encoded = le.fit_transform(y)

preprocessor = ColumnTransformer([
    ("cat", OneHotEncoder(handle_unknown="ignore"), categorical),
    ("num", StandardScaler(), numeric),
    ("txt", TfidfVectorizer(max_features=1000), text)
])

model = LogisticRegression(max_iter=3000)

pipeline = Pipeline([
    ("preprocessor", preprocessor),
    ("classifier", model)
])

X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.2)

pipeline.fit(X_train, y_train)

os.makedirs(MODEL_DIR, exist_ok=True)

pickle.dump(pipeline, open("model/pipeline.pkl","wb"))
pickle.dump(le, open("model/label_encoder.pkl","wb"))

print("✅ Model trained")