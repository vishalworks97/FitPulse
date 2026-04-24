package com.example.fitpulse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GroqApi {

    @POST("chat/completions")
    Call<GroqResponse> getChatResponse(@Body GroqRequest request);
}
