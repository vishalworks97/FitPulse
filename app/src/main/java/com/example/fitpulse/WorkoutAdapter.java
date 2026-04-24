package com.example.fitpulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private final List<Workout> workouts;
    private final OnWorkoutClickListener listener;

    public interface OnWorkoutClickListener {
        void onWorkoutClick(Workout workout);
    }

    public WorkoutAdapter(List<Workout> workouts, OnWorkoutClickListener listener) {
        this.workouts = workouts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Workout workout = workouts.get(position);
        holder.tvName.setText(workout.getName());
        
        String intensity = "Moderate Intensity";
        if (workout.getMetValue() >= 8.0) intensity = "High Intensity";
        else if (workout.getMetValue() < 5.0) intensity = "Low Intensity";
        
        holder.tvMet.setText(intensity + " • " + workout.getMetValue() + " MET");
        
        holder.itemView.setOnClickListener(v -> listener.onWorkoutClick(workout));
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMet;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWorkoutName);
            tvMet = itemView.findViewById(R.id.tvWorkoutMet);
        }
    }
}
