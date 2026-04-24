package com.example.fitpulse;

public class OnboardingItem {
    private String title;
    private String description;
    private int lottieAnimation; // Raw resource ID for animations

    public OnboardingItem(String title, String description, int lottieAnimation) {
        this.title = title;
        this.description = description;
        this.lottieAnimation = lottieAnimation;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getLottieAnimation() { return lottieAnimation; }
}