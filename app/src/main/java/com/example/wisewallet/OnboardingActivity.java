package com.example.wisewallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.wallet_wise.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 onboardingViewPager;
    private Button buttonNext, buttonSkip;
    private TabLayout tabIndicator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.viewPager);
        tabIndicator = findViewById(R.id.tabIndicator);
        buttonNext = findViewById(R.id.buttonNext);
        buttonSkip = findViewById(R.id.buttonSkip);

        onboardingViewPager.setPageTransformer((page, position) -> {
            page.setAlpha(0f);
            page.setTranslationX(page.getWidth() * -position);
            page.animate().alpha(1f).setDuration(300).start();
        });


        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.money, "Track Expenses", "Monitor your daily spending easily"));
        items.add(new OnboardingItem(R.drawable.ic_budget, "Set Budgets", "Plan your finances better"));
        items.add(new OnboardingItem(R.drawable.ic_transaction, "Save Money", "Achieve your savings goals faster"));

        onboardingViewPager.setAdapter(new OnboardingAdapter(items));

        new TabLayoutMediator(tabIndicator, onboardingViewPager, (tab, position) -> {}).attach();

        buttonSkip.setOnClickListener(v -> navigateToMain());
        buttonNext.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < items.size()) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {

                navigateToMain();
            }
        });

    }

    private void navigateToMain() {
        PreferencesManager preferencesManager = new PreferencesManager(this);
        preferencesManager.setOnboardingCompleted(true);

        // Check if user is logged in before deciding where to go
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        } else {
            startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        }
        finish();
    }
}
