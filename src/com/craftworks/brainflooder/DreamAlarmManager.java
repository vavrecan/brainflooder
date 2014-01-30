package com.craftworks.brainflooder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DreamAlarmManager {
    private Context context;
    private int alarmsCount = 3;

    public DreamAlarmManager(Context c) {
        context = c;
    }

    public void createAlarms() {
        cancelAlarms();

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor edit = preferences.edit();

        // set alarms
        for (int i = 0; i < alarmsCount; i++) {
            boolean enable = preferences.getBoolean("dreamAlarm" + i, false);
            if (enable) {
                String time = preferences.getString("dreamAlarm" + i + "time", "00:00");
                Calendar c = Calendar.getInstance();

                // parse out time
                String[] pieces = time.split(":");
                int hours = Integer.parseInt(pieces[0]);
                int minutes = Integer.parseInt(pieces[1]);
                long now = c.getTimeInMillis();

                c.set(Calendar.HOUR_OF_DAY, hours);
                c.set(Calendar.MINUTE, minutes);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);

                // make sure we are not setting time in past, rather set alarm on tomorrow (setting alarm on 4:00 after 0:00)
                if (c.getTimeInMillis() < now)
                    c.add(Calendar.HOUR, 24);

                // create alarm
                Intent intent = new Intent(context, DreamAlarmBroadcastReceiver.class);
                intent.putExtra("alarmId", i);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);

                // save this time as mark to alarm in in progress
                edit.putLong("dreamAlarm" + i + "schedule", c.getTimeInMillis());
            }
        }

        edit.commit();
    }

    public void cancelAlarms() {
        Intent intent = new Intent(context, DreamAlarmBroadcastReceiver.class);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor edit = preferences.edit();

        // kill all alarms
        for (int i = 0; i < alarmsCount; i++) {
            pendingIntent = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_CANCEL_CURRENT);   // cancel first intent, second later for test.
            alarmManager.cancel(pendingIntent);
            edit.putLong("dreamAlarm" + i + "schedule", 0);
        }

        edit.commit();
    }

    /**
     * Get alarms from settings or show no alarm activated
     */
    public List<ScheduledAlarm> getScheduledAlarms() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        List<ScheduledAlarm> alarms = new ArrayList<ScheduledAlarm>();

        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();

        for (int i = 0; i < alarmsCount; i++) {
            long time = preferences.getLong("dreamAlarm" + i + "schedule", 0);

            // check if they are set and were not yet executed
            if (time > 0 && time >= now)
                alarms.add(new ScheduledAlarm(i, time));
        }

        return alarms;
    }

    public class ScheduledAlarm {
        public long time;
        public int id;

        public ScheduledAlarm(int id, long time) {
            this.time = time;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public String getTimeLeft() {
            long now = Calendar.getInstance().getTimeInMillis();

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            long alarm = c.getTimeInMillis();
            long timeRemaining = alarm - now;

            long sec = (timeRemaining/1000) % 60;
            long min = (timeRemaining/(1000*60)) % 60;
            long hours = (timeRemaining/(1000*60*60)) % 24;

            String format = hours + " hr";
            format += " " + min + " min";
            //format += " " + sec + " sec";

            return format;
        }
    }
}
