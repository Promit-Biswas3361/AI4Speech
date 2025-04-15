package com.example.ai4speech.api;

import com.google.gson.annotations.SerializedName;

public class PronunciationRequest {
    @SerializedName("user_audio_path")
    private String userAudioPath;
    
    @SerializedName("reference_audio_id")
    private String referenceAudioId;

    public PronunciationRequest(String userAudioPath, String referenceAudioId) {
        this.userAudioPath = userAudioPath;
        this.referenceAudioId = referenceAudioId;
    }
}

public class PronunciationResponse {
    @SerializedName("mos")
    private double meanOpinionScore;
    
    @SerializedName("accuracy")
    private double accuracyScore;
    
    @SerializedName("fluency")
    private double fluencyScore;
    
    @SerializedName("completeness")
    private double completenessScore;
    
    @SerializedName("pronunciation")
    private double pronunciationScore;

    public double getMeanOpinionScore() {
        return meanOpinionScore;
    }

    public double getAccuracyScore() {
        return accuracyScore;
    }

    public double getFluencyScore() {
        return fluencyScore;
    }

    public double getCompletenessScore() {
        return completenessScore;
    }

    public double getPronunciationScore() {
        return pronunciationScore;
    }
}