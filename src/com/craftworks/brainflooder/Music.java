package com.craftworks.brainflooder;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

public class Music {
    private MediaPlayer mp;
    private Runnable decreaseVol;

    public void stop()
    {
        // create fade out effect
        try {
            final Handler h = new Handler();
            decreaseVol = new Runnable() {
                float volume = 1f;

                public void run(){
                    if (mp == null)
                        return;

                    mp.setVolume(volume, volume);
                    if (volume > 0f){
                        volume -= .05f;
                        h.postDelayed(decreaseVol, 80);
                    }
                    else {
                        mp.stop();
                        mp = null;
                    }
                }
            };
            h.post(decreaseVol);
        }
        catch (IllegalStateException e) {
            Log.e("com.craftworks.brainflooder", e.toString());
        }
    }

    public void start(String path)
    {
        try {
            mp = new MediaPlayer();
            mp.setVolume(1f, 1f);
            mp.setDataSource(path);
            mp.prepare();
            mp.setLooping(true);
            mp.start();
        } catch (Exception e) {
            Log.e("com.craftworks.brainflooder", e.toString());
        }
    }
}
