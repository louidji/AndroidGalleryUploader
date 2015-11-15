package fr.louidji.agu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String[] PROJECTION = {MediaStore.MediaColumns.DATA};
    private static final String CLASS = "fr.louidji.agu";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        final ListView listView = (ListView) findViewById(R.id.listView);

        adapter.setNotifyOnChange(true);

        listView.setAdapter(adapter);
// Here, thisActivity is the current activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Explain to the user
                }

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Explain to the user
                }

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

            }
            if (checkSelfPermission(Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.INTERNET)) {
                    // Explain to the user
                }

                requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);

            }
        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                    final String[] images = getAllShownImagesPath();
                    if (null != images && images.length > 0) {
                        Snackbar.make(view, "Loading " + images.length + " images", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                        new UploadImage(adapter, view, fab).execute(getAllShownImagesPath());
                    } else {
                        Snackbar.make(view, "No images to load", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }


            }
        });


    }

    private UploadResult push(String image) throws IOException {

        DataOutputStream out = null;
        FileInputStream in = null;
        BufferedReader reader = null;
        UploadResult uploadResult = null;
        // TODO param URL
        final String url = "http://192.168.1.45:9000/json-file-upload";

        final StringBuffer sb = new StringBuffer();
        try {
            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "image/jpg");
            con.connect();

            out = new DataOutputStream(con.getOutputStream());
            final File file = new File(image);
            in = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            final int response = con.getResponseCode();
            final String responseMessage = con.getResponseMessage();
            if (200 == response) {
                Log.d(CLASS, "Upload Data, msg : " + responseMessage);
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    Log.i(CLASS, line);
                    sb.append(line).append("\n");
                }
                JSONObject jsonObj = new JSONObject(sb.toString());
                uploadResult = new UploadResult(jsonObj.getString("uuid"), file.getName(), jsonObj.getString("status"));
            } else {
                Log.e(CLASS, "Error " + response + ", msg : " + responseMessage);
            }
        } finally {
            if (null != in) {
                try {
                    in.close();
                } finally {
                    try {
                        if (null != reader) reader.close();
                    } finally {
                        if (null != out) out.close();
                    }
                }
            }

            return uploadResult;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Getting All Images Path
     *
     * @return Array with images Path
     */
    public String[] getAllShownImagesPath() {
        Cursor cursor = null;
        final String[] listOfAllImages;
        try {

            cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, // Which columns to return
                    null,       // Which rows to return (all rows)
                    null,       // Selection arguments (none)
                    null        // Ordering
            );
            final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            //listOfAllImages = new String[cursor.getCount()];
            listOfAllImages = new String[3]; // FIXME hack pour test => desactiver
            int i = 0;
            while (cursor.moveToNext()) {
                listOfAllImages[i++] = cursor.getString(column_index_data);
                if (i > 2) break; // FIXME hack pour test => desactiver
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return listOfAllImages;
    }

    private class UploadImage extends AsyncTask<String, UploadResult, Integer> {
        private final ArrayAdapter<String> adapter;
        private final View view;
        private final FloatingActionButton fab;

        UploadImage(final ArrayAdapter<String> adapter, View view, FloatingActionButton fab) {
            this.adapter = adapter;
            this.view = view;
            this.fab = fab;
            adapter.clear();
            adapter.notifyDataSetChanged();
            fab.setEnabled(false);
            fab.hide();
        }

        protected Integer doInBackground(String... images) {
            int count = images.length;

            for (int i = 0; i < count; i++) {
                try {
                    UploadResult uploadResult = push(images[i]);
                    publishProgress(uploadResult);
                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return count;
        }

        protected void onProgressUpdate(UploadResult... results) {
            // TODO gestion des resultat pour verification
            final UploadResult result = results[0];
            Log.i(CLASS, "Push : " + result);
            adapter.insert(result.fileName + " (" + result.uuid + ") : " + result.status, adapter.getCount());
            adapter.notifyDataSetChanged();
        }

        protected void onPostExecute(Integer result) {
            Log.i(CLASS, "Nb upload : " + result);
            Snackbar.make(view, "Loaded : " + result, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            fab.setEnabled(true);
            fab.show();

        }
    }

    private class UploadResult {
        public final String uuid;
        public final String fileName;
        public final String status;

        private UploadResult(String uuid, String fileName, String status) {
            this.uuid = uuid;
            this.fileName = fileName;
            this.status = status;
        }

        @Override
        public String toString() {
            return "UploadResult{" +
                    "uuid='" + uuid + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }


}
