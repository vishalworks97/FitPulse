package com.example.fitpulse;

import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {

    private static final String TAG = "GeminiHelper";
    
    // Explicitly using the most stable model name
    private static final String MODEL_NAME = "gemini-1.5-flash";
    private static final String API_KEY = "AIzaSyCn8idi6P1jWJZ0ykRgizmpCnu9ZZXAPco";
    
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public interface ResponseCallback {
        void onResponse(String response);
    }

    public static void getAiResponse(String prompt, ResponseCallback callback) {
        try {
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.1f;
            GenerationConfig generationConfig = configBuilder.build();

            List<SafetySetting> safetySettings = new ArrayList<>();
            safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH));
            safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));
            safetySettings.add(new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH));
            safetySettings.add(new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH));

            // Ensure the model is initialized with the correct API Key and Model Name
            GenerativeModel gm = new GenerativeModel(
                MODEL_NAME, 
                API_KEY, 
                generationConfig, 
                safetySettings
            );
            
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> future = model.generateContent(content);

            Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            callback.onResponse(text.trim());
                        } else {
                            callback.onResponse("AI: No result found.");
                        }
                    } catch (Exception e) {
                        callback.onResponse("Safety Filter: Content blocked.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini Error: " + t.getMessage());
                    // Catching the error message to avoid serialization crashes in the UI
                    String msg = t.getMessage();
                    if (msg != null && msg.contains("404")) {
                        callback.onResponse("Error: Model not found. Check your API key region.");
                    } else if (msg != null && msg.contains("403")) {
                        callback.onResponse("Error: API Key Restricted. Enable 'Generative Language API'.");
                    } else {
                        callback.onResponse("AI Error: Connection failed.");
                    }
                }
            }, executor);
        } catch (Exception e) {
            callback.onResponse("Error: Initialization failed.");
        }
    }

    public static void getDietAdvice(double bmi, String status, ResponseCallback callback) {
        String prompt = "Give a 10-word fitness tip for BMI " + bmi + " (" + status + ").";
        getAiResponse(prompt, callback);
    }
}
