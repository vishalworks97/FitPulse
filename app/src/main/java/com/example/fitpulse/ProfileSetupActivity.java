package com.example.fitpulse;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSetupActivity";
    private EditText etName, etAge, etHeight, etWeight, etCountry;
    private RadioGroup rgGender, rgDiet;
    private ProgressDialog progressDialog;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        preferenceManager = new PreferenceManager(this);

        // Initialize Views
        etName = findViewById(R.id.et_name);
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etCountry = findViewById(R.id.et_country);
        rgGender = findViewById(R.id.rg_gender);
        rgDiet = findViewById(R.id.rg_diet);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Finalizing your profile...");
        progressDialog.setCancelable(false);

        findViewById(R.id.btn_finish_setup).setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String country = etCountry.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(ageStr) || 
            TextUtils.isEmpty(heightStr) || TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(country)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            float h = Float.parseFloat(heightStr);
            float w = Float.parseFloat(weightStr);
            
            if (age <= 0 || age > 120 || h <= 0 || w <= 0) {
                Toast.makeText(this, "Please enter realistic values", Toast.LENGTH_SHORT).show();
                return;
            }

            String gender = (rgGender.getCheckedRadioButtonId() == R.id.rb_male) ? "Male" : "Female";
            boolean isVegetarian = (rgDiet.getCheckedRadioButtonId() == R.id.rb_veg);
            
            saveToFirebase(name, age, h, w, country, isVegetarian, gender);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric values", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeSetupAndNavigate() {
        if (isFinishing() || isDestroyed()) return;
        
        Log.d(TAG, "Navigating to Main Dashboard...");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        
        // Mark profile as complete before navigating
        preferenceManager.setProfileComplete(true);
        
        Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveToFirebase(String name, int age, float h, float w, String country, boolean isVegetarian, String gender) {
        if (!isFinishing()) progressDialog.show();

        double bmr = HealthMathHelper.calculateBMR(w, h, age, gender);
        double bmi = HealthMathHelper.calculateBMI(w, h);

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("age", age);
        user.put("gender", gender);
        user.put("height", h);
        user.put("weight", w);
        user.put("country", country);
        user.put("isVegetarian", isVegetarian);
        user.put("bmi", bmi);
        user.put("status", "Active");

        // Cache locally
        preferenceManager.saveFullProfile(name, gender, w, h, age, "Active");
        preferenceManager.cacheUserData(name, bmr);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            completeSetupAndNavigate();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        completeSetupAndNavigate();
                    } else {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Toast.makeText(this, "Cloud sync failed. Navigating anyway...", Toast.LENGTH_SHORT).show();
                        completeSetupAndNavigate(); // Added fall-back to prevent infinite loading
                    }
                });

        // Forced safety timeout after 5 seconds to ensure the user never gets stuck
        etName.postDelayed(() -> {
            if (progressDialog.isShowing()) {
                Log.e(TAG, "Firebase timed out. Forcing navigation.");
                completeSetupAndNavigate();
            }
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}
