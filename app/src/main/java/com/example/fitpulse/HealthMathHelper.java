package com.example.fitpulse;

public class HealthMathHelper {

    // Calculate BMI: weight / (height/100)^2
    public static double calculateBMI(double weightKg, double heightCm) {
        if (heightCm <= 0) return 0;
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        return Math.round(bmi * 10.0) / 10.0; // Returns 1 decimal place
    }

    // Get Status String
    public static String getBMIStatus(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    // Calculate BMR (Mifflin-St Jeor Equation)
    public static int calculateBMR(double weight, double height, int age, String gender) {
        double bmr;
        if ("Male".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }
        return (int) bmr;
    }

    // Calculate Daily Calorie Goal based on BMI
    public static int getCalorieGoal(double bmi) {
        if (bmi < 18.5) return 2500; // Need to gain
        if (bmi > 25) return 1800;   // Need to lose
        return 2200;                // Maintain
    }
}
