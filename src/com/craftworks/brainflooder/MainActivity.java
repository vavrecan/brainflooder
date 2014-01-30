package com.craftworks.brainflooder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class MainActivity extends Activity {
    private DreamAlarmManager alarmManager;
    private final int PICK_MUSIC = 101;
    private final int MENU_SUBLIMINALS = 1;
    private final int MENU_MUSIC = 2;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // set default values
        PreferenceManager.setDefaultValues(this.getApplicationContext(), R.xml.preferences, false);

        // oh epilepsy warning!
        promptEpilepsyWarning();

        // passed as extra to activity, save as selected dream
        String dreamPackage = getIntent().getStringExtra("package");
        if (dreamPackage != null)
            AppEnvironment.setDreamPackage(getApplication(), dreamPackage);

        // read selected dream from preferences
        dreamPackage = AppEnvironment.getDreamPackage(getApplication());
        if (dreamPackage != null) {
            // dream is selected, update UI
            ((TextView)findViewById(R.id.selectedDreamText)).setText(dreamPackage);
        } else {
            // no dream package selected, disable some UI controls
            ((Button)findViewById(R.id.flood)).setEnabled(false);
            ((Button)findViewById(R.id.previewButton)).setEnabled(false);
        }

        alarmManager = new DreamAlarmManager(this);

        // check if alarms are set
        updateScheduledAlarms(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no menu if no dream selected
        if (AppEnvironment.getDreamPackage(getApplication()) == null)
            return super.onCreateOptionsMenu(menu);

        menu.add(1, MENU_SUBLIMINALS, MENU_SUBLIMINALS, "Change Subliminals");
        menu.add(1, MENU_MUSIC, MENU_MUSIC, "Change Music");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SUBLIMINALS: {

                // show change subliminal activity
                Intent intent = new Intent(this, SubliminalEditorActivity.class);
                startActivity(intent);

            }
            break;
            case MENU_MUSIC: {
                // show change music activity
                // remove all music from path
                // get mp3 or wav and copy it to the directory

                Intent musicPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(musicPickerIntent, "Music"), PICK_MUSIC);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_MUSIC: {
                if (resultCode == RESULT_OK) {
                    // update music file
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Audio.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    updateDreamMusic(filePath);
                }
            }
            break;
        }
    }

    private void updateDreamMusic(String filePath) {
        // remove all other music
        File file = new File(AppEnvironment.getPath(this.getApplication()) + AppEnvironment.getDreamPackage(this.getApplication()));
        File [] musicFiles = file.listFiles(new AppEnvironment.MusicFilter());
        for (File f : musicFiles) {
            f.delete();
        }

        // copy into dream
        File source = new File(filePath);
        File target = new File(AppEnvironment.getPath(this.getApplication()) + AppEnvironment.getDreamPackage(this.getApplication()) + "/" + source.getName());
        AppEnvironment.copyFile(source, target);
    }

    public void onFlood(View v)  {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int duration = Integer.parseInt(preferences.getString("duration", "3"));
        int fps = Integer.parseInt(preferences.getString("fps", "15"));
        boolean durationEnabled = preferences.getBoolean("durationEnable", true);
        boolean maxBrightness = preferences.getBoolean("maxBrightness", true);
        boolean playMusic = preferences.getBoolean("playMusic", true);
        float subliminalOccurrence = Float.parseFloat(preferences.getString("subliminalOccurrence", "0.2"));

        // no duration, set time limit to 0
        if (!durationEnabled)
            duration = 0;

        // start flooding intent
        Intent intent = new Intent(v.getContext(), FloodActivity.class);
        intent.putExtra("fps", fps);
        intent.putExtra("duration", duration);
        intent.putExtra("playMusic", playMusic);
        intent.putExtra("maxBrightness", maxBrightness);
        intent.putExtra("subliminalOccurrence", subliminalOccurrence);
        intent.putExtra("package", AppEnvironment.getDreamPackage(getApplication()));
        startActivity(intent);
    }

    public void onPickDream(View v) {
        startActivity(new Intent(this, DreamListActivity.class));
    }

    public void onPreferences(View v)  {
        startActivity(new Intent(this, PreferencesActivity.class));
    }

    /**
     * Update view if alarms are scheduled
     * TODO Maybe it would be useful to show notification too
     */
    public void updateScheduledAlarms(boolean toast) {
        List<DreamAlarmManager.ScheduledAlarm> alarms = alarmManager.getScheduledAlarms();
        String toastMessage = "";
        if (alarms.size() > 0) {
            ((ToggleButton)findViewById(R.id.toggleButton)).setChecked(true);

            // get info about alarms
            for (int i = 0; i < alarms.size(); i++) {
                if (toastMessage != "") toastMessage += "\n";
                toastMessage += "Alarm #" + (alarms.get(i).getId() + 1) + " will trigger in " + alarms.get(i).getTimeLeft();
            }
        }
        else {
            ((ToggleButton)findViewById(R.id.toggleButton)).setChecked(false);
            toastMessage = "No scheduled alarms";
        }

        if (toastMessage != "" && toast) {
            toast(toastMessage);
        }
    }

    public void onPreviewButton(View v) {
        Intent intent = new Intent(v.getContext(), DreamAlarmActivity.class);
        intent.putExtra("alarmId", 0);
        startActivity(intent);
    }

    public void onAlarmToggle(View v) {
        if (((ToggleButton)v).isChecked())
            alarmManager.createAlarms();
        else
            alarmManager.cancelAlarms();

        updateScheduledAlarms(true);
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void promptEpilepsyWarning() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean agreed = preferences.getBoolean("epilepsy", false);

        if (agreed)
            return;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:

                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                        edit.putBoolean("epilepsy", true);
                        edit.commit();

                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        finish();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application is flashing images which may not be suitable for photosensitive epilepsy. Use at own risk.")
                .setPositiveButton("I agree", dialogClickListener)
                .setNegativeButton("Exit", dialogClickListener).show();
    }
}
