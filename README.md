<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/AI-Groq%20Llama%203-00FF41?style=for-the-badge&logo=meta&logoColor=black"/>
<img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
<img src="https://img.shields.io/badge/Design-Material%20Design%203-757575?style=for-the-badge&logo=material-design&logoColor=white"/>

<br/><br/>

```
  ███████╗██╗████████╗██████╗ ██╗   ██╗██╗     ███████╗███████╗
  ██╔════╝██║╚══██╔══╝██╔══██╗██║   ██║██║     ██╔════╝██╔════╝
  █████╗  ██║   ██║   ██████╔╝██║   ██║██║     ███████╗█████╗
  ██╔══╝  ██║   ██║   ██╔═══╝ ██║   ██║██║     ╚════██║██╔══╝
  ██║     ██║   ██║   ██║     ╚██████╔╝███████╗███████║███████╗
  ╚═╝     ╚═╝   ╚═╝   ╚═╝      ╚═════╝ ╚══════╝╚══════╝╚══════╝
```

### **AI-Powered Fitness & Nutrition Android App**
*Real intelligence. Real food. Real results.*

<br/>

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![API](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/versions/oreo)
[![Groq](https://img.shields.io/badge/LLM-Llama%203.3--70B-blueviolet.svg)](https://groq.com)
[![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen.svg)]()

</div>

---

## What is FitPulse?

Most fitness apps treat you like a number. They give you a calorie budget, log your steps, and call it a day. FitPulse does things differently.

We built FitPulse because we were frustrated with apps that had zero cultural awareness — tools that would suggest "quinoa salad" to someone in Pakistan, or a workout plan built for a gym that costs more than a month's rent. The AI inside FitPulse actually knows where you're from, what you eat, and what your body looks like right now — and it gives advice that fits your life, not some fitness influencer's.

It's an Android app built in Java, powered by Groq's Llama 3 model for real-time AI recommendations, and backed by Firebase for authentication and cloud sync. Dark theme, neon green accents, and a UI we're genuinely proud of.

---

## Screenshots

<div align="center">

| Home Dashboard | Diet Plan | AI Meal Ideas |
|:-:|:-:|:-:|
| <img src="readme_assets/screen_home.jpg" width="200"/> | <img src="readme_assets/screen_diet.jpg" width="200"/> | <img src="readme_assets/screen_diet_ai.jpg" width="200"/> |
| Daily overview — AI tips, calorie charts & steps | Breakfast logged, AI Recommend on each slot | Pakistan Cuisine suggestions from Groq Llama 3 |

| Workout Tracker | Profile |
|:-:|:-:|
| <img src="readme_assets/screen_workout.jpg" width="200"/> | <img src="readme_assets/screen_profile.jpg" width="200"/> |
| Live timer, exercise GIF, MET-based calorie math | BMI, stats grid, diet prefs & activity history |

</div>

---

## Features

### 🤖 Real AI, Not Canned Tips
Every recommendation you see — the daily status card, the meal suggestions, the fitness tips — comes from a live call to Groq's Llama 3.3-70B model. We pass in your BMI, calorie budget, dietary preference, and country, and you get advice that actually fits your context. No hardcoded tip arrays, no rotating motivational quotes.

### 🍽️ Country-Aware Diet Planning
This was the feature we cared most about. When you tap "AI Recommend" on a meal slot, the app sends your country to Groq as part of the prompt. If you're in Pakistan, you get suggestions like aaloo pratha, halwa puri, and besan cheela — not chicken breast and broccoli. It supports vegetarian and non-vegetarian dietary modes, and you can add any suggested meal directly to your plan.

### 🏋️ Workout Tracker with Live Burn Estimation
Pick any exercise from the library, hit play, and a live chronometer starts running alongside the exercise's animated form guide (loaded via Glide). Calories are calculated in real time using the MET formula: `MET × weight(kg) × time(hours)`. Tap SAVE when you're done and the session is written to Firestore immediately.

### 📊 Visual Health Dashboard
The home screen gives you a full picture at a glance — two pie charts showing your calorie intake vs. your daily budget and active burn vs. target, a step counter pulling from the device pedometer, and a log of today's workout activity. Everything updates live without needing a refresh.

### 🔐 One-Tap Google Sign-In
Authentication is handled entirely by Firebase + Google OAuth 2.0. No passwords, no email verification, no friction. Returning users go straight to the dashboard. New users hit the onboarding flow, then profile setup, and are in the app in under two minutes.

### ✨ Smooth Onboarding
Three onboarding slides using ViewPager2 with Lottie animations introduce the AI features before asking you to sign in. You can skip if you want — we just felt it was worth showing what the app can do before asking for your data.

---

## Tech Stack

| Layer | Technology | Why we chose it |
|-------|-----------|-----------------|
| Language | Java (OpenJDK 17) | Stable, well-documented, covered in coursework |
| UI Framework | XML + Material Design 3 | Dark theme support out of the box, clean components |
| Primary AI | Groq Cloud API — Llama 3.3-70B | Fastest LLM inference we found; matters a lot on mobile |
| Secondary AI | Google Gemini API | Used for optional generative enhancement layer |
| Auth | Firebase Authentication | Google OAuth 2.0, one-tap sign-in |
| Database | Firebase Firestore | Real-time sync, scales automatically, works offline-ish |
| HTTP Client | Retrofit + OkHttp | Clean REST API integration for Groq/Gemini endpoints |
| Charts | MPAndroidChart | Best charting library for Android, handles pie/bar well |
| Animations | Lottie | Tiny file sizes, gorgeous animations for onboarding |
| Image Loading | Glide | Handles animated GIFs for workout form guides |

---

## Architecture

FitPulse uses a **Modular Fragment-Host Architecture**. `MainActivity` is the host — it holds the `BottomNavigationView` and swaps fragments in and out as the user taps between tabs. Each fragment owns its own UI state and talks to helper classes rather than reaching into other fragments.

```
com.fitpulse/
│
├── MainActivity.java             # Fragment host + bottom nav controller
├── LoginActivity.java            # Google OAuth flow + routing (new vs. returning user)
├── ProfileSetupActivity.java     # First-run data collection
│
├── fragments/
│   ├── HomeFragment.java         # Dashboard — AI tip, charts, steps, workout log
│   ├── DietFragment.java         # Meal slots + AI recommendations
│   ├── WorkoutFragment.java      # Exercise library + active session timer
│   └── ProfileFragment.java     # User stats + settings
│
├── helpers/
│   ├── GrokHelper.java           # Groq API calls — prompt construction + response parsing
│   ├── HealthMathHelper.java     # BMI calc, calorie estimation, MET formula
│   └── PreferenceManager.java   # SharedPreferences wrapper for local storage
│
└── adapters/
    ├── WorkoutAdapter.java       # RecyclerView adapter for exercise list
    └── OnboardingAdapter.java    # ViewPager2 adapter for onboarding slides
```

**Data flow for an AI recommendation:**
```
User taps "AI Recommend"
  → DietFragment builds context object (country, diet pref, calorie budget, meal slot)
  → GrokHelper constructs prompt string
  → Retrofit sends POST to api.groq.com/openai/v1/chat/completions
  → Response arrives (avg ~1.2s on Groq)
  → RegEx parser extracts meal names + calorie values
  → Bottom sheet dialog renders options
  → User selects → item written to Firestore persisted_diet/{mealType}
  → HomeFragment calorie chart updates via SnapshotListener
```

---

## Database Structure (Firestore)

```
users/
  └── {userId}/
        name, age, height, weight, gender, country,
        dietPreference, calorieTarget, bmi

daily_logs/
  └── {YYYY-MM-DD}/
        steps, caloriesBurned, caloriesEaten, workoutCount

persisted_diet/
  └── {mealType}/           # breakfast | lunch | snacks | dinner
        items: [ { name, calories, status } ]
```

We kept the structure flat on purpose. Deep nesting in Firestore gets messy fast and makes read queries expensive. Flat documents, keyed sensibly, is the way to go.

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- A Firebase project (free Spark plan works fine)
- A Groq API key (free tier available at [console.groq.com](https://console.groq.com))

### Setup

**1. Clone the repo**
```bash
git clone https://github.com/yourusername/fitpulse.git
cd fitpulse
```

**2. Firebase setup**
- Go to [Firebase Console](https://console.firebase.google.com) → New Project
- Add an Android app, enter your package name
- Download `google-services.json` and drop it into `/app`
- Enable **Authentication → Google** and **Firestore Database**

**3. Add your API keys**

Open `app/src/main/java/.../helpers/GrokHelper.java` and add your Groq key:
```java
private static final String GROQ_API_KEY = "gsk_your_key_here";
```

> ⚠️ For production, move keys to `local.properties` or Android Keystore. Don't commit keys to git.

**4. Build and run**
```bash
./gradlew assembleDebug
```
Or just press **Run** in Android Studio. Min SDK is 26 (Android 8.0).

---

## Key Implementation Details

### BMI Classification
```java
public static String getBMIStatus(double bmi) {
    if (bmi < 18.5) return "Underweight";
    if (bmi < 25.0) return "Healthy";
    if (bmi < 30.0) return "Overweight";
    return "Obese";
}
```
This classification feeds directly into the Groq prompt — someone classified as "Overweight" gets different advice than someone "Healthy". Small detail, big difference in output quality.

### MET-Based Calorie Calculation
```java
// calories = MET × weight_kg × duration_hours
public static double calculateCaloriesBurned(double met, double weightKg, long elapsedMs) {
    double hours = elapsedMs / 3_600_000.0;
    return met * weightKg * hours;
}
```
Every exercise in the library has a MET value. Bicycle Crunches is 8.0, a brisk walk is 3.5. This makes calorie estimates actually meaningful rather than just made-up numbers.

### Prompt Construction Pattern
```java
String prompt = String.format(
    "You are a nutrition expert. Suggest 4 %s meal options for someone in %s. " +
    "They follow a %s diet with a daily calorie budget of %d kcal. " +
    "Format each as: 'Meal Name : X kcal'. Be specific and use local cuisine.",
    mealType, country, dietPref, calorieBudget
);
```
The structured format forces predictable output that RegEx can parse reliably.

---

## Challenges We Actually Ran Into

**LLM response parsing** was harder than expected. Even with a structured prompt, Groq occasionally returns slight variations in formatting. We tuned the prompt wording for about three iterations before the RegEx became reliable. The key was being very specific about the output format in the prompt itself.

**API latency on mobile** feels way longer than in a browser. A 1.2-second response is fine on desktop; on a phone you notice it. We added loading overlays and skeleton states to make the wait feel intentional rather than broken.

**Firestore write costs** crept up during testing when we accidentally triggered writes on every keystroke in the profile edit screen. Debouncing saves and batching related writes into single operations fixed it fast.

---

## Roadmap

- [ ] Image-based meal recognition — point camera at food, get calorie estimate (Gemini Vision)
- [ ] Google Fit / Samsung Health integration for real step and heart rate data
- [ ] Offline support with Room Database as local cache
- [ ] Weekly progress charts — weight trend, calorie consistency, workout streak
- [ ] Push notifications for meal reminders and daily goals
- [ ] Shareable progress cards for social media

---

## Project Structure at a Glance

```
fitpulse/
├── app/
│   ├── src/main/
│   │   ├── java/com/fitpulse/     # All Java source files
│   │   └── res/
│   │       ├── layout/            # XML UI layouts
│   │       ├── drawable/          # Icons, shapes, backgrounds
│   │       ├── raw/               # Lottie JSON animation files
│   │       └── values/            # Colors, strings, themes
│   └── google-services.json       # Firebase config (not committed)
├── README.md
└── .gitignore
```

---

## Contributing

Pull requests are welcome. If you find a bug or have a feature idea, open an issue first so we can discuss it before you spend time building something.

If you're adding a new AI feature, please keep the prompt construction inside `GrokHelper` rather than scattering API calls through the fragments.

---

## License

```
MIT License

Copyright (c) 2025 Vishal Kumar & Mohsin Abbas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

<div align="center">

Built with 💚 by **Vishal Kumar** & **Mohsin Abbas**
<br/>
School of Electrical Engineering & Computer Science — SEECS · 2025

<br/>

*If this helped you, a ⭐ on the repo means a lot.*

</div>
