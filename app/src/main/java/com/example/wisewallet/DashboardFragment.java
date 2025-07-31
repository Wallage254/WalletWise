package com.example.wisewallet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DashboardFragment extends Fragment {

    private ImageButton totalAmountBtn, totalSpendingBtn, transactionsBtn, budgetBtn;
    private ImageButton goalsBtn, insightsBtn, currencyConverterBtn, receiptScanner;
    private ImageView profileImage;
    private TextView walletBalance;

    DatabaseReference budgetRef;
    FirebaseAuth mAuth;

    private Map<String, String[]> categoryMap;

    public DashboardFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        totalAmountBtn = view.findViewById(R.id.total_amount);
        totalSpendingBtn = view.findViewById(R.id.total_spending);
        transactionsBtn = view.findViewById(R.id.transactions);
        budgetBtn = view.findViewById(R.id.budget);
        goalsBtn = view.findViewById(R.id.goals);
        insightsBtn = view.findViewById(R.id.insights);
        currencyConverterBtn = view.findViewById(R.id.currency_converter);
        receiptScanner = view.findViewById(R.id.scanner);
        profileImage = view.findViewById(R.id.profile_image);

        walletBalance = view.findViewById(R.id.wallet_balance);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // Redirect to login activity or show a message
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        budgetRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("transactions")
                .child(mAuth.getCurrentUser().getUid());

//        Load the profile Image from shared preferences
        SharedPreferences pref = requireContext().getSharedPreferences("user_profile_prefs", Activity.MODE_PRIVATE);
        String uriString = pref.getString("profile_image_uri", null);
        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            Glide.with(this).load(imageUri).circleCrop().into(profileImage);
        }


        totalAmountBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), ActivityAddTransaction.class)));
        totalSpendingBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), ActivityTransactionList.class)));
        transactionsBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), ActivityBudget.class)));
        budgetBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddBudgetActivity.class)));
        goalsBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), GoalsInsightsActivity.class)));
        insightsBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), GoalsInsightsActivity.class)));
        currencyConverterBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), CurrencyConverterActivity.class)));

        categoryMap = loadCategoryMappingFromAssets();

        receiptScanner.setOnClickListener(v -> scannedReceipt());
        calculateBalance();
    }

    private final ActivityResultLauncher<Intent> receiptLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        processReceiptImage(imageUri);
                    }
                }
            });

    private void scannedReceipt() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        receiptLauncher.launch(intent);
    }


    private void processReceiptImage(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String fullText = visionText.getText();
                        extractItemsFromText(fullText);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to recognize text", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractItemsFromText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            Toast.makeText(getContext(), "No text found in image", Toast.LENGTH_SHORT).show();
            return;
        }

        List<TransactionModel> extractedItems = new ArrayList<>();
        Pattern linePattern = Pattern.compile("^(.*?)\\s+(\\d{1,5}(?:[.,]\\d{2})?)$");

        String[] lines = fullText.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Skip unwanted summary lines
            if (line.isEmpty() || line.matches("(?i).*(total|vat|cash|balance|change).*")) {
                continue;
            }

            Matcher matcher = linePattern.matcher(line);
            if (matcher.find()) {
                String itemName = matcher.group(1).trim();
                String priceStr = matcher.group(2).replace(",", "."); // Normalize price
                try {
                    double price = Double.parseDouble(priceStr);

                    // Get category + subcategory
                    String[] catData = categorizeItem(itemName);
                    String category = catData[0];
                    String subcategory = catData[1];

                    extractedItems.add(new TransactionModel(itemName, category, subcategory, String.valueOf(price)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        if (extractedItems.isEmpty()) {
            Toast.makeText(getContext(), "No items matched. Try a clearer photo.", Toast.LENGTH_LONG).show();
        } else {
            saveTransactionsToFirebase(extractedItems);
        }
    }




    //Save transactions to firebase
    private void saveTransactionsToFirebase(List<TransactionModel> items) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (TransactionModel item : items) {
            String transactionId = dbRef.child("transactions").child(userId).push().getKey();
            Map<String, Object> data = new HashMap<>();
            data.put("name", item.getName());
            data.put("category", item.getCategory());
            data.put("subcategory", item.getSubcategory());
            data.put("amount", item.getAmount());
            data.put("type", "Expense");
            data.put("date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));

            dbRef.child("transactions").child(userId).child(transactionId).setValue(data);
        }

        Toast.makeText(getContext(), "Scanned items added as expenses", Toast.LENGTH_SHORT).show();
    }


    public String[] categorizeItem(String itemName) {
        itemName = itemName.toLowerCase();

        for (String keyword : categoryMap.keySet()) {
            if (itemName.contains(keyword)) {
                return categoryMap.get(keyword);
            }
        }
        return new String[]{"Uncategorized", "Miscellaneous"};
    }


    private String categorizeSubItem(String item) {
        item = item.toLowerCase();
        if (item.contains("milk")) return "Dairy";
        if (item.contains("soap")) return "Body Wash";
        return "General";
    }

    private Map<String, String[]> loadCategoryMappingFromAssets() {
        Map<String, String[]> categoryMap = new HashMap<>();
        try {
            InputStream is = requireContext().getAssets().open("categories.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonStr);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = jsonObject.getJSONArray(key);
                categoryMap.put(key.toLowerCase(), new String[]{array.getString(0), array.getString(1)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return categoryMap;
    }

//Calculate user Balance
    private void calculateBalance() {
        budgetRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalIncome = 0;
                double totalExpense = 0;

                for (DataSnapshot item : snapshot.getChildren()) {
                    String type = item.child("type").getValue(String.class);
                    String amountStr = item.child("amount").getValue(String.class);

                    if (amountStr != null && type != null) {
                        double amount = Double.parseDouble(amountStr); // ✅ Use double here
                        if (type.equalsIgnoreCase("Income")) {
                            totalIncome += amount;
                        } else if (type.equalsIgnoreCase("Expense")) {
                            totalExpense += amount;
                        }
                    }
                }

                double balance = totalIncome - totalExpense;
                walletBalance.setText("Ksh " + String.format("%,.2f", balance)); // Optional: formatted output
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }



}
