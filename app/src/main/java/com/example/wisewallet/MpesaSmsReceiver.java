package com.example.wisewallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class MpesaSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;

            for (Object pdu : pdus) {
                SmsMessage smsMessage;
                String format = bundle.getString("format");
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);

                String messageBody = smsMessage.getMessageBody();
                String sender = smsMessage.getDisplayOriginatingAddress();

                if (sender != null && sender.toLowerCase().contains("mpesa")) {
                    Log.d("MpesaReceiver", "M-Pesa SMS Detected: " + messageBody);
                    MpesaSmsParser.parseAndSaveTransaction(context, messageBody);
                }
            }

        }
    }
}
