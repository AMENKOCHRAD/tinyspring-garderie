# Transport AI Service

Microservice IA en `Python` + `FastAPI` pour la gestion du transport.

## Fonctionnalites

1. `POST /predict-demand`
   - predit le nombre de demandes de transport
   - retourne aussi un niveau de demande (`LOW`, `MEDIUM`, `HIGH`)

2. `POST /detect-anomaly`
   - detecte les demandes suspectes ou incoherentes
   - combine des regles metier simples et un modele `IsolationForest`

## Structure

```text
backend/ai-service
├── app
│   ├── api/routes.py
│   ├── main.py
│   ├── models/schemas.py
│   ├── services/model_service.py
│   └── training/train.py
├── data
│   ├── transport_demand_history.csv
│   └── transport_requests_history.csv
├── models
├── requirements.txt
└── README.md
```

## Installation

```bash
cd backend/ai-service
py -3.13 -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## Entrainement des modeles

```bash
python -m app.training.train
```

Cela genere :

- `models/demand_model.joblib`
- `models/anomaly_model.joblib`

## Lancement du service

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

Swagger :

- [http://localhost:8001/docs](http://localhost:8001/docs)

## Test rapide

### 1. Prediction de la demande

```bash
curl -X POST "http://localhost:8001/predict-demand" ^
  -H "Content-Type: application/json" ^
  -d "{\"target_date\":\"2026-04-20\",\"hour\":8,\"active_children_count\":145,\"available_transport_count\":10,\"avg_route_distance_km\":6.1,\"rain_flag\":true,\"school_break_flag\":false}"
```

### 2. Detection d anomalie

```bash
curl -X POST "http://localhost:8001/detect-anomaly" ^
  -H "Content-Type: application/json" ^
  -d "{\"child_id\":12,\"request_date\":\"2026-04-20\",\"requested_hour\":23,\"pickup_address\":\"Rue des Jasmins, Tunis\",\"dropoff_address\":\"Rue des Jasmins, Tunis\",\"seats_requested\":1,\"route_distance_km\":0,\"is_round_trip\":false,\"recent_requests\":[{\"child_id\":12,\"request_date\":\"2026-04-20\",\"requested_hour\":23,\"pickup_address\":\"Rue des Jasmins, Tunis\",\"dropoff_address\":\"Rue des Jasmins, Tunis\"}]}"
```

## Integration avec Spring Boot

Le plus simple est d appeler ce microservice depuis votre backend Spring Boot avec `RestTemplate` ou `WebClient`.

### Exemple de configuration

Dans `application.yml` :

```yaml
ai:
  service:
    base-url: http://localhost:8001
```

### Exemple avec WebClient

```java
@Service
public class AiTransportClient {

    private final WebClient webClient;

    public AiTransportClient(
            @Value("${ai.service.base-url}") String baseUrl,
            WebClient.Builder builder
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public DemandPredictionResponse predictDemand(DemandPredictionRequest request) {
        return webClient.post()
                .uri("/predict-demand")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DemandPredictionResponse.class)
                .block();
    }

    public AnomalyDetectionResponse detectAnomaly(AnomalyDetectionRequest request) {
        return webClient.post()
                .uri("/detect-anomaly")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnomalyDetectionResponse.class)
                .block();
    }
}
```

### Cas d usage cote Spring Boot

- avant d enregistrer une demande transport, appeler `/detect-anomaly`
- pour un tableau de bord admin, appeler `/predict-demand`
- en cas d indisponibilite du microservice, prevoir un fallback simple:
  - journaliser l erreur
  - continuer sans blocage metier si necessaire

## Notes

- les CSV fournis sont des jeux d exemple pour demarrer vite
- les modeles sont simples et facilement remplaçables plus tard par de vrais jeux de donnees
- la validation des entrees est geree par `Pydantic`
