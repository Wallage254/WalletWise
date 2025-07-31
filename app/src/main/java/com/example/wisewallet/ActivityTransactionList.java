package com.example.wisewallet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallet_wise.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityTransactionList extends AppCompatActivity {

    EditText searchField;
    RecyclerView recyclerView;
    BottomNavigationView bottomNavigationView;

    TextView totalIncomeView, totalExpenseView, tvDate, tvCategory, tvPayment, tvAmount;

    List<TransactionModel> transactionList;
    TransactionAdapter adapter;

    DatabaseReference dbRef;
    FirebaseUser currentUser;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        // Initialize Views
        searchField = findViewById(R.id.search_field);
        recyclerView = findViewById(R.id.recycler_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // NEW: Bind total income and expense views
        totalIncomeView = findViewById(R.id.totalIncome);
        totalExpenseView = findViewById(R.id.totalExpense);

        // NEW: Initialize the views
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvPayment = findViewById(R.id.tvPayment);
        TextView tvAmount = findViewById(R.id.tvAmount);

// Example click listeners
        tvDate.setOnClickListener(v -> queryTransactions("date"));
        tvCategory.setOnClickListener(v -> queryTransactions("category"));
        tvPayment.setOnClickListener(v -> queryTransactions("paymentMethod"));
        tvAmount.setOnClickListener(v -> queryTransactions("amount"));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList);
        recyclerView.setAdapter(adapter);

        // Get current user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase reference under /transactions/{uid}
        String uid = currentUser.getUid();
        dbRef = FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("transactions")
                .child(uid);
        dbRef.keepSynced(true);

        loadTransactions();

        // Search filter
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

    }

    private void loadTransactions() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();

                double totalIncome = 0;
                double totalExpense = 0;

                for (DataSnapshot snap : snapshot.getChildren()) {
                    TransactionModel model = snap.getValue(TransactionModel.class);
                    if (model != null && model.getType() != null) {
                        model.setId(snap.getKey());
                        if (model.getType().equalsIgnoreCase("Income")) {
                            transactionList.add(model);
                            totalIncome += parseAmount(model.getAmount());
                        } else if (model.getType().equalsIgnoreCase("Expense")) {
                            transactionList.add(model);
                            totalExpense += parseAmount(model.getAmount());
                        }
                    }
                }

                // Reverse to show latest transactions first
                Collections.reverse(transactionList);

                // Update totals
                totalIncomeView.setText("Total Income: Ksh " + formatAmount(totalIncome));
                totalExpenseView.setText("Total Spent: Ksh " + formatAmount(totalExpense));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ActivityTransactionList.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr.replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    private void filterList(String query) {
        List<TransactionModel> filtered = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase() : "";

        for (TransactionModel item : transactionList) {
            String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";
            String note = item.getNote() != null ? item.getNote().toLowerCase() : "";
            String date = item.getDate() != null ? item.getDate() : "";
            String type = item.getType() != null ? item.getType().toLowerCase() : "";

            if (category.contains(lowerQuery) ||
                    note.contains(lowerQuery) ||
                    date.contains(query) || // Keep date as-is because it's formatted
                    type.contains(lowerQuery)) {
                filtered.add(item);
            }
        }
            adapter = new TransactionAdapter(filtered);
            recyclerView.setAdapter(adapter);
//        adapter.updateList(filtered);  // Use adapter's update method instead of creating a new one
    }

    private void queryTransactions(String field) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child(user.getUid());

        // Order by field
        ref.orderByChild(field)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<TransactionModel> sortedList = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            TransactionModel model = child.getValue(TransactionModel.class);
                            if (model != null) {
                                sortedList.add(model);
                            }
                        }
                        // Update adapter
                        adapter.updateList(sortedList);
                        Toast.makeText(ActivityTransactionList.this,
                                "Sorted by " + field, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ActivityTransactionList.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }




}
