package fr.louidji.agu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        final ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        ListView listView = (ListView) findViewById(R.id.listView);
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

                requestPermissions(new String[]{Manifest.permission.INTERNET},
                        1);

            }
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ArrayList<String> images = getAllShownImagesPath();
                adapter.clear();
                for (String image : images) {
                    adapter.add(image);
                    try {
                        push(image);
                    } catch (IOException e) {
                        Log.e(CLASS, "IO Erreur sur l'image : " + image, e);
                    }
                }

            }
        });


    }

    private void push(String image) throws IOException {
        // TODO param URL
        DataOutputStream out = null;
        FileInputStream in = null;
        BufferedReader reader = null;
        final String url = "http://192.168.1.45:9000/json-file-upload";
        try {
            HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "image/jpg");
            con.connect();

            out = new DataOutputStream(con.getOutputStream());
            in = new FileInputStream(new File(image));

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
            } else {
                Log.e(CLASS, "Error " + response + ", msg : " + responseMessage);
            }

            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) {
                Log.i(CLASS, line);
                // TODO parse pour recuperer les UUID pour test ulterieur pour suppression
            }

        } finally {
            if (null != in) {try {in.close();} finally {
                    try {if(null != reader) reader.close();} finally {
                        if (null != out) out.close();
                    }
                }
            }
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
     * @return ArrayList with images Path
     */
    public ArrayList<String> getAllShownImagesPath() {
        Cursor cursor = null;
        final ArrayList<String> listOfAllImages = new ArrayList<String>();
        try {

            cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, // Which columns to return
                    null,       // Which rows to return (all rows)
                    null,       // Selection arguments (none)
                    null        // Ordering
            );
            final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                listOfAllImages.add(cursor.getString(column_index_data));
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return listOfAllImages;
    }


}
