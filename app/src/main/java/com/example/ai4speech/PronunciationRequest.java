package com.example.ai4speech;

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

