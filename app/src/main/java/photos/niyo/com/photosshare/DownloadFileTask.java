package photos.niyo.com.photosshare;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import photos.niyo.com.photosshare.db.UsersColumns;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;

/**
 * Created by oriharel on 27/06/2017.
 */

public class DownloadFileTask extends DriveAPIsTask {
    public static final String LOG_TAG = DownloadFileTask.class.getSimpleName();
    private String mFileId;
    public static final String LATEST_FILE_NAME = "latestFile.jpg";
    private String mOwnerEmail;
    public DownloadFileTask(Context context, ServiceCaller caller, String fileId, String ownerEmail) {
        super(context, caller);
        mFileId = fileId;
        mOwnerEmail = ownerEmail;
    }

    @Override
    protected DriveApiResult actualDoInBackground(Folder... params) throws IOException {
        Log.d(LOG_TAG, "actualDoInBackground started with fileId: "+mFileId+" owner: "+mOwnerEmail);
        FileOutputStream outputStream;
        DriveApiResult result = new DriveApiResult();
        try {
            outputStream = mContext.openFileOutput(LATEST_FILE_NAME, Context.MODE_PRIVATE);
            mService.files().get(mFileId)
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.close();
            Log.d(LOG_TAG, "file download finished");
            Cursor cursor = mContext.getContentResolver().query(Constants.USERS_URI,
                    Constants.USERS_PROJECTION,
                    UsersColumns.EMAIL_ADDRESS+"='"+mOwnerEmail+"'",
                    null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String displayName = cursor.getString(cursor.getColumnIndex(UsersColumns.DISPLAY_NAME));
                    result.setMessage(displayName);
                }
                cursor.close();
            }
            result.setResult(true);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error downloading file: "+mFileId, e);
            result.setResult(false);
        }

        return result;
    }
}
