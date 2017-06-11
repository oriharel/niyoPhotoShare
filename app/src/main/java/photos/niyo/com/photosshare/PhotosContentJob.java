package photos.niyo.com.photosshare;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static photos.niyo.com.photosshare.MainActivity.PREF_ACCOUNT_NAME;

/**
 * Created by oriharel on 06/06/2017.
 */

public class PhotosContentJob extends JobService{
    public static final String LOG_TAG = PhotosContentJob.class.getSimpleName();

    // The root URI of the media provider, to monitor for generic changes to its content.
    static final Uri MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/");

    // Path segments for image-specific URIs in the provider.
    static final List<String> EXTERNAL_PATH_SEGMENTS
            = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPathSegments();

    // The columns we want to retrieve about a particular image.
    static final String[] PROJECTION = new String[] {
            MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA
    };

    static final int PROJECTION_ID = 0;
    static final int PROJECTION_DATA = 1;
    static final String DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).getPath();
    private static final String[] SCOPES = { DriveScopes.DRIVE };

    GoogleAccountCredential mCredential;
    private com.google.api.services.drive.Drive mService = null;
    JobParameters mRunningParams;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(LOG_TAG, "JOB STARTED!");
        mRunningParams = params;

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
            getResultsFromApi();
        }

        // Instead of real work, we are going to build a string to show to the user.
        StringBuilder sb = new StringBuilder();

        // Did we trigger due to a content change?
        if (params.getTriggeredContentAuthorities() != null) {
            boolean rescanNeeded = false;

            if (params.getTriggeredContentUris() != null) {
                // If we have details about which URIs changed, then iterate through them
                // and collect either the ids that were impacted or note that a generic
                // change has happened.
                ArrayList<String> ids = new ArrayList<>();
                for (Uri uri : params.getTriggeredContentUris()) {
                    List<String> path = uri.getPathSegments();
                    if (path != null && path.size() == EXTERNAL_PATH_SEGMENTS.size()+1) {
                        Log.d(LOG_TAG, "This is a specific file.");
                        ids.add(path.get(path.size()-1));
                    } else {
                        Log.d(LOG_TAG, "Oops, there is some general change!");
                        rescanNeeded = true;
                    }
                }

                if (ids.size() > 0) {
                    // If we found some ids that changed, we want to determine what they are.
                    // First, we do a query with content provider to ask about all of them.
                    StringBuilder selection = new StringBuilder();
                    for (int i=0; i<ids.size(); i++) {
                        if (selection.length() > 0) {
                            selection.append(" OR ");
                        }
                        selection.append(MediaStore.Images.ImageColumns._ID);
                        selection.append("='");
                        selection.append(ids.get(i));
                        selection.append("'");
                    }

                    // Now we iterate through the query, looking at the filenames of
                    // the items to determine if they are ones we are interested in.
                    Cursor cursor = null;
                    boolean haveFiles = false;
                    try {
                        int permissionCheck = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            cursor = getContentResolver().query(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    PROJECTION, selection.toString(), null, null);
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
                                sb.append("\n");
                            }

//                            uploadToGoogleDrive(dir);

                        }
                    } catch (SecurityException e) {
                        Log.e(LOG_TAG, "Error: no access to media!", e);
                        sb.append("Error: no access to media!");
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

            } else {
                // We don't have any details about URIs (because too many changed at once),
                // so just note that we need to do a full rescan.
                rescanNeeded = true;
            }

            if (rescanNeeded) {
                sb.append("Photos rescan needed!");
            }
        } else {
            sb.append("(No photos content)");
        }
        Log.d(LOG_TAG, "onStartJob ended with "+sb);
//        getResultsFromApi();
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
        return true;
    }

    private void getResultsFromApi() {
        Log.d(LOG_TAG, "getDataFromApi started");
        new MakeRequestTask(mCredential).execute();
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
//        private Context mContext;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
//            mContext = context;
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
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
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            Log.d(LOG_TAG, "getDataFromApi started");
            List<String> fileInfo = new ArrayList<String>();
            FileList result = mService.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            Log.d(LOG_TAG, "after mService.execute. result: " + result.getFiles().size());
            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    fileInfo.add(String.format("%s (%s)\n",
                            file.getName(), file.getId()));
                    Log.d(LOG_TAG, "found: " + String.format("%s (%s)\n",
                            file.getName(), file.getId()));
                }
            }
            return fileInfo;
        }


        @Override
        protected void onPreExecute() {
//            mOutputText.setText("");
//            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
//            mProgress.hide();
            if (output == null || output.size() == 0) {
//                Toast.makeText(mContext, "No results returned.", Toast.LENGTH_SHORT).show();
            } else {
                output.add(0, "Data retrieved using the Drive API:");
//                Toast.makeText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
//            mProgress.hide();
            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            MainActivity.REQUEST_AUTHORIZATION);
//                } else {
//                {
                    Log.e(LOG_TAG, "The following error occurred:\n"
                            + mLastError.getMessage());
//                }
                } else {
//                    Toast.makeText(mContext, "Request cancelled.", Toast.LENGTH_SHORT).show();
                    Log.e(LOG_TAG, "Request cancelled");
                }
            }
        }
    }

//    private void uploadToGoogleDrive(final String dir) {
//
//        Log.d(LOG_TAG, "uploadToGoogleDrive started");
//
//        final String folderName = "capriza share";
//
//        Query query =
//                new Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.TITLE, folderName),
//                        Filters.eq(SearchableField.SH, false)))
//                        .build();
//
//        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
//            @Override
//            public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
//                if (!metadataBufferResult.getStatus().isSuccess()) {
//                    Log.e(LOG_TAG, "Cannot create folder in the root.");
//                } else {
//                    boolean isFound = false;
//                    for (Metadata m : metadataBufferResult.getMetadataBuffer()) {
//                        if (m.getTitle().equals(folderName)) {
//                            Log.e(LOG_TAG, "Folder exists");
//                            isFound = true;
//                            DriveId driveId = m.getDriveId();
//                            create_file_in_folder(driveId, dir);
//                            break;
//                        }
//                    }
//                    if (!isFound) {
//                        Log.i(LOG_TAG, "Folder not found; creating it.");
//                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(folderName).build();
//                        Drive.DriveApi.getRootFolder(mGoogleApiClient)
//                                .createFolder(mGoogleApiClient, changeSet)
//                                .setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
//                                    @Override public void onResult(DriveFolder.DriveFolderResult result) {
//                                        if (!result.getStatus().isSuccess()) {
//                                            Log.e(LOG_TAG, "U AR A MORON! Error while trying to create the folder");
//                                        } else {
//                                            Log.i(LOG_TAG, "Created a folder");
//                                            DriveId driveId = result.getDriveFolder().getDriveId();
//                                            create_file_in_folder(driveId, dir);
//                                        }
//                                    }
//                                });
//                    }
//                }
//            }
//        });
//    }

//    private void create_file_in_folder(final DriveId driveId, String fileName) {
//        Log.d(LOG_TAG, "create_file_in_folder, started");
//
//        final File image = new File(fileName);
//
//        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(
//                new ResultCallback<DriveApi.DriveContentsResult>() {
//            @Override
//            public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
//                if (!driveContentsResult.getStatus().isSuccess()) {
//                    Log.e(LOG_TAG, "U AR A MORON! Error while trying to create new file contents");
//                    return;
//                }
//
//                OutputStream outputStream = driveContentsResult.getDriveContents().getOutputStream();
//
//                try {
//                    FileInputStream fileInputStream = new FileInputStream(image);
//                    byte[] buffer = new byte[1024];
//                    int bytesRead;
//                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
//                        outputStream.write(buffer, 0, bytesRead);
//                    }
//                } catch (IOException e1) {
//                    Log.i(LOG_TAG, "U AR A MORON! Unable to write file contents.");
//                }
//
//                MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(image.getName())
//                        .setMimeType("image/jpeg").setStarred(false).build();
//
//                DriveFolder folder = driveId.asDriveFolder();
//                folder.createFile(mGoogleApiClient, changeSet, driveContentsResult.getDriveContents())
//                        .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
//                            @Override public void onResult(@NonNull DriveFolder.DriveFileResult driveFileResult) {
//                                if (!driveFileResult.getStatus().isSuccess()) {
//                                    Log.e(LOG_TAG, "U AR A MORON!  Error while trying to create the file");
//                                    return;
//                                }
//                                Log.v(LOG_TAG, "Created a file: " + driveFileResult.getDriveFile().getDriveId());
//                            }
//                        });
//            }
//        });
//
//    }

    @Override
    public boolean onStopJob(JobParameters params) {
//        mHandler.removeCallbacks(mWorker);
        return false;
    }
}
