package com.example.wisewallet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*; // Using wildcard import, consider specific imports
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Added for navigateTo

// Make sure this R import is correct for your project structure.
// If SettingsFragment is in 'com.example.wisewallet' and that's your app module,
// it might be 'com.example.wallet_wise.R'
import com.example.wallet_wise.R;


import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Added for better dialog styling
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit; // Added for OkHttpClient timeouts

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private Button btnLogout, btnExportData, btnDeleteAllData, btnHelp, btnContactSupport, btnRefreshCurrency;
    private TextView tvUserEmail;
    private Switch switchDarkMode, switchNotifications;
    private Spinner spinnerThemeColor;
    private PreferencesManager preferencesManager;
    private OkHttpClient okHttpClient; // For network requests

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize OkHttpClient once
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // It's safer to initialize PreferencesManager here if context is always available.
        // If requireContext() could fail, consider moving to onCreate or adding more checks.
        if (getContext() != null) {
            preferencesManager = new PreferencesManager(requireContext());
        } else {
            Log.e(TAG, "Context is null in onViewCreated, PreferencesManager not initialized.");
            // Handle this gracefully, perhaps by disabling features that need it.
            Toast.makeText(getActivity(), "Error: Preferences could not be loaded.", Toast.LENGTH_LONG).show();
            // Optionally, disable buttons or return if preferencesManager is critical
        }


        bindViews(view);
        setupUIListenersAndStates();
    }

    private void bindViews(View view) {
        btnLogout = view.findViewById(R.id.btnLogout);
        btnExportData = view.findViewById(R.id.btnExportData);
        btnDeleteAllData = view.findViewById(R.id.btnDeleteAllData);
        btnHelp = view.findViewById(R.id.btnHelp);
        btnContactSupport = view.findViewById(R.id.btnContactSupport);
        btnRefreshCurrency = view.findViewById(R.id.btnRefreshCurrency);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        spinnerThemeColor = view.findViewById(R.id.spinnerThemeColor);
    }

    private void setupUIListenersAndStates() {
        setupUserEmail();
        setupLogoutButton();
        setupDataButtons();
        setupHelpAndSupportButtons();

        // Ensure preferencesManager is not null before accessing its methods
        if (preferencesManager != null) {
            setupDarkModeSwitch();
            setupNotificationsSwitch();
            setupThemeSpinner();
        } else {
            Log.w(TAG, "PreferencesManager is null, skipping setup for dark mode, notifications, and theme.");
            // Optionally disable these UI elements or show a message
            if(switchDarkMode != null) switchDarkMode.setEnabled(false);
            if(switchNotifications != null) switchNotifications.setEnabled(false);
            if(spinnerThemeColor != null) spinnerThemeColor.setEnabled(false);
        }
    }


    private void setupUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && tvUserEmail != null) {
            tvUserEmail.setText("Email: " + user.getEmail());
        } else if (tvUserEmail != null) {
            tvUserEmail.setText("Email: Not available");
        }
    }

    private void setupLogoutButton() {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                // Consider also signing out from Google if you used Google Sign-In
                // GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
                // mGoogleSignInClient.signOut();
                if (getContext() != null) { // Check context before showing Toast
                    Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                }
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }
    }

    private void setupDataButtons() {
        if (btnExportData != null) {
            btnExportData.setOnClickListener(v -> showExportDialog());
        }
        if (btnDeleteAllData != null) {
            btnDeleteAllData.setOnClickListener(v -> showDeleteConfirmation());
        }
        if (btnRefreshCurrency != null) {
            btnRefreshCurrency.setOnClickListener(v -> {
                if (getContext() != null) { // Check context
                    Toast.makeText(getContext(), "Refreshing currency rates...", Toast.LENGTH_SHORT).show();
                }
                refreshCurrencyRates();
            });
        }
    }

    private void setupHelpAndSupportButtons() {
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> showHelpDialog());
        }
        if (btnContactSupport != null) {
            btnContactSupport.setOnClickListener(v -> {
                if (getContext() != null) { // Check context
                    Toast.makeText(getContext(), "Support: support@wisewallet.com", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setupDarkModeSwitch() {
        if (switchDarkMode != null && preferencesManager != null && getContext() != null) {
            switchDarkMode.setChecked(preferencesManager.isDarkModeEnabled());
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferencesManager.setDarkModeEnabled(isChecked);
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                Toast.makeText(getContext(), "Dark mode " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                // if (getActivity() != null) getActivity().recreate(); // If full theme change needs activity recreation
            });
        }
    }

    private void setupNotificationsSwitch() {
        if (switchNotifications != null && preferencesManager != null && getContext() != null) {
            // Assuming you have these methods in PreferencesManager
            // switchNotifications.setChecked(preferencesManager.isNotificationsEnabled());
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // preferencesManager.setNotificationsEnabled(isChecked);
                Toast.makeText(getContext(), "Notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupThemeSpinner() {
        if (spinnerThemeColor == null || preferencesManager == null || getContext() == null) {
            if (spinnerThemeColor == null) Log.e(TAG, "spinnerThemeColor is null in setupThemeSpinner.");
            if (preferencesManager == null) Log.e(TAG, "preferencesManager is null in setupThemeSpinner.");
            if (getContext() == null) Log.e(TAG, "context is null in setupThemeSpinner.");
            return;
        }

        List<String> themes = Arrays.asList("Default", "Peach", "Ocean", "Emerald", "Lavender");
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, themes);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThemeColor.setAdapter(themeAdapter);

        String savedTheme = preferencesManager.getThemeColor();
        int themePosition = themes.indexOf(savedTheme);
        spinnerThemeColor.setSelection(themePosition >= 0 ? themePosition : 0);

        spinnerThemeColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (preferencesManager == null || getContext() == null) return;
                String selectedTheme = parent.getItemAtPosition(position).toString();
                preferencesManager.setThemeColor(selectedTheme);
                //Toast.makeText(getContext(), "Theme set to " + selectedTheme, Toast.LENGTH_SHORT).show();
                // if (getActivity() != null) getActivity().recreate(); // If theme change needs activity recreation
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private void showDeleteConfirmation() {
        if (getContext() == null) return;
        // Using MaterialAlertDialogBuilder for consistency if Material Components are used
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete All Data")
                .setMessage("Are you sure you want to delete all your data? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        // Consider deleting from multiple nodes if data is spread out
                        FirebaseDatabase.getInstance().getReference("users").child(uid) // Example path
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    if (getContext() != null)
                                        Toast.makeText(getContext(), "All data deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete data", e);
                                    if (getContext() != null)
                                        Toast.makeText(getContext(), "Failed to delete data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "User not found. Cannot delete data.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showExportDialog() {
        if (getContext() == null) return;
        // Using MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(requireContext()) // Use requireContext() for non-null context
                .setTitle("Export Data")
                .setMessage("Export all transactions as CSV?") // Text color should be handled by Material theme
                .setPositiveButton("Export", (dialog, which) -> {
                    // TODO: Add actual export logic here
                    Toast.makeText(getContext(), "Data exported (stub)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHelpDialog() {
        if (getContext() == null) return;
        // Using MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Help")
                .setMessage("💡 Tips:\n• Long-press a goal to edit.\n• Use the pie chart for expense breakdown.\n• Export your data to CSV.\n\nMore help coming soon!")
                .setPositiveButton("Close", null)
                .show();
    }

    private void refreshCurrencyRates() {
        if (getContext() == null || getActivity() == null) {
            Log.w(TAG, "Context or Activity not available for currency refresh.");
            return;
        }

        // --- CONFIGURATION FOR ExchangeRate-API ---
        // TODO: IMPORTANT! Replace with your actual key or load securely (e.g., from BuildConfig)
        String exchangeRateApiKey = "816879d865c66c7e39b4a307"; // Replace this
        String baseCurrencyForAPI = "USD"; // Or "EUR", etc.

        if (exchangeRateApiKey.equals("816879d865c66c7e39b4a307") || exchangeRateApiKey.isEmpty()) {
            Log.e(TAG, "ExchangeRate-API key is a placeholder or empty. Please configure it.");
            Toast.makeText(getContext(), "ExchangeRate-API key not configured.", Toast.LENGTH_LONG).show();
            return;
        }
        // --- END CONFIGURATION ---

        String url = "https://v6.exchangerate-api.com/v6/" + exchangeRateApiKey + "/latest/" + baseCurrencyForAPI;
        Log.d(TAG, "Requesting URL for ExchangeRate-API: " + url);

        Request request = new Request.Builder().url(url).build();

        okHttpClient.newCall(request).enqueue(new Callback() { // Use the class member okHttpClient
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch currency rates from ExchangeRate-API", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Network error fetching rates: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) {
                    if (response.body() != null) response.body().close();
                    return;
                }

                final String jsonResponseString = response.body() != null ? response.body().string() : null;
                if (response.body() != null) response.body().close();


                if (response.isSuccessful() && jsonResponseString != null) {
                    try {
                        JSONObject obj = new JSONObject(jsonResponseString);
                        if ("success".equals(obj.optString("result"))) {
                            JSONObject rates = obj.getJSONObject("conversion_rates");

                            double kesRate = rates.optDouble("KES", 0.0);
                            double usdRate = rates.optDouble("USD", 0.0); // If base is EUR, this is EUR per USD
                            double gbpRate = rates.optDouble("GBP", 0.0);
                            // Add other currencies as needed by your app

                            Log.d(TAG, "Rates fetched (vs " + baseCurrencyForAPI + "): KES " + kesRate + ", USD " + usdRate + ", GBP " + gbpRate);

                            // TODO: Save these rates to SharedPreferences or a local database
                            // if (preferencesManager != null) {
                            //    preferencesManager.saveCurrencyRates(baseCurrencyForAPI, kesRate, usdRate, gbpRate);
                            //    preferencesManager.setLastRatesUpdateTimestamp(System.currentTimeMillis());
                            // }

                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null)
                                    Toast.makeText(getContext(), "Rates updated: KES " + kesRate + ", USD " + usdRate, Toast.LENGTH_LONG).show();
                            });

                        } else {
                            String errorType = obj.optString("error-type", "Unknown API error");
                            Log.e(TAG, "ExchangeRate-API Error: " + errorType + " | Response: " + jsonResponseString);
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null)
                                    Toast.makeText(getContext(), "API Error: " + errorType, Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error for ExchangeRate-API", e);
                        getActivity().runOnUiThread(() -> {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Error parsing currency data.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Failed to fetch from ExchangeRate-API. Code: " + response.code() + " | Response: " + jsonResponseString);
                    final int responseCode = response.code();
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Server error fetching rates: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Helper method to navigate to this SettingsFragment using manual Fragment transactions.
     * Call this from the Fragment/Activity from which you want to open settings.
     *
     * @param fragmentManager The FragmentManager to perform the transaction.
     * @param containerId     The ID of the FrameLayout container where the fragment will be placed.
     */
    public static void navigateTo(FragmentManager fragmentManager, int containerId) {
        if (fragmentManager == null) {
            Log.e(TAG, "FragmentManager is null, cannot navigate to SettingsFragment.");
            return;
        }
        if (containerId == 0) { // Basic check for a valid container ID
            Log.e(TAG, "Container ID is 0 (invalid), cannot navigate to SettingsFragment.");
            return;
        }

        SettingsFragment settingsFragment = new SettingsFragment();
        fragmentManager.beginTransaction()
                .replace(containerId, settingsFragment, SettingsFragment.class.getSimpleName()) // Optional: Add a tag
                .addToBackStack(SettingsFragment.class.getSimpleName()) // CRUCIAL for back navigation
                .commit();
    }
}
