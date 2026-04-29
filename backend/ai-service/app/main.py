from fastapi import FastAPI

from app.api.routes import router


app = FastAPI(
    title="Transport AI Service",
    description="Microservice IA pour prediction de la demande et detection d anomalies de transport.",
    version="1.0.0",
)

app.include_router(router)


@app.get("/", tags=["health"])
def root() -> dict[str, str]:
    return {
        "service": "transport-ai-service",
        "status": "up",
    }


@app.get("/health", tags=["health"])
def health() -> dict[str, str]:
    return {"status": "ok"}
