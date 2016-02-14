package fr.louidji.agu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
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

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {
    private final static String[] PROJECTION = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media._ID};
    private static final String CLASS = "fr.louidji.agu";
    private SharedPreferences pref;
    private String url;
    private String uploadUrl;
    private String wsUrl;
    private WebSocketClient mWebSocketClient;
    private Hashtable<String, Integer> adapters = new Hashtable<>();
    private ArrayAdapter<UploadResult> adapter;
    private String androidUUID;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mWebSocketClient) {
            mWebSocketClient.close();
            mWebSocketClient = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        final ListView listView = (ListView) findViewById(R.id.listView);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        adapter.setNotifyOnChange(true);

        listView.setAdapter(adapter);

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
                if (loadConf()) {
                    if (adapter.getCount() > 0) {
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    }
                    final Image[] images = getAllShownImagesPath();
                    if (null != images && images.length > 0) {
                        Snackbar.make(view, "Sending " + images.length + " images", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                        new UploadImage(adapter, view, fab).execute(getAllShownImagesPath());
                    } else {
                        Snackbar.make(view, "No images to load", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }

            }
        });


        initWS();

    }

    private boolean loadConf() {
        if (pref.contains("url")) {
            url = pref.getString("url", null);
            if (url.length() > 11) {
                uploadUrl = url + "/json-file-upload";
                wsUrl = (url.startsWith("https://") ?
                        "wss://" + url.substring(8) : "ws://" + url.substring(7) + "/ws");

                androidUUID = pref.getString("androidUUID", null);
                if(null == androidUUID) {
                    SharedPreferences.Editor editor = pref.edit();
                    androidUUID = java.util.UUID.randomUUID().toString();
                    editor.putString("androidUUI", androidUUID);
                    editor.commit();
                }

                return true;
            }
        }
        startSettings();
        return false;

    }

    private void initWS() {
        if (loadConf()) {
            URI uri;
            try {
                uri = new URI(wsUrl);
            } catch (URISyntaxException e) {
                Log.e("Websocket", "Error " + e.getMessage(), e);
                return;
            }

            mWebSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.d("Websocket", "Opened");
                }

                @Override
                public void onMessage(String s) {
                    Log.d("Websocket", s);
                    JSONObject jsonObj;
                    try {
                        jsonObj = new JSONObject(s);
                        final String UUID = jsonObj.getString("uuid");
                        final boolean done = jsonObj.getBoolean("done");
                        int count = 0;

                        while (!adapters.containsKey(UUID) && count++ < 10) {
                            try {
                                // WTF ws trop rapide...
                                Thread.currentThread().sleep(100l);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        Integer pos = adapters.get(UUID);
                        if (pos != null) {
                            UploadResult uploadResult = adapter.getItem(pos);
                            Log.d("Websocket", UUID + " ? " + done + ", image : " + uploadResult.image);
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, uploadResult.image.id);
                            if (done) {
                                uploadResult.status = "Image integrate";

                                if (1 == getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, BaseColumns._ID + "=" + uploadResult.image
                                        .id, null)) {
                                    Log.d("Delete", "Delete " + uploadResult.image.data);
                                }

                            } else {
                                uploadResult.status = "Error on integreting image";
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("Websocket", "Error " + e.getMessage(), e);
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.d("Websocket", "Closed " + s);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("Websocket", "Error " + e.getMessage(), e);
                }
            };
            mWebSocketClient.connect();

            mWebSocketClient.send("{id: "+androidUUID+"}");
        }


    }

    private UploadResult push(Image image) throws IOException, JSONException {

        DataOutputStream out = null;
        FileInputStream in = null;
        BufferedReader reader = null;
        UploadResult uploadResult = null;
        final StringBuilder sb = new StringBuilder();
        final File file = new File(image.data);
        try {
            HttpURLConnection con = (HttpURLConnection) (new URL(uploadUrl)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "image/jpg");
            con.setRequestProperty("Client-UUID", androidUUID);
            con.setUseCaches(false);
            con.connect();

            out = new DataOutputStream(con.getOutputStream());


            in = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            final int response = con.getResponseCode();
            if (200 == response) {
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject jsonObj = new JSONObject(sb.toString());
                uploadResult = new UploadResult(jsonObj.getString("androidUUID"), image, jsonObj.getString("status"));
            } else {
                Log.e(CLASS, "Error " + response + ", msg : " + con.getResponseMessage());
            }
            con.disconnect();
            return uploadResult;
        } finally {
            if (null != out) {
                try {
                    out.close();
                } finally {
                    try {
                        if (null != in) in.close();
                    } finally {
                        if (null != reader) reader.close();
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
            startSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSettings() {
        this.startActivity(new Intent("SETTINGS"));
    }

    /**
     * Getting All Images Path
     *
     * @return Array with images Path
     */
    public Image[] getAllShownImagesPath() {
        Cursor cursor = null;
        final Image[] listOfAllImages;
        try {

            cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, // Which columns to return
                    null,       // Which rows to return (all rows)
                    null,       // Selection arguments (none)
                    null        // Ordering
            );
            final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            final int column_index_id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            listOfAllImages = new Image[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                listOfAllImages[i++] = new Image(cursor.getLong(column_index_id), cursor.getString(column_index_data));
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return listOfAllImages;
    }

    private class UploadImage extends AsyncTask<Image, UploadResult, Integer> {
        private final ArrayAdapter<UploadResult> adapter;
        private final View view;
        private final FloatingActionButton fab;
        private Exception exception = null;

        UploadImage(final ArrayAdapter<UploadResult> adapter, View view, FloatingActionButton fab) {
            this.adapter = adapter;
            this.view = view;
            this.fab = fab;
            adapter.clear();
            adapter.notifyDataSetChanged();
            fab.setEnabled(false);
            fab.hide();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Image... images) {
            int count = images.length;
            int i = 0;
            for (; i < count; i++) {
                try {
                    UploadResult uploadResult = push(images[i]);
                    publishProgress(uploadResult);
                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                } catch (IOException | JSONException e) {
                    Log.e(CLASS, e.getMessage(), e);
                    exception = e;

                    break;
                }

            }
            return i;
        }

        @Override
        protected void onProgressUpdate(UploadResult... results) {
            if (null != results && results.length > 0 && null != results[0]) {
                final UploadResult result = results[0];
                Log.d(CLASS, "Push : " + result);
                final int count = adapter.getCount();
                adapters.put(result.uuid, count);
                adapter.insert(result, count);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (null != exception) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                exception.printStackTrace(printWriter);
                builder.setTitle(R.string.alert).setMessage(stringWriter.toString())
                        .setNeutralButton(R.string.OK, null).setCancelable(true)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .create().show();
            } else {
                Log.i(CLASS, "Nb upload : " + result);
                Snackbar.make(view, "Loaded : " + result, Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
            fab.setEnabled(true);
            fab.show();
        }
    }

    private class Image {
        public final long id;
        public final String data;

        public Image(long id, String data) {
            this.id = id;
            this.data = data;
        }


    }

    private class UploadResult {


        private String uuid;
        private Image image;
        private String status;

        private UploadResult(String uuid, Image image, String status) {
            this.uuid = uuid;
            this.image = image;
            this.status = status;
        }

        @Override
        public String toString() {
            return "{ File='" + image.data + "', status='" + status + "'}";
        }
    }


}
