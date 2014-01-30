package com.craftworks.brainflooder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class DreamListActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dreams);

        loadDreams();
    }

    private void loadDreams() {
        ArrayList<HashMap<String, String>> dreamList = new ArrayList<HashMap<String, String>>();

        File file = new File(AppEnvironment.getPath(this.getApplication()));

        // show path we are in
        ((TextView)findViewById(R.id.textDreamsPath)).setText(file.getAbsolutePath());

        // make sure default path exists
        if (!file.exists()) {
            file.mkdir();
        }

        File[] directories = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });

        for (File directory : directories) {

            // avoid displaying in gallery
            createNoMediaFile(directory);

            // count images in folder
            File [] images = getImagesInDirectory(directory);
            int imagesCount = images.length;

            HashMap<String, String> dream = new HashMap<String, String>();
            dream.put("title", directory.getName());
            dream.put("info", "Number of photos: " + imagesCount);
            dream.put("path", directory.getAbsolutePath());
            dream.put("image", imagesCount > 0 ? images[0].getAbsolutePath() : "");
            dreamList.add(dream);
        }

        ListView dreamsListView = (ListView)findViewById(R.id.dreamsListView);
        DreamListAdapter adapter = new DreamListAdapter(this, dreamList);
        dreamsListView.setAdapter(adapter);

        dreamsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            DreamListAdapter adapter = (DreamListAdapter)((ListView)adapterView).getAdapter();
            String selected = (String)adapter.getItem(position);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("package", selected);
            // reset history because this is a fresh pick
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            }
        });

        dreamsListView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView)view.findViewById(R.id.dreamImage);
                imageView.setImageBitmap(null);
            }
        });
    }

    private void createNoMediaFile(File file) {
        File noMediaFile = new File(file.getAbsolutePath() + "/.nomedia");
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File[] getImagesInDirectory(File directory) {
        File[] images = new File(directory.getAbsolutePath()).listFiles(new AppEnvironment.ImageFilter());
        return images;
    }

    public void onGalleryButton(View v) {
        Intent intent = new Intent(this, GalleryBrowserActivity.class);
        intent.putExtra("path", Environment.getExternalStorageDirectory().getAbsolutePath());
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // selected path to create dream from
            if (!data.hasExtra("path"))
                 return;

            final String createFrom = data.getStringExtra("path");
            String baseName = (new File(createFrom)).getName();

            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Brain Flooder");
            alert.setMessage("Enter name: ");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setText(baseName);
            input.setPadding(5,5,5,5);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    createDream(value, createFrom);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
    }

    public void createDream(final String name, final String sourcePath) {
        final Activity parentActivity = this;
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Please Wait!");
        progress.setMessage("Creating your dream...");
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        progress.show();
        new Thread()
        {
            public void run()
            {
                File destinationPath = new File(AppEnvironment.getPath(parentActivity.getApplication()) + "/" + name);
                if (!destinationPath.exists()) {
                    destinationPath.mkdir();
                }

                // copy over sized images
                File[] images = getImagesInDirectory(new File(sourcePath));
                for (File image : images) {
                    // nifty cleanup
                    Runtime.getRuntime().gc();
                    System.gc();

                    Bitmap scaled = loadImageAsScaledBitmap(image);
                    File destinationFile = new File(destinationPath.getAbsolutePath() + "/" + image.getName());
                    if (scaled != null) {
                        // save in destination directory
                        try {
                            FileOutputStream out = new FileOutputStream(destinationFile);
                            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            out.close();
                            out = null;

                            scaled.recycle();
                            scaled = null;

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // copy over subliminals and music
                File otherPath = new File(sourcePath);
                File [] otherFiles = otherPath.listFiles(new AppEnvironment.OtherFilter());
                for (File f : otherFiles) {
                    File destinationFile = new File(destinationPath.getAbsolutePath() + "/" + f.getName());
                    try {
                        copy(f, destinationFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // in in main thread
                runOnUiThread(new Runnable() {
                    // thread buggy part, just ignore on error
                    // reload UI
                    @Override
                    public void run() {
                        try {
                            Toast.makeText(getBaseContext(), "Dream created", Toast.LENGTH_SHORT).show();
                            if (progress != null && progress.isShowing()) {
                                progress.dismiss();
                            }
                            loadDreams();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private Bitmap loadImageAsScaledBitmap(File image) {
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);

        BitmapFactory.Options bfOptions = new BitmapFactory.Options();
        bfOptions.inPurgeable = true;
        bfOptions.inDither = true;
        bfOptions.inPreferQualityOverSpeed = true;
        bfOptions.inInputShareable = false;

        Bitmap b = BitmapFactory.decodeFile(image.getAbsolutePath(), bfOptions);

        int boundWidth = 0;
        int boundHeight = 0;

        if (display.getOrientation() == Surface.ROTATION_0 || display.getOrientation() == Surface.ROTATION_180) {
            boundHeight = metrics.widthPixels;
            boundWidth = metrics.heightPixels;
        } else {
            boundWidth = metrics.widthPixels;
            boundHeight = metrics.heightPixels;
        }

        Bitmap scaled = AppEnvironment.resizeBitmap(b, boundWidth, boundHeight, true);

        b.recycle();
        b = null;

        return scaled;
    }

    public class DreamListAdapter extends BaseAdapter {
        private Activity activity;
        private ArrayList<HashMap<String, String>> data;
        private LayoutInflater inflater;

        public DreamListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
            activity = a;
            data = d;
            inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position).get("title");
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (convertView == null)
                vi = inflater.inflate(R.layout.list_row_dream, null);

            TextView name = (TextView)vi.findViewById(R.id.dreamNameText); // title
            TextView info = (TextView)vi.findViewById(R.id.dreamInfoText); // artist name
            ImageView image = (ImageView)vi.findViewById(R.id.dreamImage); // thumb image

            HashMap<String, String> dream = new HashMap<String, String>();
            dream = data.get(position);

            // Setting all values in listview
            name.setText(dream.get("title"));
            info.setText(dream.get("info"));

            String imageFile = dream.get("image");
            if (imageFile != "") {
                try {
                    Bitmap b = BitmapFactory.decodeFile(imageFile);
                    image.setImageBitmap(AppEnvironment.resizeBitmap(b, 100, 100, false));
                    b.recycle();
                    b = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return vi;
        }
    }
}
