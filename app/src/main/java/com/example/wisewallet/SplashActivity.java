package com.example.wisewallet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
//  private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                RecurringTransactionWorker.class, 1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RecurringWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PreferencesManager preferencesManager = new PreferencesManager(this);

            if (!preferencesManager.isOnboardingCompleted()) {
                // First time → show onboarding
                startActivity(new Intent(this, OnboardingActivity.class));
            } else {
                // Check if user is logged in
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
            }
            finish();
        }, 2000);
    }
}