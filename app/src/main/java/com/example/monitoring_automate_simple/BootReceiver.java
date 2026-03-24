package com.example.monitoring_automate_simple;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Se déclenche au démarrage du système et lance MainActivity.
 * Requiert la permission RECEIVE_BOOT_COMPLETED dans le manifest.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }
    }
}
