package com.craftworks.brainflooder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.TextView;

import java.io.*;
import java.util.*;

/**
 * Main floodThread activity that will hold surface
 */
public class FloodActivity extends Activity {
    private FloodSurface surface;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        surface = new FloodSurface(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(surface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        surface.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surface.pause();
    }

    public class CacheUpdater implements Runnable {
        public List<File> imageFiles;

        boolean isRunning = false;
        boolean isError = false;
        String lastError = "";

        Thread cacheUpdateThread = null;
        String packageDirectory;

        /**
         * is cache ready to be play?
         */
        public boolean isReady = false;

        /**
         * Cache storage
         */
        Bitmap[] bitmapCache;
        int bitmapMaxCacheSize = 10;

        /**
         * Default image bounding, overwrite with canvas size
         */
        int imageBoundHeight = 320;
        int imageBoundWidth = 240;

        Context mContext;

        public CacheUpdater(Context context) {
            bitmapCache = new Bitmap[bitmapMaxCacheSize];
            mContext = context;
        }

        public void setPackageDirectory(String packageDirectory) {
           this.packageDirectory = packageDirectory;
        }

        public void setImages(List<File> images) {
            imageFiles = images;
        }

        public void setImageBounds(int width, int height) {
            imageBoundHeight = height;
            imageBoundWidth = width;
        }

        public void run() {
            // read images to cache
            int ticker = 0;
            int offset = 0;
            while (isRunning) {
                // Hups! empty image array
                if (imageFiles == null || imageFiles.size() == 0) {
                    isError = true;
                    lastError = "Images are missing in " + AppEnvironment.getPath(((Activity) mContext).getApplication()) + packageDirectory + " directory!";
                    return;
                }

                // load scaled image
                File image = imageFiles.get(ticker % imageFiles.size());
                Bitmap b = loadImageAsScaledBitmap(image);

                if (b != null) {
                    // lets be thread safe
                    synchronized(bitmapCache) {
                        // try to release
                        if (bitmapCache[offset] != null) {
                            bitmapCache[offset].recycle();
                            bitmapCache[offset] = null;
                        }

                        // save to cache
                        bitmapCache[offset++] = b;

                        // set ready signal if bitmap cache was fully filled
                        if (offset == bitmapMaxCacheSize) {
                            offset = 0;
                            isReady = true;
                            isError = false;

                            // collect garbage, I do not like mess
                            System.gc();
                        }
                    }
                }

                // avoid CPU headache
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ticker++;
            }
        }

        public Bitmap getImageFromTick(int ticker) {
            synchronized(bitmapCache) {
                int image = ticker % bitmapMaxCacheSize;
                return bitmapCache[image];
            }
        }

        private Bitmap loadImageAsScaledBitmap(File image) {
            try  {
                BitmapFactory.Options bfOptions = new BitmapFactory.Options();
                bfOptions.inPurgeable = true;
                bfOptions.inDither = false;
                bfOptions.inInputShareable = false;

                Bitmap b = BitmapFactory.decodeFile(image.getAbsolutePath(), bfOptions);
                Bitmap scaled = AppEnvironment.resizeBitmap(b, imageBoundWidth, imageBoundHeight, false);

                if (scaled != null) {
                    b.recycle();
                    b = null;
                    return scaled;
                }

                return b;
            }
            catch (Exception e) {
                if (!isReady) {
                    isError = true;
                    lastError = "Image " + image.getAbsolutePath() + " is invalid";
                }
            }

            return null;
        }

        public void pause() {
            isRunning = false;

            while (true) {
                try {
                    cacheUpdateThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }

            // cleanup
            imageFiles = null;
            cacheUpdateThread = null;
        }

        public void resume() {
            isRunning = true;
            cacheUpdateThread = new Thread(this);
            cacheUpdateThread.setPriority(Thread.NORM_PRIORITY);
            cacheUpdateThread.start();
        }
    }

    public class FloodSurface extends SurfaceView implements Runnable {

        Thread floodThread = null;

        SurfaceHolder holder;
        CacheUpdater cacheUpdater;

        boolean isRunning = false;
        boolean playMusic = true;
        Context mContext;

        private Music music;
        private int fps = 20;
        private int duration = 0;
        private long startTime;
        private float subliminalOccurrence;
        private String packageDirectory;

        private List<String> subliminals;

        public FloodSurface(Context context) {
            super(context);
            mContext = context;
            holder = getHolder();
            music = new Music();
            subliminals = new ArrayList<String>();
            cacheUpdater = new CacheUpdater(mContext);

            // get parameters from extra
            Intent intent = getIntent();
            fps = intent.getIntExtra("fps", 20);
            duration = intent.getIntExtra("duration", 0);
            playMusic = intent.getBooleanExtra("playMusic", true);
            packageDirectory = intent.getStringExtra("package");
            subliminalOccurrence = intent.getFloatExtra("subliminalOccurrence", 0f);

            cacheUpdater.setPackageDirectory(packageDirectory);

            // set screen max brightness
            if (intent.getBooleanExtra("maxBrightness", true)) {
                WindowManager.LayoutParams lp = ((Activity)context).getWindow().getAttributes();
                lp.screenBrightness = 1.0f;
                ((Activity)context).getWindow().setAttributes(lp);
            }
        }

        private void readSubliminals(File file) {
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader(file));
                String text = null;

                // repeat until all lines is read
                while ((text = reader.readLine()) != null) {
                    if (!text.trim().isEmpty())
                        subliminals.add(text);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            // collect garbage on new run
            Runtime.getRuntime().gc();
            System.gc();

            // get music file if possible
            File musicFile = null;

            List<File> imageFiles = null;
            try {
                // load images to flood
                File file = new File(AppEnvironment.getPath(((Activity) mContext).getApplication()) + packageDirectory);
                imageFiles = Arrays.asList(file.listFiles(new AppEnvironment.ImageFilter()));
                Collections.shuffle(imageFiles);

                // try to get music file (Get any first mp3 or whatever)
                file = new File(AppEnvironment.getPath(((Activity) mContext).getApplication()) + packageDirectory);
                File [] musicFiles = file.listFiles(new AppEnvironment.MusicFilter());
                if (musicFiles.length > 0)
                    musicFile = musicFiles[0];

                // try to get subliminals
                file = new File(AppEnvironment.getPath(((Activity) mContext).getApplication()) + packageDirectory + "/subliminal.txt");
                if (file.exists()) {
                    // read all text
                    readSubliminals(file);
                }
            }
            catch (Exception e) {
            }

            boolean loadCache = true;
            boolean started = false;
            int ticker = 0;

            while (isRunning) {
                if (!holder.getSurface().isValid()) {
                    continue;
                }

                // load in right moment so canvas is initialized and we can read screen bounds
                if (loadCache) {
                    Canvas c = holder.lockCanvas();

                    cacheUpdater.setImages(imageFiles);
                    cacheUpdater.setImageBounds(c.getWidth(), c.getHeight());
                    cacheUpdater.resume();
                    loadCache = false;

                    holder.unlockCanvasAndPost(c);
                }

                // wait until cache is ready
                if (cacheUpdater.isError) {
                    Canvas c = holder.lockCanvas();
                    c.drawARGB(255, 100, 0, 0);
                    drawText("Error: " + cacheUpdater.lastError, c, 0, 0, Gravity.CENTER, 19);
                    holder.unlockCanvasAndPost(c);
                }
                else if (!cacheUpdater.isReady) {
                    Canvas c = holder.lockCanvas();

                    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                    String duration = preferences.getString("duration", "");

                    c.drawARGB(255, 0, 0, 0);
                    drawText("Loading..." + duration, c, 0, 0, Gravity.CENTER, 19);
                    drawText("WARNING: this application is flashing images which may not be suitable for photosensitive epilepsy", c, 10, 10, Gravity.BOTTOM, 13);

                    holder.unlockCanvasAndPost(c);
                } else {
                    Canvas c = holder.lockCanvas();
                    // main drawing loop

                    // initialize flood
                    if (!started) {
                        if (playMusic && musicFile != null)
                            music.start(musicFile.getAbsolutePath());

                        started = true;
                        startTime = System.currentTimeMillis();
                    }

                    // redraw background
                    c.drawARGB(255, 0, 0, 0);

                    // obtain image from cache
                    Bitmap b = cacheUpdater.getImageFromTick(ticker);

                    // draw image to middle of the screen
                    c.drawBitmap(b, (c.getWidth() / 2) - (b.getWidth() / 2), (c.getHeight() / 2) - (b.getHeight() / 2), null);

                    // TODO add sublimal messages
                    if (Math.random() < subliminalOccurrence && subliminals.size() > 0) {
                        String message = subliminals.get(ticker % subliminals.size());
                        drawText(message, c, 0, 0, Gravity.CENTER, 100);
                    }

                    // could be usefull in desperate times :)
                    // drawText("Memory: " + Runtime.getRuntime().freeMemory(), c, 0, 0, Gravity.BOTTOM, 19);
                    holder.unlockCanvasAndPost(c);

                    // stop flooding
                    if (duration != 0 && System.currentTimeMillis() - startTime > duration * 1000 * 60) {
                        ((Activity)mContext).finish();
                    }
                }

                // slow down flooding process
                try {
                    Thread.sleep(1000 / fps);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ticker++;
            }
        }

        /**
         * Pretty Text Drawing
         * @param text
         * @param canvas
         * @param x
         * @param y
         * @param gravity Docking location (Gravity.TOP, Gravity.CENTER, Gravity.BOTTOM)
         * @param size SP size of the font
         */
        private void drawText(String text, Canvas canvas, int x, int y, int gravity, int size) {
            TextView tv = new TextView(mContext);
            tv.setText(text);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
            tv.setTextColor(Color.WHITE);

            tv.setDrawingCacheEnabled(true);
            tv.measure(MeasureSpec.makeMeasureSpec(canvas.getWidth() - (x*2), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(canvas.getHeight() - (y*2), MeasureSpec.AT_MOST));
            tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);

            if (gravity == Gravity.CENTER) {
                x += (canvas.getWidth() / 2) - (tv.getMeasuredWidth() / 2);
                y += (canvas.getHeight() / 2) - (tv.getMeasuredHeight() / 2);
            }

            if (gravity == Gravity.BOTTOM) {
                y = (canvas.getHeight() - tv.getMeasuredHeight() - y);
            }

            canvas.drawBitmap(tv.getDrawingCache(), x, y, paint);
            tv.setDrawingCacheEnabled(false);
        }

        /**
         * Pause flooding and kill background threads
         */
        public void pause() {
            // stop cache updater
            cacheUpdater.pause();
            isRunning = false;

            while (true) {
                try {
                    floodThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                music.stop();
                break;
            }

            floodThread = null;
        }

        public void resume() {
            isRunning = true;
            floodThread = new Thread(this);
            floodThread.setPriority(Thread.MAX_PRIORITY);
            floodThread.start();
        }
    }
}