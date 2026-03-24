package com.example.monitoring_automate_simple;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class MonitoringDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {}

    @Override
    public void onDisabled(Context context, Intent intent) {}
}
