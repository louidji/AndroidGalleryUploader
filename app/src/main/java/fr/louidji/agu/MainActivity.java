package fr.louidji.agu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private final static String[] PROJECTION = {MediaStore.MediaColumns.DATA};
    private static final String CLASS = "fr.louidji.agu";
    private SharedPreferences pref;
    private String url;
    private String uploadUrl;
    private String wsUrl;
    private WebSocketClient mWebSocketClient;
    private Hashtable<String, String> images = new Hashtable<>();

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

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

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
                    final String[] images = getAllShownImagesPath();
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
                    Log.i("Websocket", "Opened");
                }

                @Override
                public void onMessage(String s) {
                    Log.i("Websocket", s);
                    JSONObject jsonObj = null;
                    try {
                        jsonObj = new JSONObject(s);
                        final String UUID = jsonObj.getString("uuid");
                        final boolean done = jsonObj.getBoolean("done");

                        if (done) {
                            // TODO suppression de l'image... si done ...
                            int count = 0;
                            while (!images.containsKey(UUID) && count++ < 10) {
                                try {
                                    // WTF ws trop rapide...
                                    Thread.currentThread().sleep(100l);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            Log.i("Websocket", UUID + " ? " + done + ", file : " + images.get(UUID));

                        } else {
                            // TODO affichage avec info...
                        }


                    } catch (JSONException e) {
                        Log.e("Websocket", "Error " + e.getMessage(), e);
                    }

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            TextView textView = (TextView) findViewById(R.id.messages);
//                            textView.setText(textView.getText() + "\n" + message);
//                        }
//                    });
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i("Websocket", "Closed " + s);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("Websocket", "Error " + e.getMessage(), e);
                }
            };
            mWebSocketClient.connect();
        }


    }

    private UploadResult push(String image) throws IOException, JSONException {

        DataOutputStream out = null;
        FileInputStream in = null;
        BufferedReader reader = null;
        UploadResult uploadResult = null;
        final StringBuilder sb = new StringBuilder();
        final File file = new File(image);
        try {
            HttpURLConnection con = (HttpURLConnection) (new URL(uploadUrl)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "image/jpg");
            con.connect();

            out = new DataOutputStream(con.getOutputStream());

            in = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            final int response = con.getResponseCode();
            if (200 == response) {
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    //Log.i(CLASS, line);
                    sb.append(line);
                }
                JSONObject jsonObj = new JSONObject(sb.toString());
                uploadResult = new UploadResult(jsonObj.getString("uuid"), file.getName(), jsonObj.getString("status"));
                images.put(uploadResult.uuid, uploadResult.fileName);
            } else {
                Log.e(CLASS, "Error " + response + ", msg : " + con.getResponseMessage());
            }
            con.disconnect();
            return uploadResult;
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
            listOfAllImages = new String[2]; // FIXME hack pour test => desactiver
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
        private Exception exception = null;

        UploadImage(final ArrayAdapter<String> adapter, View view, FloatingActionButton fab) {
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
        protected Integer doInBackground(String... images) {
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
                Log.i(CLASS, "Push : " + result);
                adapter.insert(result.fileName + " (" + result.uuid + ") : " + result.status, adapter.getCount());
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
