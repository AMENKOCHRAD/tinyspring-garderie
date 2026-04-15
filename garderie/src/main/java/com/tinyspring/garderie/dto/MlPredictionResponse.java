package com.tinyspring.garderie.dto;

public class MlPredictionResponse {

    private String predictedCategory;
    private Double confidence;

    private String predictedPriority;
    private Double priorityConfidence;

    public MlPredictionResponse() {
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getPredictedPriority() {
        return predictedPriority;
    }

    public void setPredictedPriority(String predictedPriority) {
        this.predictedPriority = predictedPriority;
    }

    public Double getPriorityConfidence() {
        return priorityConfidence;
    }

    public void setPriorityConfidence(Double priorityConfidence) {
        this.priorityConfidence = priorityConfidence;
    }
}