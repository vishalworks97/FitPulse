package com.example.fitpulse;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutView, layoutEdit;
    private TextView tvDisplayName, tvDisplayWeight, tvDisplayAge, tvDisplayBMI, tvDisplayGender, tvDisplayStatus, tvDisplayDiet;
    private EditText etName, etWeight, etHeight, etStatus, etAge;
    private Spinner spinnerGender;
    private RadioGroup rgDiet;
    private RadioButton rbVeg, rbNonVeg;
    
    private SwitchCompat switchNotifications;
    private TextView tvBreakfastTime, tvLunchTime, tvDinnerTime;
    
    private PreferenceManager prefManager;
    private FirebaseFirestore db;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        prefManager = new PreferenceManager(requireContext());
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        // Find Views
        layoutView = view.findViewById(R.id.layoutViewMode);
        layoutEdit = view.findViewById(R.id.layoutEditMode);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvDisplayWeight = view.findViewById(R.id.tvDisplayWeight);
        tvDisplayBMI = view.findViewById(R.id.tvDisplayBMI);
        tvDisplayGender = view.findViewById(R.id.tvDisplayGender);
        tvDisplayStatus = view.findViewById(R.id.tvDisplayStatus);
        tvDisplayAge = view.findViewById(R.id.tvDisplayAge);
        tvDisplayDiet = view.findViewById(R.id.tvDisplayDiet);
        
        etName = view.findViewById(R.id.etName);
        etStatus = view.findViewById(R.id.etStatus);
        etAge = view.findViewById(R.id.etAge);
        etWeight = view.findViewById(R.id.etWeight);
        etHeight = view.findViewById(R.id.etHeight);
        spinnerGender = view.findViewById(R.id.spinnerGender);
        
        // Find Diet Views in Edit Mode (Make sure they exist in XML)
        rgDiet = view.findViewById(R.id.rg_diet_profile);
        rbVeg = view.findViewById(R.id.rb_veg_profile);
        rbNonVeg = view.findViewById(R.id.rb_nonveg_profile);

        loadDashboardData();
        syncFromFirebase();

        // Open Settings
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            preFillEditFields();
            layoutView.setVisibility(View.GONE);
            layoutEdit.setVisibility(View.VISIBLE);
        });

        // Save and Sync
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveUpdatedData();
            loadDashboardData();
            layoutEdit.setVisibility(View.GONE);
            layoutView.setVisibility(View.VISIBLE);
        });

        // Cancel Edit
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            layoutEdit.setVisibility(View.GONE);
            layoutView.setVisibility(View.VISIBLE);
        });

        // View History Button
        view.findViewById(R.id.btnViewHistory).setOnClickListener(v -> showDatePicker());

        // System Info Button
        view.findViewById(R.id.btnAboutApp).setOnClickListener(v -> showAboutApp());

        // SIGN OUT LOGIC
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to end your session?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        // FIX: Clear local preferences on sign out to handle multiple accounts correctly
                        prefManager.clearAll();
                        FirebaseAuth.getInstance().signOut();
                        if (getActivity() != null) {
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
            fetchAndShowHistory(dateStr);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fetchAndShowHistory(String date) {
        if (userId == null) return;
        
        Toast.makeText(getContext(), "Fetching records for " + date, Toast.LENGTH_SHORT).show();
        
        db.collection("users").document(userId)
                .collection("daily_logs").document(date)
                .get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showNoDataDialog(date);
                        return;
                    }

                    long intake = doc.contains("caloriesIntake") ? doc.getLong("caloriesIntake") : 0;
                    long burnt = doc.contains("caloriesBurnt") ? doc.getLong("caloriesBurnt") : 0;
                    long steps = doc.contains("stepsCount") ? doc.getLong("stepsCount") : 0;

                    // Fetch details from sub-collections for a richer history view
                    Task<QuerySnapshot> t1 = db.collection("users").document(userId).collection("daily_logs").document(date).collection("breakfast").get();
                    Task<QuerySnapshot> t2 = db.collection("users").document(userId).collection("daily_logs").document(date).collection("lunch").get();
                    Task<QuerySnapshot> t3 = db.collection("users").document(userId).collection("daily_logs").document(date).collection("dinner").get();
                    Task<QuerySnapshot> t4 = db.collection("users").document(userId).collection("daily_logs").document(date).collection("snacks").get();
                    Task<QuerySnapshot> t5 = db.collection("users").document(userId).collection("daily_logs").document(date).collection("workouts").get();

                    Tasks.whenAllSuccess(t1, t2, t3, t4, t5).addOnSuccessListener(results -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📅 DATE: ").append(date).append("\n\n");
                        sb.append("🔥 SUMMARY:\n");
                        sb.append("• Intake: ").append(intake).append(" kcal\n");
                        sb.append("• Burned: ").append(burnt).append(" kcal\n");
                        sb.append("• Steps: ").append(steps).append("\n\n");

                        sb.append("🥗 MEALS:\n");
                        appendMeals(sb, "Breakfast", (QuerySnapshot) results.get(0));
                        appendMeals(sb, "Lunch", (QuerySnapshot) results.get(1));
                        appendMeals(sb, "Dinner", (QuerySnapshot) results.get(2));
                        appendMeals(sb, "Snacks", (QuerySnapshot) results.get(3));
                        
                        sb.append("\n🏃 WORKOUTS:\n");
                        QuerySnapshot workouts = (QuerySnapshot) results.get(4);
                        if (workouts.isEmpty()) sb.append("None logged\n");
                        else {
                            for (DocumentSnapshot d : workouts) {
                                sb.append("• ").append(d.getString("name")).append(" (").append(d.get("calories")).append(" kcal)\n");
                            }
                        }

                        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle("History Record")
                                .setMessage(sb.toString())
                                .setPositiveButton("Close", null)
                                .show();
                    });
                });
    }

    private void appendMeals(StringBuilder sb, String title, QuerySnapshot snapshots) {
        sb.append("[").append(title).append("]: ");
        if (snapshots.isEmpty()) sb.append("None\n");
        else {
            List<String> items = new ArrayList<>();
            for (DocumentSnapshot d : snapshots) {
                if (Boolean.TRUE.equals(d.getBoolean("isEaten"))) {
                    items.add(d.getString("name"));
                }
            }
            if (items.isEmpty()) sb.append("Planned but not eaten\n");
            else sb.append(String.join(", ", items)).append("\n");
        }
    }

    private void showNoDataDialog(String date) {
        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("No Data")
                .setMessage("No records found for " + date)
                .setPositiveButton("OK", null)
                .show();
    }

    private void syncFromFirebase() {
        if (userId == null) return;
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String name = doc.getString("name");
                String gender = doc.getString("gender");
                Double w = doc.getDouble("weight");
                Double h = doc.getDouble("height");
                Long a = doc.getLong("age");
                String status = doc.getString("status");
                Boolean isVeg = doc.getBoolean("isVegetarian");

                prefManager.saveFullProfile(
                        name != null ? name : "Athlete",
                        gender != null ? gender : "Male",
                        w != null ? w.floatValue() : 70f,
                        h != null ? h.floatValue() : 170f,
                        a != null ? a.intValue() : 25,
                        status != null ? status : "Active"
                );
                prefManager.setVegetarian(isVeg != null && isVeg);
                loadDashboardData();
            }
        });
    }

    private void loadDashboardData() {
        tvDisplayName.setText(prefManager.getUserName());
        tvDisplayStatus.setText("Status: " + prefManager.getUserStatus());
        tvDisplayWeight.setText(prefManager.getUserWeight() + " kg");
        tvDisplayGender.setText(prefManager.getUserGender());
        tvDisplayAge.setText(String.valueOf(prefManager.getUserAge()));
        
        if (tvDisplayDiet != null) {
            tvDisplayDiet.setText(prefManager.isVegetarian() ? "Diet: Vegetarian" : "Diet: Non-Vegetarian");
        }

        float w = prefManager.getUserWeight();
        float h = prefManager.getUserHeight() / 100.0f;
        if (h > 0) {
            float bmi = w / (h * h);
            tvDisplayBMI.setText(String.format(Locale.getDefault(), "%.1f", bmi));
        } else {
            tvDisplayBMI.setText("--");
        }
    }

    private void preFillEditFields() {
        etName.setText(prefManager.getUserName());
        etStatus.setText(prefManager.getUserStatus());
        etAge.setText(String.valueOf(prefManager.getUserAge()));
        etWeight.setText(String.valueOf(prefManager.getUserWeight()));
        etHeight.setText(String.valueOf(prefManager.getUserHeight()));
        
        // Set spinner selection based on stored gender
        String gender = prefManager.getUserGender();
        if ("Female".equalsIgnoreCase(gender)) spinnerGender.setSelection(1);
        else spinnerGender.setSelection(0);
        
        // Set diet selection
        if (rgDiet != null) {
            if (prefManager.isVegetarian()) rgDiet.check(R.id.rb_veg_profile);
            else rgDiet.check(R.id.rb_nonveg_profile);
        }
        
        setupNotificationSettings();
    }
    
    private void setupNotificationSettings() {
        View editRoot = layoutEdit;
        if (editRoot == null) return;
        
        switchNotifications = editRoot.findViewById(R.id.switchNotifications);
        tvBreakfastTime = editRoot.findViewById(R.id.tvBreakfastTime);
        tvLunchTime = editRoot.findViewById(R.id.tvLunchTime);
        tvDinnerTime = editRoot.findViewById(R.id.tvDinnerTime);
        
        if (switchNotifications == null) return;
        
        switchNotifications.setChecked(prefManager.isNotificationsEnabled());
        tvBreakfastTime.setText(prefManager.getBreakfastTime());
        tvLunchTime.setText(prefManager.getLunchTime());
        tvDinnerTime.setText(prefManager.getDinnerTime());
        
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setNotificationsEnabled(isChecked);
            if (isChecked) AlarmHelper.scheduleAllReminders(requireContext());
            else AlarmHelper.cancelAllReminders(requireContext());
        });
        
        tvBreakfastTime.setOnClickListener(v -> showTimePicker("breakfast", tvBreakfastTime));
        tvLunchTime.setOnClickListener(v -> showTimePicker("lunch", tvLunchTime));
        tvDinnerTime.setOnClickListener(v -> showTimePicker("dinner", tvDinnerTime));
    }
    
    private void showTimePicker(String type, TextView target) {
        String currentTime = target.getText().toString();
        int hour = Integer.parseInt(currentTime.split(":")[0]);
        int minute = Integer.parseInt(currentTime.split(":")[1]);
        
        new TimePickerDialog(getContext(), (view, hourOfDay, min) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, min);
            target.setText(selectedTime);
            if ("breakfast".equals(type)) prefManager.setBreakfastTime(selectedTime);
            else if ("lunch".equals(type)) prefManager.setLunchTime(selectedTime);
            else if ("dinner".equals(type)) prefManager.setDinnerTime(selectedTime);
            
            if (prefManager.isNotificationsEnabled()) AlarmHelper.scheduleAllReminders(requireContext());
        }, hour, minute, true).show();
    }

    private void saveUpdatedData() {
        String name = etName.getText().toString();
        String status = etStatus.getText().toString();
        int age = Integer.parseInt(etAge.getText().toString());
        float w = Float.parseFloat(etWeight.getText().toString());
        float h = Float.parseFloat(etHeight.getText().toString());
        String gender = spinnerGender.getSelectedItem().toString();
        
        boolean isVeg = false;
        if (rgDiet != null) {
            isVeg = rgDiet.getCheckedRadioButtonId() == R.id.rb_veg_profile;
        }

        prefManager.saveFullProfile(name, gender, w, h, age, status);
        prefManager.setVegetarian(isVeg);
        
        // Sync to Firebase
        if (userId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("status", status);
            updates.put("age", age);
            updates.put("weight", w);
            updates.put("height", h);
            updates.put("gender", gender);
            updates.put("isVegetarian", isVeg);
            double bmi = w / ((h / 100) * (h / 100));
            updates.put("bmi", Math.round(bmi * 10.0) / 10.0);
            
            db.collection("users").document(userId).update(updates);
        }

        Toast.makeText(getContext(), "Metrics Synced Successfully", Toast.LENGTH_SHORT).show();
    }

    private void showAboutApp() {
        String info = "FitPulse Ecosystem v1.2.0\n\nDeveloped for peak performance and biometric analysis.\n\n" +
                "TEAM:\n• VISHAL KUMAR\n• MOHSIN ABBAS";
        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("System Information")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }
}
