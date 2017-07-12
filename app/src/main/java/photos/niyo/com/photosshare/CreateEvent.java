package photos.niyo.com.photosshare;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.google.android.flexbox.FlexboxLayout;
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
import com.robertlevonyan.views.chip.Chip;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import photos.niyo.com.photosshare.db.PhotosShareColumns;
import photos.niyo.com.photosshare.db.User;
import photos.niyo.com.photosshare.tasks.GetFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.GetUsersFromDbTask;
import photos.niyo.com.photosshare.tasks.InsertFoldersToDbTask;
import photos.niyo.com.photosshare.tasks.InsertUsersToDbTask;
import photos.niyo.com.photosshare.tasks.UpdateFolderInDbTask;

import static photos.niyo.com.photosshare.MainActivity.PREF_ACCOUNT_NAME;
import static photos.niyo.com.photosshare.MainActivity.REQUEST_AUTHORIZATION;
import static photos.niyo.com.photosshare.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class CreateEvent extends AppCompatActivity implements DatePickerFragment.DatePickerFragmentListener {

    public static final String LOG_TAG = CreateEvent.class.getSimpleName();
//    private Folder mEditedFolder;
    private static String START_DATE_TAG = "startDate";
    private static String END_DATE_TAG = "endDate";

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

        final Button createBtn = (Button)findViewById(R.id.createFolderBtn);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "createBtn clicked");
                createBtn.setVisibility(View.GONE);
                findViewById(R.id.createProgress).setVisibility(View.VISIBLE);
                EditText eventNameET = (EditText)findViewById(R.id.eventName);
                createOrUpdateEventFolder(eventNameET);
            }
        });

        findViewById(R.id.createProgress).setVisibility(View.GONE);

//        String folderId = getIntent().getStringExtra(PhotosShareColumns.FOLDER_ID);
//
//        ServiceCaller caller = new ServiceCaller() {
//            @Override
//            public void success(Object data) {
//                Folder folder = (Folder)data;
//
//                renderData(folder);
//            }
//
//            @Override
//            public void failure(Object data, String description) {
//
//            }
//        };
//
//        GetFolderFromDbTask task = new GetFolderFromDbTask(this, caller);
//        task.execute(folderId);
        initInviteesEditTextPermission();

        String editedFolderId = getIntent().getStringExtra(PhotosShareColumns.FOLDER_ID);

        ServiceCaller getFolderCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Folder folder = (Folder)data;
                renderData(folder);
                fetchWritersFromDb(folder);
            }

            @Override
            public void failure(Object data, String description) {

            }
        };

        if (editedFolderId != null){
            GetFolderFromDbTask getFolderTask = new GetFolderFromDbTask(this, getFolderCaller);
            getFolderTask.execute(editedFolderId);
            createBtn.setText("Update");
        }

        initDatePickers();

    }

    private void initDatePickers() {
        EditText startDate = (EditText)findViewById(R.id.startText);
        startDate.setFocusable(false);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), START_DATE_TAG);
            }
        });

        EditText endDate = (EditText)findViewById(R.id.endText);
        endDate.setFocusable(false);
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), END_DATE_TAG);
            }
        });
    }

    private void fetchWritersFromDb(Folder editedFolder) {
        ServiceCaller usersCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                User[] folderWriters = (User[])data;
                initWritersContainer(folderWriters);
            }

            @Override
            public void failure(Object data, String description) {

            }
        };

        GetUsersFromDbTask task = new GetUsersFromDbTask(this, usersCaller);

        if (editedFolder.getSharedWith() != null) {
            task.execute(editedFolder.getSharedWith().split(","));
        }
        else {
            Log.d(LOG_TAG, "folder "+editedFolder.getName()+" has no shared with writers");
        }

    }

    private void initWritersContainer(User[] folderWriters) {
        Log.d(LOG_TAG, "initWritersContainer started with "+folderWriters.length+" writers");
        ViewGroup writersGroup = (ViewGroup)findViewById(R.id.writersContainer);
        for (User user :
                folderWriters) {
            Log.d(LOG_TAG, "adding writer "+user.getEmailAddress()+" ("+user.getDisplayName()+")");
            Chip chip = new Chip(this);
            FlexboxLayout.LayoutParams params =
                    new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                            FlexboxLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 10, 10, 10);
            chip.setPadding(10, 10, 10, 10);
            chip.setLayoutParams(params);
            chip.setChipText(user.getDisplayName());
            chip.setClosable(true);
            writersGroup.addView(chip);
        }
    }

    private void initInviteesEditText() {
        final RecipientEditTextView writersEditText =
                (RecipientEditTextView) findViewById(R.id.inviteEditBox);
        writersEditText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter baseRecipientAdapter = new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, this);

        // Queries for all phone numbers. Includes phone numbers marked as "mobile" and "others".
        // If set as true, baseRecipientAdapter will query only for phone numbers marked as "mobile".
        baseRecipientAdapter.setShowMobileOnly(false);

        writersEditText.setAdapter(baseRecipientAdapter);
    }

    private void initInviteesEditTextPermission() {

        Log.d(LOG_TAG, "initInviteesEditText started");
        int permissionCheck = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "needs user to authorize READ_CONTACTS");
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.READ_CONTACTS},
                    Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        else {
            Log.d(LOG_TAG, "permission to READ_CONTACTS already granted");
            // creates an autocomplete for phone number contacts
            initInviteesEditText();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult called with requestCode: "+requestCode);
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                initInviteesEditText();
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void renderData(Folder folder) {
        EditText nameET = (EditText)findViewById(R.id.eventName);
        nameET.setText(folder.getName());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(folder.getStartDate());
        DateFormat sdf = DateFormat.getDateInstance();
        EditText startDate = (EditText)findViewById(R.id.startText);
        startDate.setText(sdf.format(cal.getTime()));

        cal.setTimeInMillis(folder.getEndDate());
        EditText endDate = (EditText)findViewById(R.id.endText);
        endDate.setText(sdf.format(cal.getTime()));

    }

    private void createPermission() {
        Log.d(LOG_TAG, "createPermission started");
        final View v = findViewById(R.id.createEventContainer);
        if (mFolderId != null) {
            String[] SCOPES = { DriveScopes.DRIVE };

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
            SharedPreferences pref = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
            String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credential.setSelectedAccountName(accountName);

                final ServiceCaller usersCaller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {
                        Log.d(LOG_TAG, "folder created and permissions granted");
                        finish();
                    }

                    @Override
                    public void failure(Object data, String description) {
                        findViewById(R.id.createProgress).setVisibility(View.GONE);
                        findViewById(R.id.createFolderBtn).setVisibility(View.VISIBLE);
                    }
                };


                ServiceCaller caller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {
                        User[] users = (User[])data;

                        InsertUsersToDbTask usersTask = new InsertUsersToDbTask(v.getContext(), usersCaller);
                        usersTask.execute(users);
                    }

                    @Override
                    public void failure(Object data, String description) {
                        findViewById(R.id.createProgress).setVisibility(View.GONE);
                        findViewById(R.id.createFolderBtn).setVisibility(View.VISIBLE);
                    }
                };
                new MakePermissionRequest(credential, caller, v).execute(mFolderId);
            }
        }
        else {
            Log.e(LOG_TAG, "Error. folder id is null");
        }
    }

    @Override
    public void onFinishDatePickerDialog(int year, int month, int day, String tag) {
        int viewId = tag.equals(START_DATE_TAG) ? R.id.startText : R.id.endText;
        EditText dateText = (EditText)findViewById(viewId);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        DateFormat sdf = DateFormat.getDateInstance();
        dateText.setText(sdf.format(cal.getTime()));
        dateText.setTag(String.valueOf(cal.getTimeInMillis()));
    }

    private class MakePermissionRequest extends AsyncTask<String, Void, User[]> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private Context mContext;
        private View mV;
        ServiceCaller mCaller;

        MakePermissionRequest(GoogleAccountCredential credential, ServiceCaller caller, View v) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Log.d(LOG_TAG, "making request with credentials: "+credential.getSelectedAccountName());
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            mContext = v.getContext();
            mCaller = caller;
            mV = v;
        }

        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                Log.e(LOG_TAG, "mService.permissions(): "+e.getMessage());
                Snackbar.make(mV, "Unable to grant permission", Snackbar.LENGTH_LONG);
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                Log.d(LOG_TAG, "mService.permissions() Permission ID: " + permission.getId());
                Snackbar.make(mV, "Permission granted for "+permission.getEmailAddress(), Snackbar.LENGTH_LONG);
            }
        };

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected User[] doInBackground(String... params) {
            Log.d(LOG_TAG, "[MakePermissionRequest] doInBackground started");
            try {
                return createPermissionBatch(params[0]);
            } catch (Exception e) {
                Log.e(LOG_TAG, "[MakePermissionRequest] Error!! " ,e);
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
        private User[] createPermissionBatch(String fileId) throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "createPermissionBatch started");
            BatchRequest batch = mService.batch();
            RecipientEditTextView et = (RecipientEditTextView)findViewById(R.id.inviteEditBox);
            DrawableRecipientChip[] recipientChips = et.getRecipients();
            List<User> users = new ArrayList<>();
            for (DrawableRecipientChip recipient :
                    recipientChips) {
                Log.d(LOG_TAG, "creating permission to "+recipient.getEntry().getDestination());
                Permission userPermission = new Permission()
                        .setType("user")
                        .setRole("writer")
                        .setEmailAddress(recipient.getEntry().getDestination());
                mService.permissions().create(fileId, userPermission)
                        .setFields("id")
                        .queue(batch, callback);

                User dbUser = new User();
                dbUser.setEmailAddress(recipient.getEntry().getDestination());
                dbUser.setDisplayName(recipient.getEntry().getDisplayName());
//                dbUser.setPhotoLink(recipient.getEntry().getPhotoBytes());
                users.add(dbUser);
            }

            batch.execute();
            User[] result = new User[users.size()];
            return users.toArray(result);
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(User[] result) {
            if (result != null) {
                mCaller.success(result);
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
//        getMenuInflater().inflate(R.menu.menu_create_event, menu);
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


    private void createOrUpdateEventFolder(EditText eventNameET) {
        Log.d(LOG_TAG, "createOrUpdateEventFolder started");
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
                    Log.d(LOG_TAG, "success creating folder in drive. now db...");
                    Folder createdFolder = (Folder)data;
                    if (getIntent().getStringExtra(PhotosShareColumns.FOLDER_ID) != null) {
                        updateFolderToDb(createdFolder);
                    }
                    else {
                        insertNewFolderToDb(createdFolder);
                    }

                }

                @Override
                public void failure(Object data, String description) {
                    Log.e(LOG_TAG, "Error occured while creating folder in Drive");
                }
            };
            String[] params = new String[3];
            params[0] = folderName;
            EditText startDateText = (EditText)findViewById(R.id.startText);
            EditText endDateText = (EditText)findViewById(R.id.endText);
            params[1] = (String)startDateText.getTag();
            params[2] = (String)endDateText.getTag();
            new MakeRequestTask(credential, caller, this).execute(params);
        }
        else {
            findViewById(R.id.createProgress).setVisibility(View.GONE);
            findViewById(R.id.createFolderBtn).setVisibility(View.VISIBLE);
        }
    }

    private void updateFolderToDb(final Folder createdFolder) {
        Log.d(LOG_TAG, "updateFolderToDb started");
        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "folder "+createdFolder.getName()+" created successfully");
                createPermission();
            }

            @Override
            public void failure(Object data, String description) {
                findViewById(R.id.createProgress).setVisibility(View.GONE);
                findViewById(R.id.createFolderBtn).setVisibility(View.VISIBLE);
            }
        };

        UpdateFolderInDbTask task = new UpdateFolderInDbTask(this, caller);
        task.execute(createdFolder);
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
                return createFolder(params);
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
        private Folder createFolder(String... params) throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "createFolder started");

            Folder createdFolder = new Folder();
            createdFolder.setName(params[0]);
            Calendar cal = Calendar.getInstance();
            Long now = cal.getTimeInMillis();
//            Long startDate = now;
//            cal.add(Calendar.DAY_OF_WEEK, 3);
//            Long end = cal.getTimeInMillis();


            File fileMetadata = new File();
            fileMetadata.setName(params[0]);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            Map<String, String> props = new HashMap<>();
            props.put(Folder.APP_ID, getPackageName());
            props.put("start_date", params[1]);
            props.put("end_date", params[2]);
            props.put("created_at", now.toString());
            fileMetadata.setAppProperties(props);
            File file;
            String editedFolderId = getIntent().getStringExtra(PhotosShareColumns.FOLDER_ID);

            if (editedFolderId != null) {
                file = mService.files().update(editedFolderId, fileMetadata).execute();
            }
            else {
                file = mService.files().create(fileMetadata)
                        .setFields("id")
                        .execute();
            }

            Log.d(LOG_TAG, "createFolder finished with file id: "+file.getId());

            mFolderId = file.getId();

            createdFolder.setId(file.getId());
            createdFolder.setCreatedAt(now);
            createdFolder.setStartDate(Long.valueOf(params[1]));
            createdFolder.setEndDate(Long.valueOf(params[2]));
            SharedPreferences pref = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
            String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
            createdFolder.setOwners(accountName);
            EditText et = (EditText)findViewById(R.id.inviteEditBox);
            String inviteesStr = et.getEditableText().toString();
            createdFolder.setSharedWith(inviteesStr);

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
                    createOrUpdateEventFolder(eventNameET);
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
                createPermission();
            }

            @Override
            public void failure(Object data, String description) {
                findViewById(R.id.createProgress).setVisibility(View.GONE);
                findViewById(R.id.createFolderBtn).setVisibility(View.VISIBLE);
            }
        };

        InsertFoldersToDbTask task = new InsertFoldersToDbTask(this, caller);
        task.execute(createdFolder);
    }
}
