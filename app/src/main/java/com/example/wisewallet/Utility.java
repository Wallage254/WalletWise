package com.example.wisewallet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.wallet_wise.R;

public class Utility {

    public static void sendTransactionNotification(Context context, TransactionModel transaction) {
        PreferencesManager prefs = new PreferencesManager(context);
//        if (!prefs.isPremium()) return;

        String channelId = "transaction_channel";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Transaction Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Custom view
        RemoteViews customView = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
        customView.setTextViewText(R.id.transactionType, transaction.getType() + " Transaction");
        customView.setTextViewText(R.id.transactionDetails,
                transaction.getCategory() + " - KSh " + transaction.getAmount());

        // Different color backgrounds for each type
        int bgColor;
        if ("Income".equalsIgnoreCase(transaction.getType())) {
            bgColor = ContextCompat.getColor(context, R.color.greenAccent);
        } else if ("Expense".equalsIgnoreCase(transaction.getType())) {
            bgColor = ContextCompat.getColor(context, R.color.redAccent);
        } else {
            bgColor = ContextCompat.getColor(context, R.color.blueAccent);
        }
        customView.setInt(R.id.transactionType, "setTextColor", bgColor);

        PendingIntent intent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_wallet)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(customView)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(intent);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }


}
