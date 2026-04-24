package com.example.fitpulse;

public class Workout {
    private String name;
    private int gifResource;
    private double metValue; // MET value (e.g., Jumping Jacks = 8.0)

    public Workout(String name, int gifResource, double metValue) {
        this.name = name;
        this.gifResource = gifResource;
        this.metValue = metValue;
    }

    public String getName() { return name; }
    public int getGifResource() { return gifResource; }
    public double getMetValue() { return metValue; }
}