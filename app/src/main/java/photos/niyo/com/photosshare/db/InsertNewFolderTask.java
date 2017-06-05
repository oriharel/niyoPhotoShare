package photos.niyo.com.photosshare.db;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;

/**
 * Created by oriharel on 05/06/2017.
 */

public class InsertNewFolderTask extends AsyncTask<Folder, Void, Boolean> {
    public static final String LOG_TAG = InsertNewFolderTask.class.getSimpleName();
    Context _context;
    ServiceCaller _caller;

    public InsertNewFolderTask(Context context, ServiceCaller caller) {
        _context = context;
        _caller = caller;
    }
    @Override
    protected Boolean doInBackground(Folder... params) {
        Folder folder = params[0];
        ContentValues values = new ContentValues();

        values.put(PhotosShareColumns.FOLDER_NAME, folder.getName());
        values.put(PhotosShareColumns.CREATE_AT, folder.getCreatedAt());
        Uri result = _context.getContentResolver().insert(Constants.FOLDERS_URI, values);

        Log.d(LOG_TAG, "added a friend name "+folder.getName()+" result was "+result.toString());
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(LOG_TAG, "friends db insertion succeeded");

        _caller.success(result);
    }
}
