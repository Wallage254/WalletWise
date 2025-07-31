package com.example.wisewallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MpesaSmsParser {

    public static void parseAndSaveTransaction(Context context, String smsBody) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String lower = smsBody.toLowerCase(Locale.ROOT);

            boolean isIncome = lower.contains("received") || lower.contains("sent you") || lower.contains("give you") || lower.contains("credited");
            boolean isExpense = lower.contains("sent to") || lower.contains("paid to") || lower.contains("buy goods") || lower.contains("pochi la biashara");

            // Extract amount
            Pattern amountPattern = Pattern.compile("ksh\\s?([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
            Matcher amountMatcher = amountPattern.matcher(smsBody);
            String amount = amountMatcher.find() ? Objects.requireNonNull(amountMatcher.group(1)).replace(",", "") : "0";

            // Extract recipient/payee (if available)
            String subcategory = "M-Pesa";
            Matcher payeeMatcher = Pattern.compile("(?:to|from)\\s(.+?)\\son", Pattern.CASE_INSENSITIVE).matcher(smsBody);
            if (payeeMatcher.find()) {
                subcategory = Objects.requireNonNull(payeeMatcher.group(1)).trim();
            }

            String type = isIncome ? "Income" : isExpense ? "Expense" : "Other";
            if (type.equals("Other")) return;

            String date = new SimpleDateFormat("EEE. dd/MM/yyyy", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            HashMap<String, Object> data = new HashMap<>();
            data.put("amount", amount);
            data.put("date", date);
            data.put("note", smsBody);
            data.put("recurrence", "None");
            data.put("category", "M-Pesa");
            data.put("subcategory", subcategory);
            data.put("paymentMethod", "M-Pesa");
            data.put("time", time);
            data.put("type", type);

            if (!NetworkUtils.isNetworkAvailable(context)) {
                saveTransactionOffline(context, data);
                Toast.makeText(context, "M-Pesa saved offline", Toast.LENGTH_SHORT).show();
            } else {
                pushToFirebase(user.getUid(), data, context);
            }
        } catch (Exception e) {
            Log.e("M-Pesa", "Parse error: " + e.getMessage());
        }
    }

    private static void pushToFirebase(String userId, HashMap<String, Object> data, Context context) {
        FirebaseDatabase.getInstance("https://wallet-wise-387c2-default-rtdb.firebaseio.com/")
                .getReference("transactions")
                .child(userId)
                .push()
                .setValue(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(context, "M-Pesa transaction saved", Toast.LENGTH_SHORT).show();

                    // Convert HashMap to model for notification
                    TransactionModel model = new TransactionModel();
                    model.setAmount((String) data.get("amount"));
                    model.setCategory((String) data.get("category"));
                    model.setSubcategory((String) data.get("subcategory"));
                    model.setDate((String) data.get("date"));
                    model.setNote((String) data.get("note"));
                    model.setPaymentMethod((String) data.get("paymentMethod"));
                    model.setType((String) data.get("type"));

                    Utility.sendTransactionNotification(context, model); // <-- HERE
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error: " + e.getMessage()));
    }


    private static void saveTransactionOffline(Context context, HashMap<String, Object> transaction) {
        SharedPreferences prefs = context.getSharedPreferences("offline_mpesa", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String existing = prefs.getString("transactions", "[]");
        Type listType = new TypeToken<List<HashMap<String, Object>>>() {}.getType();
        List<HashMap<String, Object>> list = gson.fromJson(existing, listType);

        if (list == null) list = new ArrayList<>();
        list.add(transaction);

        prefs.edit().putString("transactions", gson.toJson(list)).apply();

        // Send notification for offline as well
        TransactionModel model = new TransactionModel();
        model.setAmount((String) transaction.get("amount"));
        model.setCategory((String) transaction.get("category"));
        model.setSubcategory((String) transaction.get("subcategory"));
        model.setDate((String) transaction.get("date"));
        model.setNote((String) transaction.get("note"));
        model.setPaymentMethod((String) transaction.get("paymentMethod"));
        model.setType((String) transaction.get("type"));

        Utility.sendTransactionNotification(context, model);
    }


    public static void syncOfflineTransactions(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("offline_mpesa", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String offlineData = prefs.getString("transactions", "[]");
        Type listType = new TypeToken<List<HashMap<String, Object>>>() {}.getType();
        List<HashMap<String, Object>> list = gson.fromJson(offlineData, listType);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || list == null || list.isEmpty()) return;

        for (HashMap<String, Object> item : list) {
            pushToFirebase(user.getUid(), item, context);
        }

        prefs.edit().remove("transactions").apply();
        Toast.makeText(context, "Offline M-Pesa transactions synced", Toast.LENGTH_SHORT).show();
    }
}

