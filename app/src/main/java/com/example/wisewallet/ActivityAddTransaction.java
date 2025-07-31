package com.example.wisewallet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.wallet_wise.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityAddTransaction extends AppCompatActivity {

    private TextView amountValue, dateValue, noteValue, recurrenceValue, saveButton;
    private TextView categoryValue, subcategoryValue, paymentValue;
    private LinearLayout categoryRow, subcategoryRow, paymentRow, recurrenceRow, dateRow;

    private DatabaseReference transactionsRef;
    private DatabaseReference budgetRef;
    private FirebaseUser currentUser;
    private String selectedType = "Expense";

    private Map<String, List<String>> categoryMap;
    private Map<String, List<String>> incomeCategoryMap;
    private Map<String, List<String>> expenseCategoryMap;

    private boolean isUpdateMode = false;
    private String existingTransactionId = null;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);

        isUpdateMode = getIntent().getBooleanExtra("isUpdate", false);
        existingTransactionId = getIntent().getStringExtra("transactionId");

        if (isUpdateMode && existingTransactionId != null) {
            // Prefill UI
            loadExistingTransaction(existingTransactionId);
        }


        expenseCategoryMap = CategoryData.loadCategories(this);
        incomeCategoryMap = CategoryData.loadIncomeCategories(this);

        categoryMap = expenseCategoryMap;

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        transactionsRef = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child(currentUser.getUid());
        transactionsRef.keepSynced(true);

        budgetRef = FirebaseDatabase.getInstance().getReference("budgets").child(currentUser.getUid());

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        Objects.requireNonNull(tabLayout.getTabAt(0)).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Expense
                        selectedType = "Expense";
                        categoryMap = expenseCategoryMap;
                        break;
                    case 1: // Income
                        selectedType = "Income";
                        categoryMap = incomeCategoryMap;
                        break;
                    case 2: // Transfer
                        selectedType = "Transfer";
                        categoryMap = Collections.emptyMap();
                        break;
                }
                categoryValue.setText("Select Category");
                subcategoryValue.setText("Select Subcategory");
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });




        amountValue = findViewById(R.id.amountValue);
        dateValue = findViewById(R.id.dateValue);
        noteValue = findViewById(R.id.noteValue);
        recurrenceValue = findViewById(R.id.recurrenceValue);
        saveButton = findViewById(R.id.saveButton);
        categoryValue = findViewById(R.id.categoryValue);
        subcategoryValue = findViewById(R.id.subcategoryValue);
        paymentValue = findViewById(R.id.paymentValue);

        categoryRow = findViewById(R.id.categoryRow);
        subcategoryRow = findViewById(R.id.subcategoryRow);
        paymentRow = findViewById(R.id.paymentRow);
        recurrenceRow = findViewById(R.id.recurrenceRow);
        dateRow = findViewById(R.id.dateRow);

        dateValue.setText(getTodayDate());
        paymentValue.setText("Cash");
        recurrenceValue.setText("None");

        if (getIntent().hasExtra("type")) {
            selectedType = getIntent().getStringExtra("type");
        }

        saveButton.setOnClickListener(v -> saveTransactionToFirebase());

        categoryRow.setOnClickListener(v -> {
            String[] categories = categoryMap.keySet().toArray(new String[0]);
            showSelectionDialog("Select Category", categories, categoryValue, this::updateSubcategoryOptions);
        });

        subcategoryRow.setOnClickListener(v -> {
            String selectedCategory = categoryValue.getText().toString().trim();
            if (selectedCategory.isEmpty() || selectedCategory.equalsIgnoreCase("Select Category")) {
                Toast.makeText(this, "Select a category first", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> subcats = categoryMap.get(selectedCategory);
            if (subcats != null) {
                String[] subcatArray = subcats.toArray(new String[0]);
                showSelectionDialog("Select Subcategory", subcatArray, subcategoryValue);
            } else {
                Toast.makeText(this, "No subcategories found for " + selectedCategory, Toast.LENGTH_SHORT).show();
            }
        });

        paymentRow.setOnClickListener(v ->
                showSelectionDialog("Select Payment Method", new String[]{"Cash", "M-Pesa", "Card", "Bank Transfer", "Crypto"}, paymentValue));

        recurrenceRow.setOnClickListener(v -> {
            String[] options = {"None", "Daily", "Weekly", "Monthly", "Yearly", "Custom"};

            new AlertDialog.Builder(this)
                    .setTitle("Select Recurrence")
                    .setItems(options, (dialog, which) -> {
                        String selected = options[which];
                        if ("Custom".equals(selected)) {
                            PreferencesManager prefs = new PreferencesManager(this);
                            if (!prefs.isPremium()) {
                                showPremiumUpsellDialog();
                            } else {
                                showCustomDateDialog();
                            }
                        } else {
                            recurrenceValue.setText(selected);
                            recurrenceValue.setTag(null); // no custom interval
                        }
                    })
                    .show();
        });



        dateRow.setOnClickListener(v -> showDatePicker());
    }
    private void showPremiumUpsellDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Premium Feature")
                .setMessage("Custom recurrence is available for Premium users only. Upgrade now to unlock this feature.")
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(this, PremiumUpgradeActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void showCustomDateDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter interval in days (e.g., 10)");

        new AlertDialog.Builder(this)
                .setTitle("Custom Recurrence")
                .setMessage("Repeat this transaction every X days:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        recurrenceValue.setText("Custom: " + value + " days");
                        recurrenceValue.setTag(value); // store numeric days in tag
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private String getTodayDate() {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat("EEE. dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
    }

    private void showSelectionDialog(String title, String[] options, TextView targetView) {
        showSelectionDialog(title, options, targetView, null);
    }

    private void showSelectionDialog(String title, String[] options, TextView targetView, Runnable onSelected) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    targetView.setText(options[which]);
                    if (onSelected != null) onSelected.run();
                })
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void updateSubcategoryOptions() {
        subcategoryValue.setText("Select Subcategory");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            calendar.set(y, m, d);
            String formatted = String.format(Locale.getDefault(), "%s. %02d/%02d/%d",
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()),
                    d, m + 1, y);
            dateValue.setText(formatted);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTransactionToFirebase() {
        String amountStr = amountValue.getText().toString().trim();
        String date = dateValue.getText().toString().trim();
        String note = noteValue.getText().toString().trim();
        String recurrence = recurrenceValue.getText().toString().trim();
        String category = categoryValue.getText().toString().trim();
        String subcategory = subcategoryValue.getText().toString().trim();
        String payment = paymentValue.getText().toString().trim();

        // Validate required fields
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.isEmpty() || category.equalsIgnoreCase("Select Category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subcategory.isEmpty() || subcategory.equalsIgnoreCase("Select Subcategory")) {
            Toast.makeText(this, "Please select a subcategory", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (payment.equals("M-Pesa")) {
            initiateMpesaTransaction(amountStr, selectedType);
        }

        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        HashMap<String, Object> data = new HashMap<>();
        data.put("amount", amountStr);
        data.put("date", date);
        data.put("note", note);
        data.put("recurrence", recurrence);
        data.put("customIntervalDays", recurrenceValue.getTag() != null ? recurrenceValue.getTag().toString() : null);
        data.put("category", category);
        data.put("subcategory", subcategory);
        data.put("paymentMethod", payment);
        data.put("time", time);
        data.put("lastProcessedDate", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        data.put("type", selectedType);

        if (isUpdateMode && existingTransactionId != null) {
            // === UPDATE ===
            transactionsRef.child(existingTransactionId).updateChildren(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // === NEW ===
            String key = transactionsRef.push().getKey();
            if (key != null) {
                data.put("id", key);
                transactionsRef.child(key).setValue(data)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }


        String key = transactionsRef.push().getKey();
        if (key != null) {
            data.put("id", key);
            transactionsRef.child(key).setValue(data)
                    .addOnSuccessListener(unused -> {
                        if (selectedType.equals("Expense")) {
                            subtractFromBudget(amountStr);
                        }
                        // Convert HashMap to model (for notification)
                        TransactionModel model = new TransactionModel();
                        model.setAmount(amountStr);
                        model.setDate(date);
                        model.setNote(note);
                        model.setCategory(category);
                        model.setSubcategory(subcategory);
                        model.setPaymentMethod(payment);
                        model.setType(selectedType);

                        Utility.sendTransactionNotification(this, model);

                        Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity after successful save
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TransactionSave", "Failed to save transaction", e);
                        Toast.makeText(this, "Failed to save transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            Toast.makeText(this, "Failed to generate transaction key", Toast.LENGTH_SHORT).show();
        }
    }

    private void subtractFromBudget(String amountStr) {
        budgetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.hasChildren()) {
                    Log.d("BudgetUpdate", "No budgets available");
                    return;
                }

                // Assume only one budget node (the user is setting only one)
                for (DataSnapshot budgetSnap : snapshot.getChildren()) {
                    BudgetModel budget = budgetSnap.getValue(BudgetModel.class);
                    if (budget == null) continue;

                    try {
                        double allocated = Double.parseDouble(budget.getAmount());
                        double spent = (budget.getSpent() != null) ? Double.parseDouble(budget.getSpent()) : 0.0;
                        double expenseAmount = Double.parseDouble(amountStr);

                        double newSpent = spent + expenseAmount;
                        double remaining = allocated - newSpent;

                        budgetSnap.getRef().child("spent").setValue(String.valueOf(newSpent));
                        budgetSnap.getRef().child("remaining").setValue(String.valueOf(remaining));

                        if (remaining < 0) {
                            Toast.makeText(ActivityAddTransaction.this,
                                    "Budget exceeded by KSh " + String.format("%.2f", Math.abs(remaining)),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ActivityAddTransaction.this,
                                    "Budget updated. Remaining: KSh " + String.format("%.2f", remaining),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Log.e("BudgetUpdate", "Error parsing amounts", e);
                    }
                    break; // Update only one budget
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BudgetUpdate", "Failed to read budgets", error.toException());
            }
        });
    }



    private void initiateMpesaTransaction(String amount, String type) {
        Log.d("MPESA", "STK Push Initiated via sandbox (Simulated)");
        if (type.equals("Income")) {
            Toast.makeText(this, "Simulated M-Pesa INCOMING (sandbox)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Simulated M-Pesa OUTGOING (sandbox)", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExistingTransaction(String transactionId) {
        transactionsRef.child(transactionId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                TransactionModel transaction = snapshot.getValue(TransactionModel.class);
                if (transaction != null) {
                    amountValue.setText(transaction.getAmount());
                    dateValue.setText(transaction.getDate());
                    noteValue.setText(transaction.getNote());
                    recurrenceValue.setText(transaction.getRecurrence());
                    categoryValue.setText(transaction.getCategory());
                    subcategoryValue.setText(transaction.getSubcategory());
                    paymentValue.setText(transaction.getPaymentMethod());
                    selectedType = transaction.getType();
                }
            }
        });
    }


}