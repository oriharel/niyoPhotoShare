package photos.niyo.com.photosshare;

import android.Manifest;
import android.app.LoaderManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import photos.niyo.com.photosshare.db.PhotosShareColumns;

import static photos.niyo.com.photosshare.PhotosContentJob.MEDIA_URI;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private FoldersListAdapter mAdapter;
    private List<Folder> mFoldersList;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    Handler mHandler = new Handler();
    private ContentObserver mObserver;
    private PhotosContentJob job;
    private JobScheduler mJobScheduler;
    JobInfo JOB_INFO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final MainActivity context = this;
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

        mObserver = new ContentObserver(mHandler) {
            @Override
            public boolean deliverSelfNotifications() {
                return super.deliverSelfNotifications();
            }

            @Override
            public void onChange(boolean selfChange) {
                Log.d(LOG_TAG, "onChange called from observer");
                getLoaderManager().restartLoader(0, null, context);
            }
        };
        registerForChanges();
        requestPermissionForPhotosRead();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    schedulePhotosJob();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestPermissionForPhotosRead() {
        Log.d(LOG_TAG, "requestPermissionForPhotosRead started");
        int permissionCheck = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "needs user to authorize photos read");
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        else {
            Log.d(LOG_TAG, "permission to photos read already granted");
            schedulePhotosJob();
        }
    }

    private void schedulePhotosJob() {
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( 1,
                new ComponentName( getPackageName(),PhotosContentJob.class.getName() ) );
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
        // Also look for general reports of changes in the overall provider.
        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI, 0));

        JOB_INFO = builder.build();
        mJobScheduler.schedule(JOB_INFO);
        Log.i(LOG_TAG, "JOB SCHEDULED!");
    }

    private void registerForChanges() {
        getContentResolver().registerContentObserver(Constants.FOLDERS_URI, false, mObserver);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
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
        mFoldersList.clear();

        if (cursor.moveToFirst()) {
            findViewById(R.id.listContainer).setVisibility(View.VISIBLE);
            findViewById(R.id.emptyView).setVisibility(View.GONE);
            while (!cursor.isAfterLast()) {
                int colFolderNameIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_NAME);
                String foldeName = cursor.getString(colFolderNameIndex);

                int colCreatedAtIndex = cursor.getColumnIndex(PhotosShareColumns.CREATE_AT);
                String createdAt = cursor.getString(colCreatedAtIndex);
                Folder folder = new Folder();
                Log.d(LOG_TAG, "adding "+foldeName);
                folder.setName(foldeName);
                folder.setCreatedAt(Long.valueOf(createdAt));
                mFoldersList.add(folder);
                cursor.moveToNext();
            }

            mAdapter.notifyItemInserted(mFoldersList.size());

        }
        else {
            showEmptyPage();
        }

        cursor.close();

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
