package com.example.wisewallet;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wallet_wise.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;

public class ActivityIncome extends AppCompatActivity {

    EditText amountValue;
    TextView dateValue, noteValue, recurrenceValue, paymentValue;
    TextView saveButton;
    TabLayout tabLayout;

    private String selectedType = "Expense"; // Default
    private DatabaseReference databaseRef;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_transaction); // use your XML filename

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            databaseRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                    .getReference("transactions").child(user.getUid());
        } else {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        amountValue = findViewById(R.id.amountValue);
        dateValue = findViewById(R.id.dateValue);
        noteValue = findViewById(R.id.noteValue);
        recurrenceValue = findViewById(R.id.recurrenceValue);
        paymentValue = findViewById(R.id.paymentValue);
        saveButton = findViewById(R.id.saveButton);

        // Listen for tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedType = tab.getText().toString(); // Expense, Income, Transfer
                Toast.makeText(ActivityIncome.this, "Selected: " + selectedType, Toast.LENGTH_SHORT).show();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Save Button
        saveButton.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        String amount = amountValue.getText().toString().trim();
        String date = dateValue.getText().toString().trim();
        String note = noteValue.getText().toString().trim();
        String recurrence = recurrenceValue.getText().toString().trim();
        String payment = paymentValue.getText().toString().trim();

        if (amount.isEmpty()) {
            amountValue.setError("Enter amount");
            return;
        }

        // Dummy category for now
        String category = selectedType.equals("Income") ? "Salary" :
                selectedType.equals("Transfer") ? "Bank Transfer" : "Shopping";

        HashMap<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("date", date);
        data.put("note", note);
        data.put("recurrence", recurrence);
        data.put("type", selectedType);
        data.put("category", category);
        data.put("paymentMethod", payment.isEmpty() ? "Bank" : payment);

        String key = databaseRef.push().getKey();
        if (key != null) {
            databaseRef.child(key).setValue(data)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Transaction saved as " + selectedType, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}

