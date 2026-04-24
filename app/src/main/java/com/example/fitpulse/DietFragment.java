package com.example.fitpulse;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DietFragment extends Fragment {

    private LinearLayout llBreakfast, llLunch, llDinner, llSnacks;
    private SwipeRefreshLayout swipeRefreshDiet;
    private FirebaseFirestore db;
    private String userId;
    private String userCountry = "Local"; 
    private List<ListenerRegistration> registrations = new ArrayList<>();
    private PreferenceManager prefManager;
    private ProgressDialog progressDialog;

    private final Map<String, List<DocumentSnapshot>> persistedCache = new HashMap<>();
    private final Map<String, List<String>> eatenCache = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_diet_fragment, container, false);

        prefManager = new PreferenceManager(requireContext());
        llBreakfast = root.findViewById(R.id.llBreakfastItems);
        llLunch = root.findViewById(R.id.llLunchItems);
        llDinner = root.findViewById(R.id.llDinnerItems);
        llSnacks = root.findViewById(R.id.llSnacksItems);
        swipeRefreshDiet = root.findViewById(R.id.swipeRefreshDiet);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        progressDialog = new ProgressDialog(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT);
        progressDialog.setMessage("Consulting AI Health Coach...");
        progressDialog.setCancelable(false);

        fetchUserCountry();
        setupButtons(root);

        swipeRefreshDiet.setOnRefreshListener(() -> {
            loadPersistedFoods();
            swipeRefreshDiet.setRefreshing(false);
        });
        swipeRefreshDiet.setColorSchemeColors(Color.parseColor("#00FF41"));

        loadPersistedFoods();
        return root;
    }

    private void setupButtons(View root) {
        root.findViewById(R.id.btnAddBreakfast).setOnClickListener(v -> showAddOptions("breakfast"));
        root.findViewById(R.id.btnAddLunch).setOnClickListener(v -> showAddOptions("lunch"));
        root.findViewById(R.id.btnAddDinner).setOnClickListener(v -> showAddOptions("dinner"));
        root.findViewById(R.id.btnAddSnacks).setOnClickListener(v -> showAddOptions("snacks"));

        root.findViewById(R.id.btnRecommendBreakfast).setOnClickListener(v -> requestAutoRecommendation("breakfast"));
        root.findViewById(R.id.btnRecommendLunch).setOnClickListener(v -> requestAutoRecommendation("lunch"));
        root.findViewById(R.id.btnRecommendDinner).setOnClickListener(v -> requestAutoRecommendation("dinner"));
        root.findViewById(R.id.btnRecommendSnacks).setOnClickListener(v -> requestAutoRecommendation("snacks"));
    }

    private void requestAutoRecommendation(String mealType) {
        // Updated logic: Non-Veg users can eat anything, so they get general healthy recommendations.
        // Veg users get strictly Vegetarian recommendations.
        String dietType = prefManager.isVegetarian() ? "Vegetarian" : "All (Veg & Non-Veg)";
        getAiRecommendation(mealType, userCountry, dietType);
    }

    private void fetchUserCountry() {
        if (userId == null) return;
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                userCountry = doc.getString("country");
                if (userCountry == null || userCountry.isEmpty()) userCountry = "Local";
                Boolean isVeg = doc.getBoolean("isVegetarian");
                if (isVeg != null) prefManager.setVegetarian(isVeg);
            }
        });
    }

    private void showAddOptions(String mealType) {
        String[] options = {"✨ AI Smart Search", "📝 Manual Entry"};
        showStyledListDialog("Plan " + mealType.toUpperCase(), options, (dialog, which) -> {
            if (which == 0) showSearchDialog(mealType);
            else if (which == 1) showManualEntry(mealType, "");
        });
    }

    private void getAiRecommendation(String mealType, String country, String dietType) {
        progressDialog.show();
        GrokHelper.getMealRecommendations(country, dietType, mealType, new GrokHelper.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        String[] lines = response.split("\n");
                        List<String> validItems = new ArrayList<>();
                        for (String line : lines) {
                            if (line.contains(":") || line.toLowerCase().contains("kcal")) {
                                validItems.add(line.replace("*", "").trim());
                            }
                        }

                        if (validItems.isEmpty()) {
                            Toast.makeText(getContext(), "AI is updating. Try again later.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] itemsArray = validItems.toArray(new String[0]);
                        showStyledListDialog(country + " Cuisine Ideas", itemsArray, (dialog, which) -> {
                            try {
                                String selected = itemsArray[which];
                                String name = selected.contains(":") ? selected.split(":")[0].trim() : selected.replaceAll("\\d+.*", "").trim();
                                name = name.replaceAll("^\\d+[.\\s]+", "");
                                int kcal = parseAveragedCalories(selected);
                                saveToPersistedList(mealType, name, kcal);
                                Toast.makeText(getContext(), "Planned: " + name, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Selection error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            }
            @Override public void onError(String error) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> { progressDialog.dismiss(); Toast.makeText(getContext(), "AI Sync Failed", Toast.LENGTH_SHORT).show(); });
                }
            }
        });
    }

    private int parseAveragedCalories(String text) {
        Matcher m = Pattern.compile("\\d+").matcher(text);
        int maxNum = 0;
        while (m.find()) {
            int n = Integer.parseInt(m.group());
            if (n > maxNum && n < 3000 && n > 5) maxNum = n;
        }
        return maxNum > 0 ? maxNum : 200;
    }

    private void saveToPersistedList(String mealType, String food, int kcal) {
        if (userId == null) return;
        Map<String, Object> item = new HashMap<>();
        item.put("name", food);
        item.put("calories", kcal);
        item.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .collection("persisted_diet").document(mealType)
                .collection("items").add(item);
    }

    private void loadPersistedFoods() {
        for (ListenerRegistration reg : registrations) reg.remove();
        registrations.clear();
        persistedCache.clear();
        eatenCache.clear();

        setupMealListeners("breakfast", llBreakfast);
        setupMealListeners("lunch", llLunch);
        setupMealListeners("dinner", llDinner);
        setupMealListeners("snacks", llSnacks);
    }

    private void setupMealListeners(String type, LinearLayout container) {
        if (userId == null) return;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        persistedCache.put(type, new ArrayList<>());
        eatenCache.put(type, new ArrayList<>());

        registrations.add(db.collection("users").document(userId)
                .collection("persisted_diet").document(type)
                .collection("items").orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null && isAdded()) {
                        persistedCache.put(type, value.getDocuments());
                        getActivity().runOnUiThread(() -> renderMealUI(type, container, today));
                    }
                }));

        registrations.add(db.collection("users").document(userId)
                .collection("daily_logs").document(today)
                .collection(type)
                .addSnapshotListener((value, error) -> {
                    if (value != null && isAdded()) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot d : value) ids.add(d.getId());
                        eatenCache.put(type, ids);
                        getActivity().runOnUiThread(() -> renderMealUI(type, container, today));
                    }
                }));
    }

    private void renderMealUI(String type, LinearLayout container, String today) {
        if (!isAdded()) return;
        
        List<DocumentSnapshot> persisted = persistedCache.get(type);
        List<String> eaten = eatenCache.get(type);

        if (persisted == null) return; 

        container.removeAllViews();
        if (persisted.isEmpty()) {
            addEmptyState(container);
        } else {
            for (DocumentSnapshot doc : persisted) {
                String foodId = doc.getId();
                String name = doc.getString("name");
                Long kcalLong = doc.getLong("calories");
                int kcal = (kcalLong != null) ? kcalLong.intValue() : 0;
                boolean isEaten = (eaten != null && eaten.contains(foodId));
                container.addView(createProfessionalRow(type, foodId, name, kcal, isEaten, today));
            }
        }
    }

    private void addEmptyState(LinearLayout container) {
        TextView tv = new TextView(getContext());
        tv.setText("No items planned yet");
        tv.setTextColor(Color.parseColor("#80FFFFFF"));
        tv.setPadding(0, 40, 0, 40);
        tv.setGravity(android.view.Gravity.CENTER);
        container.addView(tv);
    }

    private View createProfessionalRow(String type, String foodId, String name, int kcal, boolean isEaten, String date) {
        CardView card = new CardView(requireContext());
        card.setCardBackgroundColor(Color.parseColor("#1A1A1A"));
        card.setRadius(16f);
        card.setCardElevation(0f);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 10, 0, 10);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(30, 30, 30, 30);

        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(name);
        tv.setTextSize(17f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(isEaten ? Color.parseColor("#A0FFFFFF") : Color.WHITE);
        row.addView(tv);

        TextView tvKcal = new TextView(getContext());
        tvKcal.setText(kcal + " kcal");
        tvKcal.setTextSize(14f);
        tvKcal.setTypeface(null, Typeface.BOLD);
        tvKcal.setTextColor(Color.parseColor("#00FF41"));
        tvKcal.setPadding(20, 0, 30, 0);
        row.addView(tvKcal);

        TextView btnEat = new TextView(getContext());
        btnEat.setText(isEaten ? "EATEN" : "EAT");
        btnEat.setBackgroundResource(R.drawable.bg_glow_green);
        if (isEaten) btnEat.setAlpha(0.6f);
        btnEat.setTextColor(Color.BLACK);
        btnEat.setPadding(35, 12, 35, 12);
        btnEat.setTextSize(12f);
        btnEat.setTypeface(null, Typeface.BOLD);
        btnEat.setOnClickListener(v -> {
            List<String> eatenIds = eatenCache.get(type);
            boolean alreadyHadMeal = eatenIds != null && !eatenIds.isEmpty();

            if (isEaten) {
                showStyledConfirmDialog("Already Eaten", "You logged this specific item earlier. Eat it again?", 
                        () -> markAsEaten(type, foodId, name, kcal, date));
            } else if (alreadyHadMeal) {
                String mealLabel = type.substring(0, 1).toUpperCase() + type.substring(1);
                showStyledConfirmDialog("Meal Already Logged", "You already did " + mealLabel + ". Want to do again?",
                        () -> markAsEaten(type, foodId, name, kcal, date));
            } else {
                markAsEaten(type, foodId, name, kcal, date);
            }
        });
        row.addView(btnEat);

        ImageView btnDelete = new ImageView(getContext());
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setColorFilter(Color.parseColor("#40FF5252"));
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
        btnDelete.setPadding(20, 0, 0, 0);
        btnDelete.setOnClickListener(v -> {
            showStyledConfirmDialog("Remove Item", "Do you want to remove '" + name + "' from your plan?", () -> {
                db.collection("users").document(userId).collection("persisted_diet").document(type).collection("items").document(foodId).delete();
                db.collection("users").document(userId).collection("daily_logs").document(date).collection(type).document(foodId).delete();
                Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
            });
        });
        row.addView(btnDelete);

        card.addView(row);
        return card;
    }

    private void markAsEaten(String type, String foodId, String name, int kcal, String date) {
        if (userId == null) return;
        Map<String, Object> log = new HashMap<>();
        log.put("name", name); log.put("calories", kcal); log.put("isEaten", true); log.put("timestamp", FieldValue.serverTimestamp());
        
        db.collection("users").document(userId).collection("daily_logs").document(date).collection(type).document(foodId).set(log);
        db.collection("users").document(userId).collection("daily_logs").document(date).set(new HashMap<String, Object>() {{
            put("caloriesIntake", FieldValue.increment(kcal));
        }}, SetOptions.merge());
                
        Toast.makeText(getContext(), name + " Logged!", Toast.LENGTH_SHORT).show();
    }

    private void showSearchDialog(String mealType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT);
        builder.setTitle("AI Nutrition Search");
        final EditText input = new EditText(getContext());
        input.setHint("e.g. 2 Scrambled Eggs");
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);
        builder.setPositiveButton("SEARCH", (dialog, which) -> {
            String food = input.getText().toString().trim();
            if (!food.isEmpty()) fetchCaloriesAndConfirm(mealType, food);
        });
        builder.setNegativeButton("CANCEL", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    private void fetchCaloriesAndConfirm(String mealType, String foodName) {
        progressDialog.show();
        String prompt = "Return ONLY the estimated calorie count for: " + foodName + ". Format: [number] kcal.";
        GrokHelper.askAIWithInstruction("You are a nutritional database. Return only numeric value + 'kcal'.", prompt, new GrokHelper.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        int kcal = parseAveragedCalories(response);
                        if (kcal > 0) {
                            saveToPersistedList(mealType, foodName, kcal);
                            Toast.makeText(getContext(), "Estimated: " + kcal + " kcal", Toast.LENGTH_SHORT).show();
                        } else showManualEntry(mealType, foodName);
                    });
                }
            }
            @Override public void onError(String error) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> { progressDialog.dismiss(); showManualEntry(mealType, foodName); });
                }
            }
        });
    }

    private void showManualEntry(String mealType, String foodName) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 0);

        final EditText etFoodName = new EditText(getContext());
        etFoodName.setHint("Food Name"); etFoodName.setText(foodName);
        layout.addView(etFoodName);

        final EditText etCalories = new EditText(getContext());
        etCalories.setHint("Calories (kcal)"); etCalories.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etCalories);

        AlertDialog dialog = new AlertDialog.Builder(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("Manual Entry")
                .setView(layout)
                .setPositiveButton("ADD", (d, w) -> {
                    String name = etFoodName.getText().toString().trim();
                    String kcalStr = etCalories.getText().toString().trim();
                    if (!name.isEmpty() && !kcalStr.isEmpty()) saveToPersistedList(mealType, name, Integer.parseInt(kcalStr));
                })
                .setNegativeButton("CANCEL", null).create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    private void showStyledConfirmDialog(String title, String message, Runnable onConfirm) {
        AlertDialog dialog = new AlertDialog.Builder(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("YES", (d, w) -> onConfirm.run())
                .setNegativeButton("NO", null)
                .create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    private void showStyledListDialog(String title, String[] items, DialogInterface.OnClickListener listener) {
        AlertDialog dialog = new AlertDialog.Builder(getContext(), android.app.AlertDialog.THEME_HOLO_LIGHT)
                .setTitle(title)
                .setItems(items, listener)
                .create();
        dialog.show();
    }

    private void styleDialogButtons(AlertDialog dialog) {
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (pos != null) {
            pos.setTextColor(Color.parseColor("#00FF41"));
            pos.setTypeface(null, Typeface.BOLD);
        }
        if (neg != null) {
            neg.setTextColor(Color.parseColor("#00FF41"));
            neg.setTypeface(null, Typeface.BOLD);
        }
    }

    @Override
    public void onStop() { super.onStop(); for (ListenerRegistration reg : registrations) reg.remove(); registrations.clear(); }
}
