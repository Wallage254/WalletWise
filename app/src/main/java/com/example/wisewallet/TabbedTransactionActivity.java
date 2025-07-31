package com.example.wisewallet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wallet_wise.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.*;

public class TabbedTransactionActivity extends AppCompatActivity {

    private TextView saveButton, dateValue, recurrenceValue, categoryValue, subcategoryValue, paymentValue;
    private EditText amountValue, noteValue;
    private LinearLayout categoryRow, subcategoryRow, paymentRow, recurrenceRow, dateRow;

    private FirebaseUser user;

    private final String[] dummyCategories = {"Salary", "Freelance", "Investments", "Gifts", "Refunds"};
    private final String[] dummyPayments = {"M-Pesa", "Bank Transfer", "Cash", "Crypto", "Cheque"};
    private final String[] dummyRecurrence = {"None", "Daily", "Weekly", "Monthly"};

    private final HashMap<String, String[]> dummySubcategories = new HashMap<String, String[]>() {{
        put("Salary", new String[]{"Monthly Pay", "Bonus", "Allowance"});
        put("Freelance", new String[]{"Writing", "Design", "Coding"});
        put("Investments", new String[]{"Dividends", "Interest", "Crypto"});
        put("Gifts", new String[]{"Birthday", "Holiday", "Other"});
        put("Refunds", new String[]{"Returns", "Reimbursements"});
    }};

//    Tab Layout Logic
private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction);


        //        Tab layout's Logic
        tabLayout = findViewById(R.id.tabLayout);

        // Set default tab (Expense)
        tabLayout.getTabAt(1).select();

        // Handle tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String selectedType;

                switch (tab.getPosition()) {
                    case 0:
                        selectedType = "Expense";
                        break;
                    case 1:
                        selectedType = "Income";
                        break;
                    case 2:
                        selectedType = "Transfer"; // optional if you want to use
                        break;
                    default:
                        selectedType = "Expense";
                }

                // Launch the Add Transaction screen
//                Intent intent = new Intent(TabbedTransactionActivity.this, TabbedTransactionActivity.class);
//                intent.putExtra("type", selectedType);
//                startActivity(intent);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
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

        // Default values
        dateValue.setText(getTodayDate());
        recurrenceValue.setText("None");
        categoryValue.setText("Salary");
        subcategoryValue.setText("Monthly Pay");
        paymentValue.setText("M-Pesa");

        // Category click
        categoryRow.setOnClickListener(v ->
                showDialog("Select Category", dummyCategories, categoryValue, this::resetSubcategory));

        subcategoryRow.setOnClickListener(v -> {
            String selected = categoryValue.getText().toString();
            String[] subcats = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                subcats = dummySubcategories.getOrDefault(selected, new String[]{"Other"});
            }
            showDialog("Select Subcategory", subcats, subcategoryValue, null);
        });

        paymentRow.setOnClickListener(v ->
                showDialog("Select Payment Method", dummyPayments, paymentValue, null));

        recurrenceRow.setOnClickListener(v ->
                showDialog("Select Recurrence", dummyRecurrence, recurrenceValue, null));

        dateRow.setOnClickListener(v -> showDatePicker());

        saveButton.setOnClickListener(v -> saveIncome());
    }

    private void showDialog(String title, String[] options, TextView target, Runnable afterSelect) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    target.setText(options[which]);
                    if (afterSelect != null) afterSelect.run();
                }).show();
    }

    private void resetSubcategory() {
        subcategoryValue.setText("Select Subcategory");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            String formatted = new SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
            dateValue.setText(formatted);
        }, y, m, d).show();
    }

    private String getTodayDate() {
        return new SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }

    private void saveIncome() {
        String amount = amountValue.getText().toString().trim();
        String note = noteValue.getText().toString().trim();
        String date = dateValue.getText().toString().trim();
        String recurrence = recurrenceValue.getText().toString().trim();
        String category = categoryValue.getText().toString().trim();
        String subcategory = subcategoryValue.getText().toString().trim();
        String payment = paymentValue.getText().toString().trim();

        if (amount.isEmpty()) {
            Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", amount);
        transaction.put("note", note.isEmpty() ? "Income recorded" : note);
        transaction.put("date", date);
        transaction.put("recurrence", recurrence);
        transaction.put("category", category);
        transaction.put("subcategory", subcategory);
        transaction.put("paymentMethod", payment);
        // Use proper date + time formatting
        String formattedTime = new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new java.util.Date());
        transaction.put("time", formattedTime);

        transaction.put("type", "Income");

        FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("transactions")
                .child(user.getUid())
                .push()
                .setValue(transaction)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Income saved successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
