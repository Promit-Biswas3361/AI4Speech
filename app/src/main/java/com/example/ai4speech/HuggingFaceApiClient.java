package com.example.ai4speech;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HuggingFaceApiClient {
    private static final String BASE_URL = "https://api-inference.huggingface.co/";
    private static final String API_KEY = "hf_uhwXzSYUcAbkESNmELNeEVehjMNpQKVbU"; //e
    
    private static HuggingFaceApiClient instance;
    private final com.example.ai4speech.HuggingFaceApiService apiService;

    private HuggingFaceApiClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(HuggingFaceApiService.class);
    }

    public static synchronized HuggingFaceApiClient getInstance() {
        if (instance == null) {
            instance = new HuggingFaceApiClient();
        }
        return instance;
    }

    public HuggingFaceApiService getApiService() {
        return apiService;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + API_KEY;
    }

    public MultipartBody.Part prepareAudioFilePart(File file) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
        return MultipartBody.Part.createFormData("audio", file.getName(), requestFile);
    }
}