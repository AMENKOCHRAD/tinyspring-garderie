package com.tinyspring.garderie.dto;

public class MlPredictionResponse {

    private String predictedCategory;
    private Double classificationConfidence;

    private String predictedPriority;
    private Double priorityConfidence;

    private String decisionRecommendation;
    private Double decisionConfidence;

    public MlPredictionResponse() {
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public Double getClassificationConfidence() {
        return classificationConfidence;
    }

    public void setClassificationConfidence(Double classificationConfidence) {
        this.classificationConfidence = classificationConfidence;
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

    public String getDecisionRecommendation() {
        return decisionRecommendation;
    }

    public void setDecisionRecommendation(String decisionRecommendation) {
        this.decisionRecommendation = decisionRecommendation;
    }

    public Double getDecisionConfidence() {
        return decisionConfidence;
    }

    public void setDecisionConfidence(Double decisionConfidence) {
        this.decisionConfidence = decisionConfidence;
    }
}