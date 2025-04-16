package com.example.ai4speech;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface HuggingFaceApiService {
    @POST
    Call<PronunciationResponse> evaluatePronunciation(
            @Url String url,
            @Header("Authorization") String authToken,
            @Body PronunciationRequest request
    );
    
    @Multipart
    @POST
    Call<ResponseBody> uploadAudioFile(
            @Url String url,
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part audioFile,
            @Part("reference_audio_url") RequestBody referenceAudioUrl
    );
}