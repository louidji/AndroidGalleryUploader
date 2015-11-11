package fr.louidji.agu;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String[] PROJECTION = {MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DISPLAY_NAME};


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

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ArrayList<String> images = getAllShownImagesPath();
                adapter.clear();
                //adapter.addAll(images);

                for (String image : images)
                    adapter.add(image);

            }
        });
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
            final int column_index_bucket_display_name = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            final int column_index_display_name = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            while (cursor.moveToNext()) {

                StringBuilder sb = new StringBuilder(cursor.getString(column_index_data)).append(" - ").append(cursor.getString(column_index_bucket_display_name))
                        .append(" - ").append(cursor.getString(column_index_display_name));


                listOfAllImages.add(sb.toString());
            }
        } finally {
            if (null != cursor)
                cursor.close();
        }
        return listOfAllImages;
    }


}
