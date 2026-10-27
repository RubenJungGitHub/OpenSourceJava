from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
from typing import List

app = FastAPI(title="VocBench Zero-Shot Classifier")

# Het model wordt eenmalig geladen bij het starten van de Pod
classifier = pipeline("zero-shot-classification", model="facebook/bart-large-mnli")

class ClassificationRequest(BaseModel):
    text: str
    labels: List[str]

@app.post("/classify")
async def classify_text(request: ClassificationRequest):
    if not request.labels:
        raise HTTPException(status_code=400, detail="Geen prefLabels meegegeven.")
    
    result = classifier(request.text, candidate_labels=request.labels)
    
    return {
        "top_label": result['labels'][0],
        "confidence": result['scores'][0]
    }