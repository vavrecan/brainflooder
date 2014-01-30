package com.craftworks.brainflooder;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

public class AppEnvironment {
    public static String getPath(Application app) {
        return String.format("%s/%s/", Environment.getExternalStorageDirectory().getAbsolutePath(), app.getString(R.string.app_path));
    }

    public static String getDreamPackage(Application app) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext());
        return preferences.getString("package", null);
    }

    public static void setDreamPackage(Application app, String dreamPackage) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext()).edit();
        edit.putString("package", dreamPackage);
        edit.commit();
    }

    public static boolean copyFile(File source, File dest) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(source));
            bos = new BufferedOutputStream(new FileOutputStream(dest, false));

            byte[] buf = new byte[1024];
            bis.read(buf);

            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    public static Bitmap resizeBitmap(Bitmap b, int boundWidth, int boundHeight, boolean upscale) {
        try  {
            BitmapFactory.Options bfOptions = new BitmapFactory.Options();
            bfOptions.inPurgeable = true;
            bfOptions.inDither = true;
            bfOptions.inPreferQualityOverSpeed = true;
            bfOptions.inInputShareable = false;

            int originalWidth = b.getWidth();
            int originalHeight = b.getHeight();

            int newWidth = originalWidth;
            int newHeight = originalHeight;

            double targetRatio = boundWidth / (float)boundHeight;
            double imageRatio = originalWidth / (float)originalHeight;

            if (imageRatio < targetRatio) // Target ratio is wider than the source ratio, scale by height
            {
                int smallerY = upscale ? boundHeight : Math.min(originalHeight, boundHeight);
                newWidth = (int)Math.round(smallerY * imageRatio);
                newHeight = smallerY;
            }
            else
            {
                int smallerX = upscale ? originalWidth : Math.min(originalWidth, boundWidth);
                newWidth = smallerX;
                newHeight = (int)Math.round(smallerX / imageRatio);
            }

            Bitmap scaled = Bitmap.createScaledBitmap(b, newWidth, newHeight, true);
            return scaled;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    static class MusicFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".mp3") ||
                    name.toLowerCase().endsWith(".ogg")||
                    name.toLowerCase().endsWith(".wav") ||
                    name.toLowerCase().endsWith(".m4a") ||
                    name.toLowerCase().endsWith(".acc"));
        }
    }

    static class ImageFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".jpeg")||
                    name.toLowerCase().endsWith(".png") ||
                    name.toLowerCase().endsWith(".gif"));
        }
    }

    static class OtherFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".mp3") ||
                    name.toLowerCase().endsWith(".ogg")||
                    name.toLowerCase().endsWith(".wav") ||
                    name.toLowerCase().endsWith(".m4a") ||
                    name.toLowerCase().endsWith(".txt") ||
                    name.toLowerCase().endsWith(".acc"));
        }
    }
}
