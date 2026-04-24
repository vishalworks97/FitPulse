package com.example.fitpulse;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroqRequest {
    @SerializedName("model")
    public String model;

    @SerializedName("messages")
    public List<Message> messages;

    public GroqRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        @SerializedName("role")
        public String role;

        @SerializedName("content")
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
