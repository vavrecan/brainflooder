package com.craftworks.brainflooder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class GalleryBrowserActivity extends Activity {
    private String path;
    private Stack<String> pathHistory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        // load directory
        path = getIntent().getStringExtra("path");
        pathHistory = new Stack<String>();

        // init path browser
        loadDirectory();
    }

    @Override
    public void onBackPressed() {
        if (!pathHistory.empty()) {
            path = pathHistory.pop();
            loadDirectory();
        } else {
            super.onBackPressed();
        }
    }

    private void loadDirectory() {
        ((TextView)findViewById(R.id.directoryPath)).setText(path);
        ArrayList<HashMap<String, String>> pathList = new ArrayList<HashMap<String, String>>();

        File filePath = new File(path);
        File[] directories = filePath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
            File file = new File(dir, name);
            return file.isDirectory() && !file.isHidden() && file.getName() != "Brain Flooder";
            }
        });

        if (filePath.getParentFile() != null) {
            HashMap<String, String> pathItemParent = new HashMap<String, String>();
            pathItemParent.put("title", "..");
            pathItemParent.put("image", "");
            pathItemParent.put("type", "directory");
            pathList.add(pathItemParent);
        }

        for (File directory : directories) {
            HashMap<String, String> pathItem = new HashMap<String, String>();
            pathItem.put("title", directory.getName());
            pathItem.put("image", "directory");
            pathItem.put("type", "directory");
            pathList.add(pathItem);
        }

        // check if folder contains some images
        File[] images = getImagesInDirectory(filePath);
        if (images.length > 0) {
            File [] musicFiles = filePath.listFiles(new AppEnvironment.MusicFilter());

            // select subliminals txt
            Boolean subliminalsFile = (new File(path + "/subliminal.txt").exists()) ;

            // fill gallery picker element
            HashMap<String, String> galleryItemParent = new HashMap<String, String>();
            galleryItemParent.put("images", Integer.toString(images.length));
            galleryItemParent.put("subliminals", subliminalsFile ? "Yes" : "No");
            galleryItemParent.put("music", musicFiles.length > 0 ? "Yes" : "No");
            galleryItemParent.put("title", "[select]");
            galleryItemParent.put("image", images[0].getAbsolutePath());
            galleryItemParent.put("type", "gallery");
            pathList.add(galleryItemParent);
        }

        ListView pathListView = (ListView)findViewById(R.id.pathListView);
        PathListAdapter adapter = new PathListAdapter(this, pathList);
        pathListView.setAdapter(adapter);

        pathListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            PathListAdapter adapter = (PathListAdapter) ((ListView) adapterView).getAdapter();
            String selected = (String)adapter.getItem(position);

            if (selected == "[select]") {
                // selected path yupie
                Intent data = new Intent();
                data.putExtra("path", path);
                setResult(Activity.RESULT_OK, data);
                finish();
            }
            else if (selected == "..") {
                // keep history for back button
                pathHistory.add(path);
                // selected path ..
                File filePath = new File(path);
                path = filePath.getParentFile().getAbsolutePath();
                loadDirectory();
            }
            else {
                pathHistory.add(path);
                // append to path
                path = path + (path == "/" ? "" : "/") + selected;
                loadDirectory();
            }
            }
        });

        pathListView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView)view.findViewById(R.id.pathItemImage);
                if (imageView != null)
                    imageView.setImageBitmap(null);

                final ImageView imageViewGallery = (ImageView)view.findViewById(R.id.pathItemGalleryImage);
                if (imageViewGallery != null)
                    imageViewGallery.setImageBitmap(null);
            }
        });
    }

    private File[] getImagesInDirectory(File directory) {
        File[] images = new File(directory.getAbsolutePath()).listFiles(new AppEnvironment.ImageFilter());
        return images;
    }

    public class PathListAdapter extends BaseAdapter {
        private Activity activity;
        private ArrayList<HashMap<String, String>> data;
        private LayoutInflater inflater;

        public PathListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
            HashMap<String, String> pathItem = data.get(position);

            if (pathItem.get("type") == "gallery") {
                View vi = inflater.inflate(R.layout.list_row_gallery_images, null);

                ImageView galleryImage = (ImageView)vi.findViewById(R.id.pathItemGalleryImage);

                Bitmap b = BitmapFactory.decodeFile(pathItem.get("image"));
                galleryImage.setImageBitmap(AppEnvironment.resizeBitmap(b, 100, 100, false));
                b.recycle();
                b = null;

                ((TextView)vi.findViewById(R.id.galleryNumberOfImagesTextView)).setText("Number of pictures: " + pathItem.get("images"));
                ((TextView)vi.findViewById(R.id.galleryHasMusicTextView)).setText("Music: " + pathItem.get("music"));
                ((TextView)vi.findViewById(R.id.galleryHasSubliminalsTextView)).setText("Subliminals: " + pathItem.get("subliminals"));

                return vi;
            }

            // get directory item
            // always fresh inflate because we are setting visibility
            View vi = inflater.inflate(R.layout.list_row_gallery, null);

            ((TextView)vi.findViewById(R.id.pathItemText)).setText(pathItem.get("title"));

            ImageView image = (ImageView)vi.findViewById(R.id.pathItemImage); // thumb image

            // Setting all values in listview
            String imageFile = pathItem.get("image");

            if (imageFile == "directory") {
                Bitmap icon = BitmapFactory.decodeResource(this.activity.getBaseContext().getResources(), R.drawable.directory);
                image.setImageBitmap(icon);
            }
            else if (imageFile != "") {
                Bitmap b = BitmapFactory.decodeFile(imageFile);
                image.setImageBitmap(AppEnvironment.resizeBitmap(b, 100, 100, false));
                b.recycle();
                b = null;
            }
            else {
                Bitmap icon = BitmapFactory.decodeResource(this.activity.getBaseContext().getResources(), R.drawable.no_image);
                image.setImageBitmap(icon);
                image.setVisibility(View.INVISIBLE);
            }

            return vi;
        }
    }
}