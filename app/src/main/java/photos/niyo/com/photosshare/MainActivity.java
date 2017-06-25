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
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import photos.niyo.com.photosshare.db.PhotosShareColumns;
import photos.niyo.com.photosshare.tasks.DeleteFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.GetFoldersTask;
import photos.niyo.com.photosshare.tasks.InsertFoldersToDbTask;
import photos.niyo.com.photosshare.tasks.IsFoldersChangeTask;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static photos.niyo.com.photosshare.PhotosContentJob.MEDIA_URI;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
//    private static final long JOB_PERIODIC_INTERVAL = 15 * 60 * 1000;
    private static final long JOB_PERIODIC_INTERVAL = 30 * 1000;

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
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final Integer PHOTOS_JOB_ID = 1;
    public static final Integer FOLDERS_JOB_ID = 2;

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

        showActiveFolder();
        mRecyclerView = (RecyclerView) findViewById(R.id.archivedFoldersList);
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

        updateLastSyncViews(pref);

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(FolderSyncService.LAST_SYNC_KEY) || key.equals(PhotosContentJob.LAST_SYNC_KEY)) {
                    updateLastSyncViews(pref);
                }
            }
        };

        pref.registerOnSharedPreferenceChangeListener(listener);

    }

    private void showActiveFolder() {
        Log.d(LOG_TAG, "[showActiveFolder] started");
        ServiceCaller activeCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "[showActiveFolder] success");
                Folder activeFolder = (Folder)data;

                FolderViewHolder holder = new FolderViewHolder(findViewById(R.id.folderCard));
                holder.bindFolder(activeFolder);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "[showActiveFolder] can't find active folder");
            }
        };

        GetActiveFolderFromDbTask task = new GetActiveFolderFromDbTask(this, activeCaller);
        task.execute();
    }

    private void updateLastSyncViews(SharedPreferences pref) {
        Long lastSyncFolders = pref.getLong(FolderSyncService.LAST_SYNC_KEY, -1);
        Long lastSyncPhotos = pref.getLong(PhotosContentJob.LAST_SYNC_KEY, -1);

        TextView lastSyncFoldersView = (TextView)findViewById(R.id.lastSyncFolders);
        TextView lastSyncPhotosView = (TextView)findViewById(R.id.lastSyncPhotos);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastSyncFolders);

        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy kk:mm:ss");
        SimpleDateFormat.getDateInstance();

        lastSyncFoldersView.setText("Folders last sync: "+sdf.format(cal.getTime()));
        cal.setTimeInMillis(lastSyncPhotos);
        lastSyncPhotosView.setText("Photos last sync: "+sdf.format(cal.getTime()));
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

    /**
     * An asynchronous task that handles the Drive API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
//    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
//        private com.google.api.services.drive.Drive mService = null;
//        private Exception mLastError = null;
//        private Context mContext;
//
//        MakeRequestTask(GoogleAccountCredential credential, Context context) {
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.drive.Drive.Builder(
//                    transport, jsonFactory, credential)
//                    .setApplicationName("Drive API Android Quickstart")
//                    .build();
//            mContext = context;
//        }
//
//        /**
//         * Background task to call Drive API.
//         * @param params no parameters needed for this task.
//         */
//        @Override
//        protected List<String> doInBackground(Void... params) {
//            try {
//                return getDataFromApi();
//            } catch (Exception e) {
//                mLastError = e;
//                cancel(true);
//                return null;
//            }
//        }
//
//        /**
//         * Fetch a list of up to 10 file names and IDs.
//         * @return List of Strings describing files, or an empty list if no files
//         *         found.
//         * @throws IOException
//         */
//        private List<String> getDataFromApi() throws IOException {
//            // Get a list of up to 10 files.
//            Log.d(LOG_TAG, "getDataFromApi started");
//            List<String> fileInfo = new ArrayList<String>();
//            FileList result = mService.files().list()
//                    .setPageSize(30)
//                    .setFields("nextPageToken, files(id, kind, parents, name)")
//                    .execute();
//            Log.d(LOG_TAG, "after mService.execute. result: "+result.getFiles().size());
//            List<File> files = result.getFiles();
//            if (files != null) {
//                for (File file : files) {
//                    fileInfo.add(String.format("%s (%s)\n",
//                            file.getName(), file.getId()));
//                    Log.d(LOG_TAG, "found: "+String.format("%s %s %s (%s)\n",
//                            file.getName(), file.getKind(), file.getParents(), file.getId()));
//
//                }
//            }
//            return fileInfo;
//        }
//
//
//        @Override
//        protected void onPreExecute() {
////            mOutputText.setText("");
////            mProgress.show();
//        }
//
//        @Override
//        protected void onPostExecute(List<String> output) {
////            mProgress.hide();
//            if (output == null || output.size() == 0) {
//                Toast.makeText(mContext, "No results returned.", Toast.LENGTH_SHORT).show();
//            } else {
//                output.add(0, "Data retrieved using the Drive API:");
////                Toast.makeText.setText(TextUtils.join("\n", output));
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
////            mProgress.hide();
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            MainActivity.REQUEST_AUTHORIZATION);
//                } else {
//                    Toast.makeText(mContext, "The following error occurred:\n"
//                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
//                }
//            } else {
//                Toast.makeText(mContext, "Request cancelled.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
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

//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        schedulePhotosJobNougat();
//                    }
//                    else {
                        schedulePhotosJob();
//                    }

                    scheduleFoldersSync();

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
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                schedulePhotosJobNougat();
//            }
//            else {
                schedulePhotosJob();
//            }

            scheduleFoldersSync();
        }
    }

    private void scheduleFoldersSync() {
        Log.d(LOG_TAG, "scheduleFoldersSync started");
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( FOLDERS_JOB_ID,
                new ComponentName( getPackageName(),FolderSyncService.class.getName() ) );

        builder.setPeriodic(JOB_PERIODIC_INTERVAL);

        mJobScheduler.schedule(builder.build());
        syncFoldersNow();
        Log.i(LOG_TAG, "FolderSyncService JOB SCHEDULED!");
    }

    private void syncFoldersNow() {
        Log.d(LOG_TAG, "syncFoldersNow started");
        final ServiceCaller dbCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from InsertNewFoldersTask");
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from InsertNewFoldersTask "+description);
            }
        };

        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {

                final DriveAPIsTask.DriveApiResult result = (DriveAPIsTask.DriveApiResult)data;

                Log.d(LOG_TAG, "received success from GetFoldersTask with "
                        +result.getFolders().length+" shared folders");

                ServiceCaller needToUpdateCaller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {
                        Boolean isNeedToUpdate = (Boolean)data;
                        if (isNeedToUpdate) {
                            //TODO should be on a different thread
                            getApplicationContext().getContentResolver().delete(Constants.FOLDERS_URI, null, null);

                            if (result.getFolders().length > 0) {
                                InsertFoldersToDbTask task = new InsertFoldersToDbTask(getApplicationContext(),
                                        dbCaller);
                                task.execute(result.getFolders());
                            }
                        }
                        else {
                            Log.d(LOG_TAG, "all is synced");
                        }
                    }

                    @Override
                    public void failure(Object data, String description) {
                        Log.e(LOG_TAG, "Error from need to update query");
                    }
                };

                checkIfNeedToUpdate(result.getFolders(), needToUpdateCaller);

            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from GetFoldersTask with: "+description);
            }
        };


        GetFoldersTask task = new GetFoldersTask(getApplicationContext(), caller);
        task.execute();
    }

    private void checkIfNeedToUpdate(Folder[] folders, ServiceCaller caller) {
        IsFoldersChangeTask task = new IsFoldersChangeTask(getApplicationContext(), caller);
        task.execute(folders);
    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    private void schedulePhotosJobNougat() {
//        Log.d(LOG_TAG, "schedulePhotosJobNougat started");
//        mJobScheduler = (JobScheduler)
//                getSystemService( Context.JOB_SCHEDULER_SERVICE );
//
//        JobInfo.Builder builder = new JobInfo.Builder( PHOTOS_JOB_ID,
//                new ComponentName( getPackageName(),PhotosContentJob.class.getName() ) );
//        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(
//        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
//        // Also look for general reports of changes in the overall provider.
//        builder.addTriggerContentUri(new JobInfo.TriggerContentUri(MEDIA_URI, 0));
//
//        mJobScheduler.schedule(builder.build());
//        Log.i(LOG_TAG, "JOB SCHEDULED!");
//    }

    private void schedulePhotosJob() {
        Log.d(LOG_TAG, "schedulePhotosJob started");
        mJobScheduler = (JobScheduler)
                getSystemService( Context.JOB_SCHEDULER_SERVICE );

        JobInfo.Builder builder = new JobInfo.Builder( PHOTOS_JOB_ID,
                new ComponentName(getPackageName(),PhotosContentJob.class.getName() ) );

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

        final SharedPreferences pref = getApplicationContext().getSharedPreferences("app",
                Context.MODE_PRIVATE);

        updateLastSyncViews(pref);
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
                int colFolderIdIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_ID);
                String folderId = cursor.getString(colFolderIdIndex);
                int colCreatedAtIndex = cursor.getColumnIndex(PhotosShareColumns.CREATE_AT);
                int colStartDateIndex = cursor.getColumnIndex(PhotosShareColumns.START_DATE);
                int colEndDateIndex = cursor.getColumnIndex(PhotosShareColumns.END_DATE);
                String createdAt = cursor.getString(colCreatedAtIndex);
                Folder folder = new Folder();
                Log.d(LOG_TAG, "adding "+foldeName);
                folder.setName(foldeName);
                folder.setCreatedAt(Long.valueOf(createdAt));
                folder.setStartDate(cursor.getLong(colStartDateIndex));
                folder.setEndDate(cursor.getLong(colEndDateIndex));
                Log.d(LOG_TAG, "endDate for folder is: "+folder.getEndDate());
                folder.setId(folderId);
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
