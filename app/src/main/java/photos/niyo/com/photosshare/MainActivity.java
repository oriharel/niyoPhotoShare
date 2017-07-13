package photos.niyo.com.photosshare;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import photos.niyo.com.photosshare.db.PhotosShareColumns;
import photos.niyo.com.photosshare.db.User;
import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.GetUsersFromDbTask;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static photos.niyo.com.photosshare.PhotosContentJob.MEDIA_URI;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final long JOB_PERIODIC_INTERVAL = 15 * 60 * 1000;

    private FoldersListAdapter mAdapter;
    private List<Folder> mFoldersList;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    Handler mHandler = new Handler();
    private ContentObserver mObserver;
    private JobScheduler mJobScheduler;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = { DriveScopes.DRIVE_METADATA_READONLY };
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_START_DATE = 1004;
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final Integer PHOTOS_JOB_ID = 1;
    public static final Integer FOLDERS_JOB_ID = 2;
    public static final Integer PREVIEW_IMAGE_JOB_ID = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final MainActivity context = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CreateEvent.class);
                startActivity(intent);
            }
        });

        showActiveFolder();
        mRecyclerView = (RecyclerView) findViewById(R.id.archivedFoldersList);
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mFoldersList = new ArrayList<>();
        mAdapter = new FoldersListAdapter(mFoldersList);
        mRecyclerView.setAdapter(mAdapter);
        findViewById(R.id.archivedFoldersList).setVisibility(View.GONE);
        findViewById(R.id.archivedListLabel).setVisibility(View.GONE);
        findViewById(R.id.folderCard).setVisibility(View.GONE);

        mObserver = new ContentObserver(mHandler) {
            @Override
            public boolean deliverSelfNotifications() {
                return super.deliverSelfNotifications();
            }

            @Override
            public void onChange(boolean selfChange) {
                Log.d(LOG_TAG, "onChange called from observer");
                getLoaderManager().restartLoader(0, null, context);
                showActiveFolder();
            }
        };
        registerForChanges();

        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );
        mJobScheduler.cancelAll();

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        final SharedPreferences pref = getApplicationContext().getSharedPreferences("app",
                Context.MODE_PRIVATE);
        String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
        Log.d(LOG_TAG, "accountName found is: "+accountName);
        mCredential.setSelectedAccountName(accountName);
        getResultsFromApi();
    }

    private void showActiveFolder() {
        Log.d(LOG_TAG, "[showActiveFolder] started");
        final FolderViewHolder holder = new FolderViewHolder(findViewById(R.id.folderCard));
        final ServiceCaller usersCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                User[] attendees = (User[])data;
                holder.addAttendees(attendees);
            }

            @Override
            public void failure(Object data, String description) {

            }
        };

        final Context context = this;
        ServiceCaller activeCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "[showActiveFolder] success");
                Folder activeFolder = (Folder)data;
                activeFolder.setIsActive(true);
                GetUsersFromDbTask usersTask = new GetUsersFromDbTask(context, usersCaller, MainActivity.class.getSimpleName());
                usersTask.execute(activeFolder.getSharedWith());
                findViewById(R.id.folderCard).setVisibility(View.VISIBLE);

                holder.bindFolder(activeFolder);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "[showActiveFolder] can't find active folder");
                findViewById(R.id.folderCard).setVisibility(View.VISIBLE);
                FolderViewHolder holder = new FolderViewHolder(findViewById(R.id.folderCard));
                holder.bindEmptyFolder();
            }
        };

        GetActiveFolderFromDbTask task = new GetActiveFolderFromDbTask(this, activeCaller);
        task.execute();
    }

    private void getResultsFromApi() {
        Log.d(LOG_TAG, "getResultsFromApi started mCredential.getSelectedAccountName() = "+mCredential.getSelectedAccountName());
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissionForPhotosRead();
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        Log.d(LOG_TAG, "chooseAccount started");
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            Log.d(LOG_TAG, "already has permissions to GET_ACCOUNTS");
            String accountName = getSharedPreferences("app", Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            Log.d(LOG_TAG, "need permission to GET_ACCOUNTS");
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        boolean result = connectionStatusCode == ConnectionResult.SUCCESS;
        Log.d(LOG_TAG, "isGooglePlayServicesAvailable ended with success? "+result);
        return result;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(LOG_TAG, "onActivityResult called with requestCode: "+requestCode);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                } else {
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getSharedPreferences("app", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {

        Log.d(LOG_TAG, "onRequestPermissionsResult called with requestCode: "+requestCode);
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    schedulePhotosJob();
                    scheduleFoldersSync();
                    schedulePreviewImageJob();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case REQUEST_PERMISSION_GET_ACCOUNTS: {
                getResultsFromApi();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void schedulePreviewImageJob() {
        Log.d(LOG_TAG, "schedulePreviewImageJob started");
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( PREVIEW_IMAGE_JOB_ID,
                new ComponentName( getPackageName(),GetPreviewPhotoService.class.getName() ) );

        builder.setPeriodic(JOB_PERIODIC_INTERVAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        mJobScheduler.schedule(builder.build());
        Log.i(LOG_TAG, "GetPreviewPhotoService JOB SCHEDULED!");
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
            scheduleFoldersSync();
            schedulePreviewImageJob();
        }
    }

    private void scheduleFoldersSync() {
        Log.d(LOG_TAG, "scheduleFoldersSync started");
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( FOLDERS_JOB_ID,
                new ComponentName( getPackageName(),FolderSyncService.class.getName() ) );

        builder.setPeriodic(JOB_PERIODIC_INTERVAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        mJobScheduler.schedule(builder.build());
        syncFoldersNow();
        Log.i(LOG_TAG, "FolderSyncService JOB SCHEDULED!");
    }

    private void syncFoldersNow() {
        Log.d(LOG_TAG, "syncFoldersNow started");
        final Context context = this;
        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from FoldersSyncUtil");
                ServiceCaller someCaller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {
                        Log.d(LOG_TAG, "returned success from GetPreviewPhotoUtil");
                    }

                    @Override
                    public void failure(Object data, String description) {
                        Log.e(LOG_TAG, "returned failure from GetPreviewPhotoUtil");
                    }
                };
                GetPreviewPhotoUtil.updatePhoto(context, someCaller);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received failure from FoldersSyncUtil");
            }
        };

        FoldersSyncUtil.updateFolders(this, caller);
    }

    private void schedulePhotosJob() {
        Log.d(LOG_TAG, "schedulePhotosJob started");
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( PHOTOS_JOB_ID,
                new ComponentName(getPackageName(),PhotosContentJob.class.getName() ) );
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(JOB_PERIODIC_INTERVAL);

        mJobScheduler.schedule(builder.build());
        Log.i(LOG_TAG, "JOB SCHEDULED!");
    }

    private void registerForChanges() {
        getContentResolver().registerContentObserver(Constants.FOLDERS_URI, false, mObserver);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LOG_TAG, "onResume started");
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = PhotosShareColumns.END_DATE+"<"+Calendar.getInstance().getTimeInMillis();
        return new CursorLoader(this, Constants.FOLDERS_URI,
                Constants.FOLDERS_PROJECTION, selection, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished started");
        mFoldersList.clear();
        Log.d(LOG_TAG, "currently there are "+mFoldersList.size()+" shown folders. results from db: "+cursor.getCount());

        if (cursor.moveToFirst()) {
            findViewById(R.id.archivedFoldersList).setVisibility(View.VISIBLE);
            findViewById(R.id.archivedListLabel).setVisibility(View.VISIBLE);
            while (!cursor.isAfterLast()) {
                Folder folder = Folder.createFolderFromCursor(cursor, false);
                Log.d(LOG_TAG, "adding folder: "+folder.getId());
                mFoldersList.add(folder);
                cursor.moveToNext();
            }

            mAdapter.notifyItemInserted(mFoldersList.size());

        }
        else {
//            showEmptyPage();
        }

        cursor.close();

    }

    private void showEmptyPage() {
        Log.d(LOG_TAG, "showEmptyPage started");
        findViewById(R.id.listContainer).setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
