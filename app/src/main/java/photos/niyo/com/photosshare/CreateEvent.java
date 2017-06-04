package photos.niyo.com.photosshare;

import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;

public class CreateEvent extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    public static final String LOG_TAG = CreateEvent.class.getSimpleName();

    private void CreateFolderOnGoogleDrive(DriveApi.DriveContentsResult result) {
        final DriveContents driveContents = result.getDriveContents();

    }

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
            EditText eventNameET = (EditText)findViewById(R.id.eventName);
            createEventFolder(eventNameET);
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
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {

            // disconnect Google Android Drive API connection.
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
            ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFolderResult result) {
                    Log.d(LOG_TAG, "folder creation returned");
                    if (!result.getStatus().isSuccess()) {
                        Snackbar.make(findViewById(R.id.eventName),
                                "Error while trying to create the folder", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        return;
                    }
                    Snackbar.make(findViewById(R.id.eventName), "Created a folder: " +
                            result.getDriveFolder().getDriveId(),
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            };

    private void createEventFolder(EditText eventNameET) {
        Log.d(LOG_TAG, "createEventFolder started");
        CustomPropertyKey appKey = new CustomPropertyKey("originator", CustomPropertyKey.PRIVATE);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(eventNameET.getText().toString())
                .setCustomProperty(appKey, getPackageName()).build();
        Log.d(LOG_TAG, "creating folder "+eventNameET.getText());
        Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                mGoogleApiClient, changeSet).setResultCallback(folderCreatedCallback);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Snackbar.make(findViewById(R.id.eventName), "Connected", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Called whenever the API client fails to connect.
        Log.i(LOG_TAG, "GoogleApiClient connection failed: " + connectionResult.toString());

        if (!connectionResult.hasResolution()) {

            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }

        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */

        try {

            connectionResult.startResolutionForResult(this, 0);

        } catch (IntentSender.SendIntentException e) {

            Log.e(LOG_TAG, "Exception while starting resolution activity", e);
        }
    }
}
