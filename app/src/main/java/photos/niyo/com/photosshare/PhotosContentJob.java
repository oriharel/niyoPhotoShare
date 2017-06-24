package photos.niyo.com.photosshare;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;
import pub.devrel.easypermissions.EasyPermissions;

import static photos.niyo.com.photosshare.MainActivity.PREF_ACCOUNT_NAME;

/**
 * Created by oriharel on 06/06/2017.
 */

public class PhotosContentJob extends JobService{
    public static final String LOG_TAG = PhotosContentJob.class.getSimpleName();

    // The root URI of the media provider, to monitor for generic changes to its content.
    static final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");

    // Path segments for image-specific URIs in the provider.
//    static final List<String> EXTERNAL_PATH_SEGMENTS
//            = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPathSegments();

    // The columns we want to retrieve about a particular image.
    static final String[] PROJECTION = new String[] {
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN
    };

    static final int PROJECTION_ID = 0;
    static final int PROJECTION_DATA = 1;
    static final String DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath();
    private static final String[] SCOPES = { DriveScopes.DRIVE };
    private static final String LAST_SYNC_IN_MILLIS = "lastSyncInMillis";
    public static final String LAST_SYNC_KEY = "last_sync_photos";

    GoogleAccountCredential mCredential;
    private com.google.api.services.drive.Drive mService = null;
    JobParameters mRunningParams;

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.i(LOG_TAG, "JOB STARTED!");
        mRunningParams = params;

        initJob();

        ServiceCaller activeFolderCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Folder activeFolder = (Folder)data;
                Log.d(LOG_TAG, String.format("found active folder %s", activeFolder.getName()));

                //get new photos
                List<String> photoNames = getNewPhotosFileNames(params,
                        activeFolder.getStartDate());

                if (photoNames.size() > 0) {
                    Log.d(LOG_TAG, "uploading "+photoNames.size()+"" +
                            " new photos to "+activeFolder.getName());
                    //upload new photos to drive
                    uploadNewPhotosToDrive(photoNames, activeFolder.getId(), params);
                }
                else {
                    Log.d(LOG_TAG, "no photos to upload");
                    jobFinished(params, false);
                }

            }

            @Override
            public void failure(Object data, String description) {
                Log.d(LOG_TAG, String.format("no active folder %s", description));
                jobFinished(params, true);
            }
        };

        GetActiveFolderFromDbTask folderTask = new GetActiveFolderFromDbTask(this, activeFolderCaller);
        folderTask.execute();

        Log.d(LOG_TAG, "onStartJob ended");

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_SYNC_KEY, Calendar.getInstance().getTimeInMillis());
        editor.apply();
        return true;
    }

    private void initJob() {
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Drive API Android Quickstart")
                .build();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("app", Context.MODE_PRIVATE);
        String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            mCredential.setSelectedAccountName(accountName);
        }
    }

    private void uploadNewPhotosToDrive(final List<String> photoNames,
                                        String folderId,
                                        final JobParameters params) {
        Log.d(LOG_TAG, "uploadNewPhotosToDrive started with folder Id: "+folderId);

        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "success in uploading to drive!!");

                SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(LAST_SYNC_IN_MILLIS, Calendar.getInstance().getTimeInMillis());
                editor.apply();

                jobFinished(params, false);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "fail to upload "+photoNames.get(0));
                jobFinished(params, false);
            }
        };

        if (photoNames.size() > 0) {
            new UploadToGoogleDriveTask(mCredential, caller).execute(photoNames.get(0), folderId);
        }
        else {
            jobFinished(params, false);
        }
    }

    private class UploadToGoogleDriveTask extends AsyncTask<String, Void, Boolean> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private ServiceCaller _caller;

        UploadToGoogleDriveTask(GoogleAccountCredential credential, ServiceCaller caller) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            _caller = caller;
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Boolean doInBackground(String... params) {
            Log.d(LOG_TAG, "[UploadToGoogleDriveTask] started with fileName: "+
                    params[0].toString()+" folder id: "+params[1].toString());
            try {
                return uploadToDrive(params[0], params[1]);
            } catch (Exception e) {
                mLastError = e;
                Log.e(LOG_TAG, "[UploadToGoogleDriveTask] ERROR!! ", e);
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of up to 10 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private Boolean uploadToDrive(String fileName, String folderId) throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "uploadToDrive started");
            Log.d(LOG_TAG, "uploading "+fileName+" to google drive");
            Boolean result = false;
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(folderId));
            java.io.File filePath = new java.io.File(fileName);
            FileContent mediaContent = new FileContent("image/jpeg", filePath);
            File file = null;
            try {
                file = mService.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();
                result = true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "[uploadNewPhotosToDrive] Error ", e);
            }
            return result;
        }


        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Boolean output) {
//            mProgress.hide();
            if (output) _caller.success(output);
            else _caller.failure(output, "fail to upload");
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    Log.e(LOG_TAG, "The following error occurred:\n"
                            + mLastError.getMessage());
                } else {
                    Log.e(LOG_TAG, "Request cancelled");
                }
            }
        }
    }

    private List<String> getNewPhotosFileNames(JobParameters params, Long startDate) {
        Log.d(LOG_TAG, "getNewPhotosFileNames started");
        String selection;
        List<String> result = new ArrayList<>();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            selection = getNewPhotosNougat(params);
//        }
//        else {
            selection = getNewPhotosLegacySelectionString(startDate);
//        }

        Cursor cursor = null;
        boolean haveFiles = false;
        StringBuilder sb = new StringBuilder();
        try {
            if (EasyPermissions.hasPermissions(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d(LOG_TAG, "querying with selection: "+selection);
                cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION, selection, null, null);
            }
            else {
                Log.e(LOG_TAG, "no permission READ_EXTERNAL_STORAGE");
            }

            while (cursor != null && cursor.moveToNext()) {
                // We only care about files in the DCIM directory.
                String dir = cursor.getString(PROJECTION_DATA);
                if (dir.startsWith(DCIM_DIR)) {
                    if (!haveFiles) {
                        haveFiles = true;
                        sb.append("New photos:\n");
                    }
                    sb.append(cursor.getInt(PROJECTION_ID));
                    sb.append(": ");
                    sb.append(dir);
                    result.add(dir);
                    sb.append("\n");
                }
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Error: no access to media!", e);
            sb.append("Error: no access to media!");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(LOG_TAG, "query result: "+sb);
        Log.d(LOG_TAG, "getNewPhotosFileNames ended with "+result.size()+" length");
        return result;
    }

    private String getNewPhotosLegacySelectionString(Long startDate) {
        Log.d(LOG_TAG, "getNewPhotosLegacySelectionString started");
        SharedPreferences pref = getSharedPreferences("app", MODE_PRIVATE);
        Long lastSync = pref.getLong(LAST_SYNC_IN_MILLIS, startDate);
        String selection = MediaStore.Images.ImageColumns.DATE_TAKEN+" > "+lastSync;
        Log.d(LOG_TAG, "selection clause is: "+selection);
        return selection;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
