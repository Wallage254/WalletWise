package com.example.wisewallet;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wallet_wise.R;

public class PremiumUpgradeActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_upgrade);

        preferencesManager = new PreferencesManager(this);

        Button btnUpgrade = findViewById(R.id.btnUpgrade);
        btnUpgrade.setOnClickListener(v -> {
            // Normally, trigger Google Play Billing here
            preferencesManager.setPremium(true);
            Toast.makeText(this, "Premium Unlocked!", Toast.LENGTH_SHORT).show();
            finish();
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }
}
