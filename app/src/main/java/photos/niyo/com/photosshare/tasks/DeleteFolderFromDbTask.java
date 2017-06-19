package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 20/06/2017.
 */

public class DeleteFolderFromDbTask extends AsyncTask<Folder, Void, Boolean> {

    public static final String LOG_TAG = DeleteFolderFromDbTask.class.getSimpleName();
    private Context mContext;
    private ServiceCaller mCaller;

    public DeleteFolderFromDbTask(Context context, ServiceCaller caller) {

        mContext = context;
        mCaller = caller;

    }
    @Override
    protected Boolean doInBackground(Folder... params) {
        Log.d(LOG_TAG, "doInBackground started with "+params[0].getName());

        String selection = PhotosShareColumns.FOLDER_ID+"='"+params[0].getId()+"'";

        int rowsDeleted = mContext.getContentResolver().delete(Constants.FOLDERS_URI, selection, null);
        return rowsDeleted == 1;
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
}
