package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Arrays;

import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;

import static photos.niyo.com.photosshare.MainActivity.PREF_ACCOUNT_NAME;

/**
 * Created by oriharel on 18/06/2017.
 */

public abstract class DriveAPIsTask extends AsyncTask<Folder, Void, DriveAPIsTask.DriveApiResult> {
    private static final String LOG_TAG = DriveAPIsTask.class.getSimpleName();
    protected com.google.api.services.drive.Drive mService = null;
    private ServiceCaller mCaller;
    protected Context mContext;
    private GoogleAccountCredential mCredentials;
    private Exception mLastError = null;

    public DriveAPIsTask(Context context, ServiceCaller caller) {


        String[] SCOPES = {DriveScopes.DRIVE};
        mCredentials = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mContext = context;
        mCaller = caller;
    }

    @Override
    protected DriveApiResult doInBackground(Folder... params) {
        SharedPreferences pref = mContext.getSharedPreferences("app", Context.MODE_PRIVATE);
        String accountName = pref.getString(PREF_ACCOUNT_NAME, null);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        if (accountName != null) {
            mCredentials.setSelectedAccountName(accountName);

            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, mCredentials)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();

            try {
                return actualDoInBackground(params);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error!! " ,e);
                mLastError = e;
                cancel(true);
                return null;
            }

        }
        else {
            mLastError = new Exception("Account not set yet");
            cancel(true);
            return null;
        }
    }

    protected abstract DriveApiResult actualDoInBackground(Folder... params) throws IOException;

    @Override
    protected void onCancelled() {
        Log.e(LOG_TAG, "task was cancelled");
        mCaller.failure(mLastError, "error");
    }

    @Override
    protected void onPostExecute(DriveApiResult result) {
        if (result.getResult()) {
            mCaller.success(result);
        }
        else {
            mCaller.failure(result, "error");
        }
    }

    public class DriveApiResult {
        private Folder[] mFolders;
        private Boolean mResult = false;
        private String message;
        private Exception exception;
        private FileList mFileList;

        public Folder[] getFolders() {
            return mFolders;
        }

        public void setFolders(Folder[] mFolder) {
            this.mFolders = mFolder;
        }

        public Boolean getResult() {
            return mResult;
        }

        public void setResult(Boolean mResult) {
            this.mResult = mResult;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public FileList getFileList() {
            return mFileList;
        }

        public void setFileList(FileList mFileList) {
            this.mFileList = mFileList;
        }

        public String getMessage() {
            return message;
        }
    }
}
