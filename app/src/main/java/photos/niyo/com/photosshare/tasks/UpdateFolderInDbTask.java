package photos.niyo.com.photosshare.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 23/06/2017.
 */

public class UpdateFolderInDbTask extends AsyncTask<Folder, Void, Boolean> {
    public static final String LOG_TAG = UpdateFolderInDbTask.class.getSimpleName();

    private Context mContext;
    private ServiceCaller mCaller;
    private Exception mLastError;

    public UpdateFolderInDbTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }

    @Override
    protected Boolean doInBackground(Folder... params) {
        Log.d(LOG_TAG, "doInBackground started");

        try{
            for (Folder folder : params) {
                ContentValues values = new ContentValues();

                values.put(PhotosShareColumns.FOLDER_ID, folder.getId());
                values.put(PhotosShareColumns.FOLDER_NAME, folder.getName());
                values.put(PhotosShareColumns.CREATE_AT, folder.getCreatedAt());
                values.put(PhotosShareColumns.START_DATE, folder.getStartDate());
                values.put(PhotosShareColumns.END_DATE, folder.getEndDate());
                Log.d(LOG_TAG, "inserting folder "+folder.getName()+" with sharedWith: "+ folder.getSharedWith());
                values.put(PhotosShareColumns.SHARED_WITH, folder.getSharedWith());
                int numOrRowsUpdated = mContext.getContentResolver().update(Constants.FOLDERS_URI, values, PhotosShareColumns.FOLDER_ID+"='"+folder.getId()+"'", null);

                Log.d(LOG_TAG, "updated a folder name "+folder.getName()+" result was "+numOrRowsUpdated+" rows updated");
            }
        }
        catch (Exception ex) {
            Log.e(LOG_TAG, "exception thrown in updated folder in db", ex);
            mLastError = ex;
            cancel(true);
        }


        return true;
    }

    @Override
    protected void onCancelled() {
        Log.e(LOG_TAG, "update task cancelled", mLastError);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            mCaller.success(true);
        }
    }
}
