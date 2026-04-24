package com.example.fitpulse;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment dietFragment = new DietFragment();
    private final Fragment workoutFragment = new WorkoutFragment();
    private final Fragment profileFragment = new ProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = homeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this); // Professional Edge-to-Edge support
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle System Insets (Status bar/Notch/Navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Only padding Top for notch
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom); // Ensure Bottom Nav isn't cut off
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Add fragments and hide inactive ones
        if (fm.findFragmentByTag("1") == null) {
            fm.beginTransaction().add(R.id.fragment_container, profileFragment, "4").hide(profileFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, workoutFragment, "3").hide(workoutFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, dietFragment, "2").hide(dietFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fm.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                activeFragment = homeFragment;
                return true;
            } else if (id == R.id.nav_diet) {
                fm.beginTransaction().hide(activeFragment).show(dietFragment).commit();
                activeFragment = dietFragment;
                return true;
            } else if (id == R.id.nav_workout) {
                fm.beginTransaction().hide(activeFragment).show(workoutFragment).commit();
                activeFragment = workoutFragment;
                return true;
            } else if (id == R.id.nav_profile) {
                fm.beginTransaction().hide(activeFragment).show(profileFragment).commit();
                activeFragment = profileFragment;
                return true;
            }
            return false;
        });
    }
}