package photos.niyo.com.photosshare;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import photos.niyo.com.photosshare.db.PhotosShareColumns;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private FoldersListAdapter mAdapter;
    private List<Folder> mFoldersList;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final Context context = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CreateEvent.class);
                startActivity(intent);
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.foldersList);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mFoldersList = new ArrayList<>();
        mAdapter = new FoldersListAdapter(mFoldersList);
        mRecyclerView.setAdapter(mAdapter);
        findViewById(R.id.listContainer).setVisibility(View.GONE);
        findViewById(R.id.emptyView).setVisibility(View.GONE);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Constants.FOLDERS_URI,
                Constants.FOLDERS_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished started");
        mFoldersList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            findViewById(R.id.listContainer).setVisibility(View.VISIBLE);
            findViewById(R.id.emptyView).setVisibility(View.GONE);
            while (!cursor.isAfterLast()) {
                int colFolderNameIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_NAME);
                String foldeName = cursor.getString(colFolderNameIndex);

                int colCreatedAtIndex = cursor.getColumnIndex(PhotosShareColumns.CREATE_AT);
                String createdAt = cursor.getString(colCreatedAtIndex);
                Folder folder = new Folder();
                folder.setName(foldeName);
                folder.setCreatedAt(new Date(Long.valueOf(createdAt)));
                mFoldersList.add(folder);
            }

        }
        else {
            showEmptyPage();
        }

    }

    private void showEmptyPage() {
        Log.d(LOG_TAG, "showEmptyPage started");
        findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
        findViewById(R.id.listContainer).setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
