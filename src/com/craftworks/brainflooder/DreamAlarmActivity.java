package com.craftworks.brainflooder;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

public class DreamAlarmActivity extends Activity {
    private Flashlight flash;
    private Music music;

    private int durationMinutes;
    private int alarmId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm);

        // allow to run on lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // get alarm ID
        alarmId = getIntent().getIntExtra("alarmId", 0);

        // load preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int fps = Integer.parseInt(preferences.getString("fps", "15"));
        boolean enableFlash = preferences.getBoolean("dreamAlarmFlash", false);
        boolean playMusic = preferences.getBoolean("dreamAlarmMusic", true);
        durationMinutes = Integer.parseInt(preferences.getString("dreamAlarmDuration", "2"));

        if (playMusic) {
            // find out music for selected dream package
            String dreamPackage = AppEnvironment.getDreamPackage(this.getApplication());
            File file = new File(AppEnvironment.getPath(this.getApplication()) + dreamPackage);
            if (file.exists() && file.isDirectory()) {
                File [] musicFiles = file.listFiles(new AppEnvironment.MusicFilter());

                if (musicFiles.length > 0) {
                    music = new Music();
                    music.start(musicFiles[0].getAbsolutePath());
                }
            }
        }

        if (enableFlash) {
            flash = new Flashlight(this);
            flash.strobe(1000 / fps);
        }

        // some useful data can be obtained as
        // String a1 = preferences.getString("dreamAlarm1time", "00:00");
        // ((TextView)findViewById(R.id.textView1)).setText(a1 + " alarms id " + alarmId);

        // auto kill alarm
        new CountDownTimer(durationMinutes * 60 * 1000, 1000) {
            int remaining = durationMinutes * 60;

            @Override
            public void onTick(long l) {
                ((TextView)findViewById(R.id.textView1)).setText("Alarm #" + (alarmId + 1) + ", remaining: " + remaining);
                remaining--;
            }

            @Override
            public void onFinish() {
                finish();
            }
        }.start();
    }

    public void onSnooze(View v) {
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (music != null)
            music.stop();

        if (flash != null)
            flash.release();

        // allow device to go to sleep again
        WakeLocker.release();
    }
}