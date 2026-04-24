package com.example.fitpulse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext;
    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.onboarding_viewpager);
        btnNext = findViewById(R.id.btn_next);

        setupOnboardingItems();

        new TabLayoutMediator(findViewById(R.id.tab_indicator), viewPager,
                (tab, position) -> {}).attach();

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        findViewById(R.id.tv_skip).setOnClickListener(v -> completeOnboarding());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem("Track Fitness", "Advanced tracking for steps...", R.raw.anim_workout));
        items.add(new OnboardingItem("AI-Powered Diet", "Personalized nutrition plans...", R.raw.anim_diet));
        items.add(new OnboardingItem("Visual Analytics", "Deep dive into your progress...", R.raw.anim_charts));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);
    }

    private void completeOnboarding() {
        startActivity(new Intent(this, ProfileSetupActivity.class));
        finish();
    }
}
