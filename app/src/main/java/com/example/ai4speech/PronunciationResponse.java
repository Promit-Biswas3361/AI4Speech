package com.example.ai4speech;

import com.google.gson.annotations.SerializedName;

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
