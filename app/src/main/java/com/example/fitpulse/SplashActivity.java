package com.example.fitpulse;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_splash_logo);
        View glow = findViewById(R.id.logo_glow);
        TextView name = findViewById(R.id.tv_splash_name);
        View tagline = name.getRootView().findViewWithTag("tagline"); // Fallback or use ID

        // Initial states
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        glow.setAlpha(0f);
        name.setAlpha(0f);
        name.setTranslationY(50f);

        // Professional Animation Sequence
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        glow.animate()
                .alpha(0.4f)
                .setDuration(1500)
                .setStartDelay(500)
                .start();

        name.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(1000)
                .start();

        new Handler().postDelayed(() -> {
            PreferenceManager preferenceManager = new PreferenceManager(this);
            
            boolean isFirebaseInitialized = !FirebaseApp.getApps(this).isEmpty();
            boolean isLoggedIn = false;

            if (isFirebaseInitialized) {
                isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
            }
            
            if (isLoggedIn) {
                if (!preferenceManager.isProfileComplete()) {
                    startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3500);
    }
}
