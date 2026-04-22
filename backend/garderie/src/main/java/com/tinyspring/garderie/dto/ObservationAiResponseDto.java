package com.tinyspring.garderie.dto;

import java.util.List;

public class ObservationAiResponseDto {
    private String description;
    private String resume;
    private String messageParent;
    private List<String> actionsProposees;
    private List<String> pointsASurveiller;
    private List<String> questionsAuxParents;
    private List<String> problemesPossibles;
    private List<String> contexteRecent;

    // Advanced IA (local) signals
    private String urgenceSuggeree;
    private Integer scoreUrgence; // 0..100
    private List<String> tags;
    private List<String> signauxDetectes;
    private List<String> observationsSimilaires;

    public ObservationAiResponseDto() {}

    public ObservationAiResponseDto(String description,
                                    String resume,
                                    String messageParent,
                                    List<String> actionsProposees,
                                    List<String> pointsASurveiller,
                                    List<String> questionsAuxParents,
                                    List<String> problemesPossibles,
                                    List<String> contexteRecent) {
        this.description = description;
        this.resume = resume;
        this.messageParent = messageParent;
        this.actionsProposees = actionsProposees;
        this.pointsASurveiller = pointsASurveiller;
        this.questionsAuxParents = questionsAuxParents;
        this.problemesPossibles = problemesPossibles;
        this.contexteRecent = contexteRecent;
    }

    public ObservationAiResponseDto(String description,
                                    String resume,
                                    String messageParent,
                                    List<String> actionsProposees,
                                    List<String> pointsASurveiller,
                                    List<String> questionsAuxParents,
                                    List<String> problemesPossibles,
                                    List<String> contexteRecent,
                                    String urgenceSuggeree,
                                    Integer scoreUrgence,
                                    List<String> tags,
                                    List<String> signauxDetectes,
                                    List<String> observationsSimilaires) {
        this.description = description;
        this.resume = resume;
        this.messageParent = messageParent;
        this.actionsProposees = actionsProposees;
        this.pointsASurveiller = pointsASurveiller;
        this.questionsAuxParents = questionsAuxParents;
        this.problemesPossibles = problemesPossibles;
        this.contexteRecent = contexteRecent;
        this.urgenceSuggeree = urgenceSuggeree;
        this.scoreUrgence = scoreUrgence;
        this.tags = tags;
        this.signauxDetectes = signauxDetectes;
        this.observationsSimilaires = observationsSimilaires;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getMessageParent() {
        return messageParent;
    }

    public void setMessageParent(String messageParent) {
        this.messageParent = messageParent;
    }

    public List<String> getActionsProposees() {
        return actionsProposees;
    }

    public void setActionsProposees(List<String> actionsProposees) {
        this.actionsProposees = actionsProposees;
    }

    public List<String> getPointsASurveiller() {
        return pointsASurveiller;
    }

    public void setPointsASurveiller(List<String> pointsASurveiller) {
        this.pointsASurveiller = pointsASurveiller;
    }

    public List<String> getQuestionsAuxParents() {
        return questionsAuxParents;
    }

    public void setQuestionsAuxParents(List<String> questionsAuxParents) {
        this.questionsAuxParents = questionsAuxParents;
    }

    public List<String> getProblemesPossibles() {
        return problemesPossibles;
    }

    public void setProblemesPossibles(List<String> problemesPossibles) {
        this.problemesPossibles = problemesPossibles;
    }

    public List<String> getContexteRecent() {
        return contexteRecent;
    }

    public void setContexteRecent(List<String> contexteRecent) {
        this.contexteRecent = contexteRecent;
    }

    public String getUrgenceSuggeree() {
        return urgenceSuggeree;
    }

    public void setUrgenceSuggeree(String urgenceSuggeree) {
        this.urgenceSuggeree = urgenceSuggeree;
    }

    public Integer getScoreUrgence() {
        return scoreUrgence;
    }

    public void setScoreUrgence(Integer scoreUrgence) {
        this.scoreUrgence = scoreUrgence;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getSignauxDetectes() {
        return signauxDetectes;
    }

    public void setSignauxDetectes(List<String> signauxDetectes) {
        this.signauxDetectes = signauxDetectes;
    }

    public List<String> getObservationsSimilaires() {
        return observationsSimilaires;
    }

    public void setObservationsSimilaires(List<String> observationsSimilaires) {
        this.observationsSimilaires = observationsSimilaires;
    }
}
