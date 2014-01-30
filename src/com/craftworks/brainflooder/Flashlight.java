package com.craftworks.brainflooder;

import android.app.Activity;
import android.hardware.Camera;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;

// suppose this is the easiest flashlight code ever
public class Flashlight implements SurfaceHolder.Callback {
    private Camera camera;
    private Camera.Parameters cameraParameters;

    private Thread strobe;
    private int strobeInterval;

    public Flashlight(Activity main) {
        try {
            int flashCamera = findFlashCamera();
            camera = Camera.open(flashCamera);

            SurfaceView surfaceView = new SurfaceView(main);

            // it has to be micro visible
            main.addContentView(surfaceView, new ViewGroup.LayoutParams(1, 1));

            // create surface holder for camera
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(this);

            cameraParameters = camera.getParameters();
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY); // some cams require this to enable flash
            camera.setParameters(cameraParameters);

            camera.startPreview();
        }
        catch (Exception e) {
            Log.e("com.craftworks.brainflooder", e.toString());
        }
    }

    private int findFlashCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);

            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }

        return cameraId;
    }

    public void release() {
        enable(false);

        try {
            synchronized(camera) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }

            if (strobe != null) {
                strobe.join();
                strobe = null;
            }
        } catch (Exception e) {
            Log.e("com.craftworks.brainflooder", e.toString());
        }
    }

    public void enable(boolean enable) {

        try {
            synchronized(camera) {
                if (camera != null) {
                    if (enable) {
                        cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    } else {
                        cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                    camera.setParameters(cameraParameters);
                }
            }
        } catch (Exception e) { }
    }

    public void strobe(int ms) {
        strobeInterval = ms;
        strobe = new Thread() {
            private int x = 0;

            public void run() {
                // do stuff
                while (camera != null) {
                    enable(x++ % 2 == 1);

                    try {
                        Thread.sleep(strobeInterval);
                    } catch (InterruptedException e) { }
                }
            }
        };

        strobe.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setDisplayOrientation(0);
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Log.e("com.craftworks.brainflooder", e.toString());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }
}
