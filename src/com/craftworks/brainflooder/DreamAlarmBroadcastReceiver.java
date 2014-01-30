package com.craftworks.brainflooder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.util.Log;

public class DreamAlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // acquire wake lock
        // remember to release it when activity closes
        WakeLocker.acquire(context);

        Intent alarmIntent = new Intent(context, DreamAlarmActivity.class);

        // forward extras to activity
        alarmIntent.putExtra("alarmId", intent.getIntExtra("alarmId", 0));

        //start activity
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmIntent);
    }
}