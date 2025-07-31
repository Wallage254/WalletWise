package com.example.wisewallet;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class RecurringTransactionWorker extends Worker {

    public RecurringTransactionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("transactions")
                    .child(user.getUid());

            Query query = ref.orderByChild("recurrence").startAt("Weekly").endAt("Monthly");
            DataSnapshot snapshot = Tasks.await(query.get());

            for (DataSnapshot child : snapshot.getChildren()) {
                TransactionModel model = child.getValue(TransactionModel.class);
                if (model != null && isDue(model)) {
                    sendDueNotification(model);
                    saveToTransactions(user.getUid(), model);

                    // Update last processed date
                    child.getRef().child("lastProcessedDate")
                            .setValue(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
                }
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private boolean isDue(TransactionModel model) {
        try {
            Date lastDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .parse(model.getLastProcessedDate());
            Calendar last = Calendar.getInstance();
            assert lastDate != null;
            last.setTime(lastDate);
            Calendar now = Calendar.getInstance();

            switch (model.getRecurrence()) {
                case "Daily":
                    // Run every day but ensure it's not already processed today
                    return now.get(Calendar.YEAR) != last.get(Calendar.YEAR)
                            || now.get(Calendar.DAY_OF_YEAR) != last.get(Calendar.DAY_OF_YEAR);

                case "Weekly":
                    return now.get(Calendar.DAY_OF_WEEK) == last.get(Calendar.DAY_OF_WEEK)
                            && now.get(Calendar.WEEK_OF_YEAR) != last.get(Calendar.WEEK_OF_YEAR);

                case "Monthly":
                    return now.get(Calendar.DAY_OF_MONTH) == last.get(Calendar.DAY_OF_MONTH)
                            && now.get(Calendar.MONTH) != last.get(Calendar.MONTH);

                case "Custom":
                    // Premium feature - check if premium unlocked
                    if (isPremiumUser()) {
                        return isCustomDue(model, last, now);
                    } else {
                        sendPremiumFeatureNotification();
                        return false;
                    }

                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPremiumUser() {
        // TODO: Replace with actual premium check (e.g., from Firestore or SharedPreferences)
        return false;
    }

    private boolean isCustomDue(TransactionModel model, Calendar last, Calendar now) {
        // Example: Custom recurrence every X days
        int customDays = 5; // For example
        last.add(Calendar.DAY_OF_YEAR, customDays);
        return !now.before(last);
    }

    private void sendPremiumFeatureNotification() {
        String channelId = "premium_feature_channel";
        createNotificationChannel(channelId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Premium Feature")
                .setContentText("Custom recurrence is available in Premium only.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }



    private void sendDueNotification(TransactionModel model) {
        String channelId = "recurring_bills_channel";
        createNotificationChannel(channelId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Bill Due")
                .setContentText(model.getName() + " of " + model.getAmount() + " is due!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Recurring Bills",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void saveToTransactions(String uid, TransactionModel model) {
        DatabaseReference transactionRef = FirebaseDatabase.getInstance()
                .getReference("transactions").child(uid).push();

        HashMap<String, Object> data = new HashMap<>();
        data.put("amount", model.getAmount());
        data.put("category", model.getCategory());
        data.put("subcategory", model.getSubcategory());
        data.put("note", model.getNote());
        data.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        data.put("time", new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
        data.put("type", model.getType());
        data.put("recurrence", model.getRecurrence());
        data.put("lastProcessedDate", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        transactionRef.setValue(data);
    }
}
