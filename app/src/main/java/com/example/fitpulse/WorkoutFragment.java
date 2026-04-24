package com.example.fitpulse;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkoutFragment extends Fragment {

    private Chronometer chronometer;
    private TextView tvLiveCalories, tvActiveName;
    private ImageView ivWorkoutGif;
    private View cardActive;
    private Button btnStart, btnPause, btnResume, btnSave;
    private View layoutControls;
    
    private double currentMet = 0;
    private double userWeight = 70.0;
    private Workout selectedWorkout;
    
    private boolean isPaused = false;
    private long timeWhenPaused = 0;
    private double totalBurnedCached = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        chronometer = view.findViewById(R.id.workoutTimer);
        tvLiveCalories = view.findViewById(R.id.tvLiveCalories);
        tvActiveName = view.findViewById(R.id.tvActiveExerciseName);
        ivWorkoutGif = view.findViewById(R.id.ivWorkoutGif);
        cardActive = view.findViewById(R.id.cardActiveSession);
        btnStart = view.findViewById(R.id.btnStartWorkout);
        
        layoutControls = view.findViewById(R.id.llActiveControls);
        btnPause = view.findViewById(R.id.btnPauseWorkout);
        btnResume = view.findViewById(R.id.btnResumeWorkout);
        btnSave = view.findViewById(R.id.btnStopWorkout);

        setupRecyclerView(view);
        fetchUserWeight();

        btnStart.setOnClickListener(v -> startTimer());
        btnPause.setOnClickListener(v -> pauseTimer());
        btnResume.setOnClickListener(v -> resumeTimer());
        btnSave.setOnClickListener(v -> stopAndSaveWorkout());

        return view;
    }

    private void prepareWorkout(Workout workout) {
        selectedWorkout = workout;
        currentMet = workout.getMetValue();
        tvActiveName.setText(workout.getName());
        cardActive.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.GONE);
        
        tvLiveCalories.setText("0.0 kcal");
        chronometer.setBase(SystemClock.elapsedRealtime());
        timeWhenPaused = 0;
        totalBurnedCached = 0;
        isPaused = false;
        
        Glide.with(this)
                .asGif()
                .load(workout.getGifResource())
                .into(ivWorkoutGif);
    }

    private void startTimer() {
        if (selectedWorkout == null) return;
        
        btnStart.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        btnPause.setVisibility(View.VISIBLE);
        btnResume.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        chronometer.setOnChronometerTickListener(c -> {
            updateLiveCalories();
        });
    }

    private void pauseTimer() {
        if (!isPaused) {
            timeWhenPaused = chronometer.getBase() - SystemClock.elapsedRealtime();
            chronometer.stop();
            isPaused = true;
            
            btnPause.setVisibility(View.GONE);
            btnResume.setVisibility(View.VISIBLE);
            updateLiveCalories();
        }
    }

    private void resumeTimer() {
        if (isPaused) {
            chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            chronometer.start();
            isPaused = false;
            
            btnPause.setVisibility(View.VISIBLE);
            btnResume.setVisibility(View.GONE);
        }
    }

    private void updateLiveCalories() {
        long elapsedMillis;
        if (isPaused) {
            elapsedMillis = Math.abs(timeWhenPaused);
        } else {
            elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        }
        
        double minutes = elapsedMillis / 60000.0;
        double burned = (currentMet * 3.5 * userWeight / 200) * minutes;
        totalBurnedCached = burned;
        tvLiveCalories.setText(String.format(Locale.getDefault(), "%.1f kcal", burned));
    }

    private void stopAndSaveWorkout() {
        chronometer.stop();
        updateLiveCalories();
        int finalBurned = (int) totalBurnedCached;

        saveToFirebase(selectedWorkout.getName(), finalBurned);
        cardActive.setVisibility(View.GONE);
        Glide.with(this).clear(ivWorkoutGif);
        selectedWorkout = null;
    }

    private void saveToFirebase(String workoutName, int kcal) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> workoutLog = new HashMap<>();
        workoutLog.put("name", workoutName);
        workoutLog.put("calories", kcal);
        workoutLog.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .collection("daily_logs").document(today)
                .collection("workouts").add(workoutLog);

        db.collection("users").document(uid)
                .collection("daily_logs").document(today)
                .set(new HashMap<String, Object>() {{
                    put("caloriesBurnt", FieldValue.increment(kcal));
                }}, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(getContext(), "Burned " + kcal + " kcal saved!", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserWeight() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get().addOnSuccessListener(doc -> {
                    if(doc.exists() && isAdded()) {
                        Double weight = doc.getDouble("weight");
                        if (weight != null) userWeight = weight;
                    }
                });
    }

    private void setupRecyclerView(View v) {
        RecyclerView rv = v.findViewById(R.id.rvWorkouts);
        List<Workout> list = new ArrayList<>();
        list.add(new Workout("Bicycle Crunches", R.raw.bicyclecrunches, 8.0));
        list.add(new Workout("Crunch Kicks", R.raw.crunchkicks, 5.0));
        list.add(new Workout("Jumping Jacks", R.raw.jumpingjacks, 10.0));

        WorkoutAdapter adapter = new WorkoutAdapter(list, workout -> prepareWorkout(workout));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);
    }
}
