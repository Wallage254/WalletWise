package com.example.wisewallet;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.wallet_wise.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileFragment extends Fragment {
    private static final String PREF_NAME = "user_profile_prefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_IMAGE_URI = "profile_image_uri";
    private ImageView profileImage;
    private TextView nameText, emailText, joinedText;
    private Button btnEdit, btnChangePass, btnPreferences, btnLogout;
    private Switch switchDarkMode;
    private Spinner spinnerLanguage;
    private TextView totalSpentText, goalsAchievedText;
    private SharedPreferences pref;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private boolean isFirstSelection = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileImage = view.findViewById(R.id.profile_image);
        nameText = view.findViewById(R.id.profile_name);
        emailText = view.findViewById(R.id.profile_email);
        joinedText = view.findViewById(R.id.joined_text);
        btnEdit = view.findViewById(R.id.btn_edit_profile);
        btnChangePass = view.findViewById(R.id.btn_change_password);
        btnPreferences = view.findViewById(R.id.btn_preferences);
        btnLogout = view.findViewById(R.id.btn_logout);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        spinnerLanguage = view.findViewById(R.id.spinner_language);
        totalSpentText = view.findViewById(R.id.total_spent);
        goalsAchievedText = view.findViewById(R.id.goals_achieved);

        auth = FirebaseAuth.getInstance();
        pref = requireContext().getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);

        setupImagePicker();
        loadProfile();
        loadUserInfo();
        setupEditNameDialog();
        setupPasswordReset();
        setupDarkModeSwitch();
        setupLanguageSpinner();
        loadProfileSummary();
    }

    private void setupImagePicker() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).circleCrop().into(profileImage);
                            pref.edit().putString(KEY_IMAGE_URI, selectedImageUri.toString()).apply();
                        }
                    }
                }
        );

        profileImage.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(pickIntent);
        });
    }

    private void loadProfile() {
        nameText.setText(pref.getString(KEY_NAME, "Your Name"));
        String uri = pref.getString(KEY_IMAGE_URI, null);
        if (uri != null) {
            Glide.with(this).load(Uri.parse(uri)).circleCrop().into(profileImage);
        }
    }

    private void loadProfileSummary() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // 1. Fetch total spending
        database.child("transactions").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalSpent = 0.0;
                        for (DataSnapshot transactionSnap : snapshot.getChildren()) {
                            String type = transactionSnap.child("type").getValue(String.class);
                            String amount = String.valueOf(transactionSnap.child("amount").getValue(String.class));

                            if ("Expense".equalsIgnoreCase(type) && amount != null) {
                                totalSpent += Double.parseDouble(amount);
                            }
                        }
                        totalSpentText.setText("Total Spent: KES " + String.format(Locale.getDefault(), "%,.2f", totalSpent));
                    }


                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error loading spending", Toast.LENGTH_SHORT).show();
                    }
                });

        // 2. Fetch goals achieved
        database.child("goals").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int achieved = 0;
                        for (DataSnapshot goalSnap : snapshot.getChildren()) {
                            double target = goalSnap.child("targetAmount").getValue(Double.class) != null
                                    ? goalSnap.child("targetAmount").getValue(Double.class) : 0;
                            double saved = goalSnap.child("savedAmount").getValue(Double.class) != null
                                    ? goalSnap.child("savedAmount").getValue(Double.class) : 0;

                            if (saved >= target && target > 0) achieved++;
                        }
                        goalsAchievedText.setText("Goals Achieved: " + achieved);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error loading goals", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            emailText.setText(user.getEmail());
            if (user.getMetadata() != null) {
                String joinedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(user.getMetadata().getCreationTimestamp()));
                joinedText.setText("Joined: " + joinedDate);
            }
        }

    }

    private void setupEditNameDialog() {
        btnEdit.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setHint("Enter new name");

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Edit Name")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            pref.edit().putString(KEY_NAME, newName).apply();
                            nameText.setText(newName);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupPasswordReset() {
        btnChangePass.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                auth.sendPasswordResetEmail(user.getEmail())
                        .addOnSuccessListener(unused -> Toast.makeText(getContext(),
                                "Password reset email sent. Check spam/promotions folder.",
                                Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupDarkModeSwitch() {
        boolean isDark = pref.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);
        AppCompatDelegate.setDefaultNightMode(isDark ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void setupLanguageSpinner() {
        List<String> langs = Arrays.asList("English", "Swahili");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, langs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        String selectedLang = pref.getString("language", "English");
        spinnerLanguage.setSelection(langs.indexOf(selectedLang));

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }

                String chosen = langs.get(position);
                pref.edit().putString("language", chosen).apply();

                Locale locale = chosen.equals("Swahili") ? new Locale("sw") : Locale.ENGLISH;
                Locale.setDefault(locale);

                Resources resources = requireContext().getResources();
                Configuration config = new Configuration(resources.getConfiguration());
                config.setLocale(locale);
                resources.updateConfiguration(config, resources.getDisplayMetrics());

                requireActivity().recreate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
}
