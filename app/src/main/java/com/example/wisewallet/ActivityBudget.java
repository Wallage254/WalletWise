package com.example.wisewallet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ActivityBudget extends AppCompatActivity {

    RecyclerView recyclerView;
    BudgetAdapter adapter;
    List<BudgetModel> budgetList;
    DatabaseReference budgetRef, transactionRef;
    FirebaseUser currentUser;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;


    double totalIncome = 0.0;

    TextView totalIncomeView, totalSpentView;
    ProgressBar progressBar, spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "budget_alerts",
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }


        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        budgetList = new ArrayList<>();
        adapter = new BudgetAdapter(this, budgetList);
        recyclerView.setAdapter(adapter);

        totalIncomeView = findViewById(R.id.total_income);
        totalSpentView = findViewById(R.id.total_spent);
        progressBar = findViewById(R.id.budget_progress);
        spinner = findViewById(R.id.progressBar);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        transactionRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("transactions")
                .child(currentUser.getUid());

        budgetRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("budgets")
                .child(currentUser.getUid());

        createNotificationChannel();
        loadBudgetData();
        loadTotalIncome();
        checkNotificationPermission();

    }

    private void loadBudgetData() {


        budgetRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                budgetList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    BudgetModel model = snap.getValue(BudgetModel.class);
                    if (model != null) {
                        budgetList.add(model);
                    }
                }
                adapter.notifyDataSetChanged();
                calculateTotalSpent();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ActivityBudget.this, "Failed to load budget data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTotalIncome() {

        spinner.setVisibility(View.VISIBLE);
        transactionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                totalIncome = 0.0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String type = snap.child("type").getValue(String.class);
                    String amount = snap.child("amount").getValue(String.class);
                    if (type != null && type.equals("Income") && amount != null) {
                        totalIncome += Double.parseDouble(amount);
                    }
                }
                totalIncomeView.setText(String.format("%.2f", totalIncome));
                spinner.setVisibility(View.GONE);
                calculateTotalSpent(); // In case budgets already loaded
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ActivityBudget.this, "Failed to load transactions.", Toast.LENGTH_SHORT).show();
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void calculateTotalSpent() {
        double totalSpent = 0.0;
        for (BudgetModel model : budgetList) {
            totalSpent += Double.parseDouble(model.getAmount());
        }

        totalSpentView.setText(String.format("%.2f", totalSpent));

        if (totalIncome > 0) {
            int percentUsed = (int) ((totalSpent / totalIncome) * 100);
            progressBar.setProgress(Math.min(percentUsed, 100));

            if (totalSpent >= totalIncome) {
                sendBudgetExceededNotification();
            }
        }
    }

    private void sendBudgetExceededNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "budget_alerts")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Budget Limit Reached")
                .setContentText("You've allocated all your income. Track spending carefully.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return; // Permission not granted
        }

        manager.notify(1, builder.build());
    }


    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "budget_alerts",
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
