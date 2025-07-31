package com.example.wisewallet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wallet_wise.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class GoalsInsightsActivity extends AppCompatActivity {

    private EditText goalNameInput, goalAmountInput;
    private Button saveGoalButton;
    private TextView goalDescription, goalProgressText, moneyTip;
    private ProgressBar goalProgressBar;
    private PieChart pieChart;
    private LinearLayout goalsContainer;

    private DatabaseReference databaseRef;
    private String uid;

    private final Handler tipHandler = new Handler();
    private final List<String> tips = Arrays.asList(
            "Track every shilling. It adds up!",
            "Always save before you spend.",
            "Invest in assets, not liabilities.",
            "Build an emergency fund for tough times.",
            "Use the 50/30/20 budgeting rule.",
            "Avoid impulse buying. Sleep on it.",
            "Compare prices before purchases.",
            "Cook at home more often to save money."
    );
    private int tipIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals_insights);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = user.getUid();
        databaseRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("goals").child(uid);

        // Bind views
        goalNameInput = findViewById(R.id.goalNameInput);
        goalAmountInput = findViewById(R.id.goalAmountInput);
        saveGoalButton = findViewById(R.id.saveGoalButton);
        goalDescription = findViewById(R.id.goalDescription);
        goalProgressText = findViewById(R.id.goalProgressText);
        goalProgressBar = findViewById(R.id.goalProgressBar);
        pieChart = findViewById(R.id.pieChartImage);
        moneyTip = findViewById(R.id.moneyTip);
        goalsContainer = findViewById(R.id.goalsContainer);

        saveGoalButton.setOnClickListener(v -> saveGoal());

        loadGoals();
        startTipRotation();
        renderPieChart();
    }

    private void saveGoal() {
        String name = goalNameInput.getText().toString().trim();
        String amountStr = goalAmountInput.getText().toString().trim();

        if (name.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount = Double.parseDouble(amountStr);
        String key = databaseRef.push().getKey();
        Goal goal = new Goal(name, targetAmount, 0);

        if (key != null) {
            goal.id = key;
            databaseRef.child(key).setValue(goal)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Goal saved", Toast.LENGTH_SHORT).show();
                        goalNameInput.setText("");
                        goalAmountInput.setText("");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadGoals() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                goalsContainer.removeAllViews();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Goal goal = snap.getValue(Goal.class);
                    if (goal == null) continue;
                    goal.id = snap.getKey();
                    addGoalCard(goal);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GoalsInsightsActivity.this, "Error loading goals", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addGoalCard(Goal goal) {
        View card = LayoutInflater.from(this).inflate(R.layout.card_goal, goalsContainer, false);
        TextView tvGoalName = card.findViewById(R.id.tvGoalName);
        TextView tvGoalProgress = card.findViewById(R.id.tvGoalProgress);
        ProgressBar progressBar = card.findViewById(R.id.goalProgressBar);
        Button updateSavedButton = card.findViewById(R.id.btnUpdateSavedAmount); // NEW

        tvGoalName.setText(goal.name);
        updateGoalProgressUI(goal, tvGoalProgress, progressBar);

        // Long press to edit goal name or target
        card.setOnLongClickListener(v -> {
            showEditGoalDialog(goal);
            return true;
        });

        // Update saved amount logic
        updateSavedButton.setOnClickListener(v -> {
            showUpdateSavedDialog(goal);
        });

        goalsContainer.addView(card);
    }

    private void updateGoalProgressUI(Goal goal, TextView progressText, ProgressBar progressBar) {
        double saved = goal.savedAmount;
        double target = goal.targetAmount;
        int percent = (target > 0) ? (int) ((saved / target) * 100) : 0;
        progressText.setText("Ksh " + saved + " / " + target);
        progressBar.setProgress(Math.min(percent, 100));
    }

    private void showUpdateSavedDialog(Goal goal) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_update_saved_amount, null);
        EditText editSavedAmount = view.findViewById(R.id.editSavedAmount);
        Button btnSave = view.findViewById(R.id.btnSaveSavedAmount);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnSave.setOnClickListener(v -> {
            String amountStr = editSavedAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double newSavedAmount = Double.parseDouble(amountStr);

            databaseRef.child(goal.id).child("savedAmount").setValue(newSavedAmount)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Saved amount updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); // Close dialog
                        // The ValueEventListener in loadGoals() will auto-refresh the progress
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }



    private void showEditGoalDialog(Goal goal) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialogue_edit_goal, null);
        EditText editGoalName = view.findViewById(R.id.editGoalName);
        EditText editTargetAmount = view.findViewById(R.id.editTargetAmount);
        Button updateButton = view.findViewById(R.id.updateGoalButton);

        editGoalName.setText(goal.name);
        editTargetAmount.setText(String.valueOf(goal.targetAmount));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        updateButton.setOnClickListener(v -> {
            String newName = editGoalName.getText().toString().trim();
            String newTargetStr = editTargetAmount.getText().toString().trim();

            if (newName.isEmpty() || newTargetStr.isEmpty()) {
                Toast.makeText(this, "Missing fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double newTarget = Double.parseDouble(newTargetStr);
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("targetAmount", newTarget);

            databaseRef.child(goal.id).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Goal updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Live data listener will auto-refresh UI
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update goal", Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }

    private void startTipRotation() {
        tipHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moneyTip.setText("Tip: " + tips.get(tipIndex));
                tipIndex = (tipIndex + 1) % tips.size();
                tipHandler.postDelayed(this, 60000); // 1 minute
            }
        }, 0);
    }

    private void renderPieChart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference txnRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("transactions").child(uid);

        txnRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Float> categoryTotals = new HashMap<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String category = snap.child("category").getValue(String.class);
                    String type = snap.child("type").getValue(String.class);

                    Object amountObj = snap.child("amount").getValue();
                    double amount = 0;

                    if (amountObj instanceof Long) {
                        amount = ((Long) amountObj).doubleValue();
                    } else if (amountObj instanceof Double) {
                        amount = (Double) amountObj;
                    } else if (amountObj instanceof String) {
                        try {
                            amount = Double.parseDouble((String) amountObj);
                        } catch (NumberFormatException e) {
                            // skip invalid value
                            continue;
                        }
                    }

                    if (category != null && "Expense".equalsIgnoreCase(type)) {
                        categoryTotals.put(category,
                                categoryTotals.getOrDefault(category, 0f) + (float) amount);
                    }
                }

                List<PieEntry> entries = new ArrayList<>();
                for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
                    entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                }

                if (entries.isEmpty()) return;

                PieDataSet dataSet = new PieDataSet(entries, "Expenses");
                dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                PieData data = new PieData(dataSet);
                data.setValueTextSize(14f);

                pieChart.setData(data);
                pieChart.setDrawEntryLabels(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.setUsePercentValues(true);

                Legend legend = pieChart.getLegend();
                legend.setEnabled(true);
                pieChart.invalidate(); // refresh chart
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GoalsInsightsActivity.this, "Failed to load chart", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Firebase-compatible model
    public static class Goal {
        public String id;
        public String name;
        public double targetAmount;
        public double savedAmount;

        public Goal() {
        }

        public Goal(String name, double targetAmount, double savedAmount) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.savedAmount = savedAmount;
        }
    }

}
