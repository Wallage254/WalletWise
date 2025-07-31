package com.example.wisewallet;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.wallet_wise.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Contains only FrameLayout and BottomNav

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Default load
        loadFragment(new DashboardFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.home) {
                selectedFragment = new DashboardFragment();
            } else if (id == R.id.settings) {
                selectedFragment = new SettingsFragment();
            } else if (id == R.id.profile) {
                selectedFragment = new ProfileFragment(); // Create if not already done
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NetworkUtils.isNetworkAvailable(this)) {
            MpesaSmsParser.syncOfflineTransactions(this);
        }
    }
}
