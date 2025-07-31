package com.example.wisewallet;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<BudgetModel> budgetList;
    private Context context;
    private static final String CHANNEL_ID = "budget_notification_channel";

    public BudgetAdapter(Context context, List<BudgetModel> budgetList) {
        this.context = context;
        this.budgetList = budgetList;
        createNotificationChannel();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetModel item = budgetList.get(position);

        holder.categoryText.setText(item.getCategory());
        if (holder.subCategoryText != null)
            holder.subCategoryText.setText(item.getSubcategory());

        // Format budget amount (stored as string) to two decimal places
        try {
            double amount = Double.parseDouble(item.getAmount());
            holder.amountText.setText(String.format("Sh%.2f", amount));
        } catch (NumberFormatException e) {
            holder.amountText.setText("Invalid amount");
        }

        if (holder.dateText != null)
            holder.dateText.setText(item.getBudgetPeriod());

        fetchTotalExpense(holder, item);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText, subCategoryText, amountText, dateText, ratioText, exceededText;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(R.id.textCategory);
            subCategoryText = itemView.findViewById(R.id.textSubCategory);
            amountText = itemView.findViewById(R.id.textSpent);
            dateText = itemView.findViewById(R.id.textDateRange);
            ratioText = itemView.findViewById(R.id.textRatio);
            exceededText = itemView.findViewById(R.id.textExceed);
        }
    }

    private void fetchTotalExpense(BudgetViewHolder holder, BudgetModel item) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child(currentUser.getUid());

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalExpense = 0;

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String type = snap.child("type").getValue(String.class);
                    if ("Expense".equalsIgnoreCase(type)) {
                        String amountStr = snap.child("amount").getValue(String.class);
                        if (amountStr != null && !amountStr.isEmpty()) {
                            try {
                                double amount = Double.parseDouble(amountStr);
                                totalExpense += amount;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                try {
                    double budgetAmount = Double.parseDouble(item.getAmount());
                    holder.ratioText.setText(String.format("T.E/B.A: %.2f / %.2f", totalExpense, budgetAmount));

                    if (totalExpense > budgetAmount) {
                        double exceeded = totalExpense - budgetAmount;
                        holder.exceededText.setText(String.format("Exceeded: Sh%.2f", exceeded));
                        sendExceededNotification(exceeded, item.getCategory());
                    } else {
                        holder.exceededText.setText("Within Budget");
                    }
                } catch (NumberFormatException e) {
                    holder.ratioText.setText("Invalid budget amount");
                    holder.exceededText.setText("Cannot calculate");
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.exceededText.setText("Error loading expense");
            }
        });
    }

    private void sendExceededNotification(double exceededAmount, String category) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wallet)
                .setContentTitle("Budget Exceeded")
                .setContentText("Your budget for " + category + " exceeded by Sh" + String.format("%.2f", exceededAmount))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Budget Channel";
            String description = "Alerts for budget exceed";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
