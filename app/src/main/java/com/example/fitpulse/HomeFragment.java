package com.example.fitpulse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment implements SensorEventListener {

    private TextView tvGreeting, tvSteps, tvCalorieStatus, tvAiTip, tvIntakeInfo, tvBurntInfo, tvStepCalories;
    private TextView tvNoWorkouts, tvNoMeals;
    private ProgressBar stepProgress;
    private PieChart intakePieChart, burntPieChart;
    private LinearLayout llWorkoutItems, llDietItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FirebaseFirestore db;
    private PreferenceManager prefManager;

    private final double stepCalorieFactor = 0.04;
    private List<ListenerRegistration> registrations = new ArrayList<>();
    private final Map<String, List<ActivityItem>> dietItemsMap = new HashMap<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) setupStepSensor();
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_home_fragment, container, false);

        tvGreeting = root.findViewById(R.id.tvGreeting);
        tvSteps = root.findViewById(R.id.tvSteps);
        tvCalorieStatus = root.findViewById(R.id.tvCalorieStatus);
        tvAiTip = root.findViewById(R.id.tvAiTip);
        tvIntakeInfo = root.findViewById(R.id.tvIntakeInfo);
        tvBurntInfo = root.findViewById(R.id.tvBurntInfo);
        stepProgress = root.findViewById(R.id.stepProgress);
        intakePieChart = root.findViewById(R.id.intakePieChart);
        burntPieChart = root.findViewById(R.id.burntPieChart);
        llWorkoutItems = root.findViewById(R.id.llWorkoutItems);
        llDietItems = root.findViewById(R.id.llDietItems);
        tvNoWorkouts = root.findViewById(R.id.tvNoWorkouts);
        tvNoMeals = root.findViewById(R.id.tvNoMeals);
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        tvStepCalories = root.findViewById(R.id.tvStepCalories);

        db = FirebaseFirestore.getInstance();
        prefManager = new PreferenceManager(requireContext());

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#00FF41"));

        loadUserDataAndStats();
        checkPermissionAndSetupSensor();

        return root;
    }

    private void updateUI() {
        if (!isAdded()) return;

        updateGreeting(prefManager.getUserName());
        int currentSteps = prefManager.getSteps();
        tvSteps.setText(String.valueOf(currentSteps));

        double burnedBySteps = currentSteps * stepCalorieFactor;
        tvStepCalories.setText(String.format(Locale.getDefault(), "%.1f kcal burned", burnedBySteps));
        stepProgress.setProgress(currentSteps);
        
        int eaten = Math.round(prefManager.getIntake());
        int bmrGoal = Math.round(prefManager.getUserBmr());

        ArrayList<PieEntry> intakeEntries = new ArrayList<>();
        if (eaten > bmrGoal) {
            intakeEntries.add(new PieEntry(eaten, ""));
            setupPieChart(intakePieChart, intakeEntries, eaten + "\nkcal\nLimit!", "#FF5252");
        } else {
            int intakeRemaining = Math.max(0, bmrGoal - eaten);
            intakeEntries.add(new PieEntry(eaten, ""));
            intakeEntries.add(new PieEntry(intakeRemaining, ""));
            setupPieChart(intakePieChart, intakeEntries, eaten + "\nkcal", "#00FF41");
        }
        tvIntakeInfo.setText("Eaten: " + eaten + " / Budget: " + bmrGoal);

        float totalBurnt = prefManager.getWorkoutBurnt() + (float) (currentSteps * stepCalorieFactor);
        float activeBurnGoal = 500f;
        float burnRemaining = Math.max(0, activeBurnGoal - totalBurnt);

        ArrayList<PieEntry> burntEntries = new ArrayList<>();
        burntEntries.add(new PieEntry(totalBurnt, ""));
        burntEntries.add(new PieEntry(burnRemaining, ""));
        setupPieChart(burntPieChart, burntEntries, (int) totalBurnt + "\nBurnt", "#40C4FF");
        tvBurntInfo.setText("Activity: " + (int) totalBurnt + " / Target: " + (int) activeBurnGoal);

        if (eaten > bmrGoal) {
            tvCalorieStatus.setText("⚠️ Calorie Limit Exceeded");
            tvCalorieStatus.setTextColor(Color.parseColor("#FF5252"));
        } else {
            tvCalorieStatus.setText("Fit and Active!");
            tvCalorieStatus.setTextColor(Color.parseColor("#00FF41"));
        }
    }

    private void setupPieChart(PieChart chart, ArrayList<PieEntry> entries, String centerText, String mainColor) {
        if (chart == null) return;
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor(mainColor), Color.parseColor("#1A1A1A"));
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(2f);
        chart.setData(new PieData(dataSet));
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setHoleRadius(82f);
        chart.setCenterText(centerText);
        chart.setCenterTextColor(Color.WHITE);
        chart.setCenterTextSize(12f);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.invalidate();
    }

    private void refreshData() {
        loadUserDataAndStats();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void checkPermissionAndSetupSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            } else setupStepSensor();
        } else setupStepSensor();
    }

    private void loadUserDataAndStats() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        for (ListenerRegistration reg : registrations) reg.remove();
        registrations.clear();
        dietItemsMap.clear();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String name = doc.getString("name");
                Double w = doc.getDouble("weight"), h = doc.getDouble("height");
                Long a = doc.getLong("age"); String g = doc.getString("gender");
                double bmr = HealthMathHelper.calculateBMR(w!=null?w:70, h!=null?h:170, a!=null?a.intValue():25, g!=null?g:"Male");
                prefManager.cacheUserData(name, bmr);
                updateUI();
                getAiRecommendation(HealthMathHelper.calculateBMI(w!=null?w:70, h!=null?h:170), "Active");
            }
        });

        registrations.add(db.collection("users").document(uid).collection("daily_logs").document(today)
                .addSnapshotListener((logDoc, e) -> {
                    if (logDoc == null || !logDoc.exists() || !isAdded()) return;
                    Double intake = logDoc.getDouble("caloriesIntake");
                    Double burnt = logDoc.getDouble("caloriesBurnt");
                    prefManager.setIntake(intake != null ? intake.floatValue() : 0f);
                    prefManager.setWorkoutBurnt(burnt != null ? burnt.floatValue() : 0f);
                    updateUI();
                }));

        setupActivityListeners(uid, today);
    }

    private void setupActivityListeners(String uid, String today) {
        // Workout Listener
        registrations.add(db.collection("users").document(uid).collection("daily_logs").document(today)
                .collection("workouts").orderBy("timestamp", Query.Direction.DESCENDING).limit(10)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || !isAdded()) return;
                    List<ActivityItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        items.add(new ActivityItem(doc.getString("name"), doc.get("calories") + " kcal", "Workout", true));
                    }
                    renderCompactActivities(llWorkoutItems, tvNoWorkouts, items, R.drawable.ic_fitness, "#40C4FF");
                }));

        // Diet Listeners
        String[] meals = {"breakfast", "lunch", "snacks", "dinner"};
        for (String meal : meals) {
            registrations.add(db.collection("users").document(uid).collection("daily_logs").document(today)
                    .collection(meal).addSnapshotListener((snapshots, e) -> {
                        if (snapshots == null || !isAdded()) return;
                        List<ActivityItem> items = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            Boolean isEaten = doc.getBoolean("isEaten");
                            if (isEaten != null && isEaten) {
                                items.add(new ActivityItem(doc.getString("name"), doc.get("calories") + " kcal", meal, true));
                            }
                        }
                        dietItemsMap.put(meal, items);
                        updateDietListUI();
                    }));
        }
    }

    private void updateDietListUI() {
        if (!isAdded()) return;
        List<ActivityItem> allDietItems = new ArrayList<>();
        String[] order = {"breakfast", "lunch", "dinner", "snacks"};
        for (String key : order) {
            if (dietItemsMap.containsKey(key)) {
                allDietItems.addAll(dietItemsMap.get(key));
            }
        }
        renderCompactActivities(llDietItems, tvNoMeals, allDietItems, R.drawable.ic_restaurant, "#FFD740");
    }

    private void renderCompactActivities(LinearLayout container, TextView emptyTv, List<ActivityItem> items, int iconRes, String accentColor) {
        if (!isAdded()) return;
        getActivity().runOnUiThread(() -> {
            container.removeAllViews();
            if (items.isEmpty()) {
                emptyTv.setVisibility(View.VISIBLE);
            } else {
                emptyTv.setVisibility(View.GONE);
                for (ActivityItem item : items) {
                    View row = LayoutInflater.from(getContext()).inflate(R.layout.item_activity_compact, container, false);
                    ((ImageView)row.findViewById(R.id.ivActivityIcon)).setImageResource(iconRes);
                    ((ImageView)row.findViewById(R.id.ivActivityIcon)).setColorFilter(Color.parseColor(accentColor));
                    ((TextView)row.findViewById(R.id.tvActivityName)).setText(item.name);
                    ((TextView)row.findViewById(R.id.tvActivityDetail)).setText(item.type.toUpperCase());
                    ((TextView)row.findViewById(R.id.tvActivityValue)).setText(item.value);
                    ((TextView)row.findViewById(R.id.tvActivityValue)).setTextColor(Color.parseColor(accentColor));
                    container.addView(row);
                }
            }
        });
    }

    private static class ActivityItem {
        String name, value, type; boolean isCompleted;
        ActivityItem(String n, String v, String t, boolean c) { name=n; value=v; type=t; isCompleted=c; }
    }

    private void updateGreeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = (hour < 12) ? "Good Morning" : (hour < 17) ? "Good Afternoon" : "Good Evening";
        tvGreeting.setText(g + ( (name!=null && !name.isEmpty()) ? ", " + name + "!" : "!" ));
    }

    private void getAiRecommendation(double bmi, String status) {
        GrokHelper.getDietAdvice(bmi, status, new GrokHelper.ResponseCallback() {
            @Override public void onSuccess(String response) { if(isAdded()) getActivity().runOnUiThread(() -> tvAiTip.setText(response)); }
            @Override public void onError(String error) {}
        });
    }

    private void setupStepSensor() {
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int total = (int) event.values[0];
            if (prefManager.shouldResetSteps() || total < prefManager.getStepsOffset()) {
                prefManager.saveStepsOffset(total); prefManager.updateStepResetDate();
            }
            int steps = Math.max(0, total - prefManager.getStepsOffset());
            prefManager.setSteps(steps); updateUI();
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                Map<String, Object> data = new HashMap<>(); data.put("stepsCount", steps);
                db.collection("users").document(uid).collection("daily_logs").document(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())).set(data, SetOptions.merge());
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onStop() { super.onStop(); for (ListenerRegistration reg : registrations) reg.remove(); }
    @Override public void onDestroy() { super.onDestroy(); if (sensorManager != null) sensorManager.unregisterListener(this); }
}
