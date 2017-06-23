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

public class InsertFoldersToDbTask extends AsyncTask<Folder, Void, Boolean> {
    public static final String LOG_TAG = InsertFoldersToDbTask.class.getSimpleName();
    Context _context;
    ServiceCaller _caller;

    public InsertFoldersToDbTask(Context context, ServiceCaller caller) {
        _context = context;
        _caller = caller;
    }
    @Override
    protected Boolean doInBackground(Folder... params) {
        for (Folder folder : params) {
            ContentValues values = new ContentValues();

            values.put(PhotosShareColumns.FOLDER_ID, folder.getId());
            values.put(PhotosShareColumns.FOLDER_NAME, folder.getName());
            values.put(PhotosShareColumns.CREATE_AT, folder.getCreatedAt());
            values.put(PhotosShareColumns.START_DATE, folder.getStartDate());
            values.put(PhotosShareColumns.END_DATE, folder.getEndDate());
            values.put(PhotosShareColumns.SHARED_WITH, folder.getSharedWith());
            Uri result = _context.getContentResolver().insert(Constants.FOLDERS_URI, values);

            Log.d(LOG_TAG, "added a folder name "+folder.getName()+" result was "+result);
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(LOG_TAG, "friends db insertion succeeded");

        _caller.success(result);
    }
}
