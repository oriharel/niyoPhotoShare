package photos.niyo.com.photosshare;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import photos.niyo.com.photosshare.db.InsertNewFoldersTask;

import static photos.niyo.com.photosshare.MainActivity.PREF_ACCOUNT_NAME;
import static photos.niyo.com.photosshare.MainActivity.REQUEST_AUTHORIZATION;
import static photos.niyo.com.photosshare.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class CreateEvent extends AppCompatActivity  {

    public static final String LOG_TAG = CreateEvent.class.getSimpleName();

    String mFolderId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.createEventToolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        if (ab != null) {
            // Enable the Up button
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Button createBtn = (Button)findViewById(R.id.createFolderBtn);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText eventNameET = (EditText)findViewById(R.id.eventName);
                createEventFolder(eventNameET);
            }
        });

        Button permButton = (Button)findViewById(R.id.permBtn);
        permButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPermission();
            }
        });


    }

    private void createPermission() {
        if (mFolderId != null) {
            String[] SCOPES = { DriveScopes.DRIVE };

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
            SharedPreferences pref = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
            String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);
                ServiceCaller caller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {

                    }

                    @Override
                    public void failure(Object data, String description) {

                    }
                };
                new MakePermissionRequest(credential, caller, this).execute(mFolderId);
            }



        }
        else {
            Log.e(LOG_TAG, "Error. folder id is null");
        }
    }

    private class MakePermissionRequest extends AsyncTask<String, Void, Boolean> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private Context mContext;
        ServiceCaller mCaller;

        MakePermissionRequest(GoogleAccountCredential credential, ServiceCaller caller, Context context) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Log.d(LOG_TAG, "making request with credentials: "+credential.getSelectedAccountName());
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            mContext = context;
            mCaller = caller;
        }

        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                Log.e(LOG_TAG, "mService.permissions(): "+e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                Log.d(LOG_TAG, "mService.permissions() Permission ID: " + permission.getId());
            }
        };

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected Boolean doInBackground(String... params) {
            Log.d(LOG_TAG, "[MakePermissionRequest] doInBackground started");
            try {
                return createPermissionBatch(params[0]);
            } catch (Exception e) {
                Log.e(LOG_TAG, "[MakePermissionRequest] Error!! " ,e);
                mLastError = e;
                cancel(true);
                return false;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private Boolean createPermissionBatch(String fileId) throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "createPermissionBatch started");
            BatchRequest batch = mService.batch();
            Permission userPermission = new Permission()
                    .setType("user")
                    .setRole("writer")
                    .setEmailAddress("ori.harel@capriza.com");
            mService.permissions().create(fileId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);
            batch.execute();
            return true;
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mCaller.success(true);
            }
            else {
                mCaller.failure(result, "error");
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(mContext, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(mContext, "Request cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_event, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_submit) {
            finish();
            return true;
        }
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LOG_TAG, "checking if api client exists");

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void createEventFolder(EditText eventNameET) {
        Log.d(LOG_TAG, "createEventFolder started");
        String[] SCOPES = { DriveScopes.DRIVE };
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        SharedPreferences pref = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
        String folderName = eventNameET.getText().toString();
        if (accountName != null) {
            credential.setSelectedAccountName(accountName);

            ServiceCaller caller = new ServiceCaller() {
                @Override
                public void success(Object data) {
                    Folder createdFolder = (Folder)data;
                    insertNewFolderToDb(createdFolder);
                }

                @Override
                public void failure(Object data, String description) {
                    Log.e(LOG_TAG, "Error occured while creating folder in Drive");
                }
            };

            new MakeRequestTask(credential, caller, this).execute(folderName);
        }
    }

    private class MakeRequestTask extends AsyncTask<String, Void, Folder> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private Context mContext;
        ServiceCaller mCaller;

        MakeRequestTask(GoogleAccountCredential credential, ServiceCaller caller, Context context) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Log.d(LOG_TAG, "making request with credentials: "+credential.getSelectedAccountName());
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            mContext = context;
            mCaller = caller;
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected Folder doInBackground(String... params) {
            Log.d(LOG_TAG, "[MakeRequestTask] doInBackground started");
            try {
                return createFolder(params[0]);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error!! " ,e);
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         * @return List of Strings describing files, or an empty list if no files
         *         found.
         * @throws IOException
         */
        private Folder createFolder(String folderName) throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "createFolder started");

            Folder createdFolder = new Folder();
            createdFolder.setName(folderName);
            Calendar cal = Calendar.getInstance();
            Long now = cal.getTimeInMillis();
            Long startDate = now;
            cal.add(Calendar.DAY_OF_WEEK, 3);
            Long end = cal.getTimeInMillis();


            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            Map<String, String> props = new HashMap<>();
            props.put(Folder.APP_ID, getPackageName());
            props.put("start_date", startDate.toString());
            props.put("end_date", end.toString());
            props.put("created_at", now.toString());
            fileMetadata.setAppProperties(props);

            File file = mService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            Log.d(LOG_TAG, "createFolder finished with file id: "+file.getId());

            mFolderId = file.getId();

            createdFolder.setId(file.getId());
            createdFolder.setCreatedAt(now);
            createdFolder.setStartDate(startDate);
            createdFolder.setEndDate(end);
            return createdFolder;
        }


        @Override
        protected void onPostExecute(Folder result) {
            if (result != null) {
                mCaller.success(result);
            }
            else {
                mCaller.failure(null, "error");
            }
        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(mContext, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(mContext, "Request cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                } else {
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    EditText eventNameET = (EditText)findViewById(R.id.eventName);
                    createEventFolder(eventNameET);
                }
                break;
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
                CreateEvent.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void insertNewFolderToDb(final Folder createdFolder) {
        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "folder "+createdFolder.getName()+" created successfully");
            }

            @Override
            public void failure(Object data, String description) {

            }
        };

        InsertNewFoldersTask task = new InsertNewFoldersTask(this, caller);
        task.execute(createdFolder);
    }
}
