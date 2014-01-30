package com.craftworks.brainflooder;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class SubliminalEditorActivity  extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subliminals);
    }

    @Override
    public void onResume() {
        super.onResume();

        String messages = "";
        File file = new File(AppEnvironment.getPath(this.getApplication()) + AppEnvironment.getDreamPackage(this.getApplication()) + "/subliminal.txt");
        if (file.exists()) {
            // read all text
            try {
                StringBuffer fileData = new StringBuffer();
                BufferedReader reader = new BufferedReader(new FileReader(file));

                char[] buf = new char[1024];
                int numRead = 0;
                while((numRead = reader.read(buf)) != -1) {
                    fileData.append(buf, 0, numRead);
                }

                reader.close();
                messages = fileData.toString();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        ((TextView)findViewById(R.id.subliminalsEditText)).setText(messages);
    }

    public void onSave(View v) {
        String message = ((TextView)findViewById(R.id.subliminalsEditText)).getText().toString();

        File file = new File(AppEnvironment.getPath(this.getApplication()) + AppEnvironment.getDreamPackage(this.getApplication()) + "/subliminal.txt");
        try {
            PrintWriter out = new PrintWriter(file);
            out.print(message);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        this.finish();
    }
}
