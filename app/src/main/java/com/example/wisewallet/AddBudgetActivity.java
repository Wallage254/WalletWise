package com.example.wisewallet;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AddBudgetActivity extends AppCompatActivity {

    private EditText amountInput;
    private Spinner categoryInput, subcategoryInput, periodInput;
    private Button saveButton;
    private DatabaseReference budgetRef;
    private Map<String, List<String>> categoryMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_budget);

        amountInput = findViewById(R.id.Editamount);
        categoryInput = findViewById(R.id.categorySpinner);
        subcategoryInput = findViewById(R.id.subCategorySpinner);
        periodInput = findViewById(R.id.periodSpinner);
        saveButton = findViewById(R.id.btn_save_budget);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        budgetRef = FirebaseDatabase.getInstance().getReference("budgets").child(uid);

        // Load categories from JSON
        categoryMap = CategoryData.loadCategories(this);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryMap.keySet().toArray(new String[0]));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryInput.setAdapter(categoryAdapter);

        categoryInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categoryInput.getSelectedItem().toString();
                List<String> subcats = categoryMap.get(selectedCategory);
                if (subcats != null) {
                    ArrayAdapter<String> subAdapter = new ArrayAdapter<>(
                            AddBudgetActivity.this, android.R.layout.simple_spinner_item, subcats);
                    subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    subcategoryInput.setAdapter(subAdapter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

//Period adapter
        List<String> periods = Arrays.asList("Daily", "Weekly", "Monthly", "Yearly");
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, periods
        );
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodInput.setAdapter(periodAdapter);



        saveButton.setOnClickListener(v -> saveBudget());
    }

    private void saveBudget() {
        String amount = amountInput.getText().toString().trim();
        String category = categoryInput.getSelectedItem().toString();
        String subcategory = subcategoryInput.getSelectedItem().toString();
        String period = periodInput.getSelectedItem().toString();

        if (amount.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        BudgetModel budget = new BudgetModel(category, subcategory, amount, period);
        String key = budgetRef.push().getKey();
        if (key != null) {
            budgetRef.child(key).setValue(budget)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Budget Saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
