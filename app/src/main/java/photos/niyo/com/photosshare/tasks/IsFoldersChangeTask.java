package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 19/06/2017.
 */

public class IsFoldersChangeTask extends AsyncTask<Folder, Void, Boolean> {
    public static final String LOG_TAG = IsFoldersChangeTask.class.getSimpleName();
    private Context mContext;
    private ServiceCaller mCaller;
    private Exception mLastError;

    public IsFoldersChangeTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }
    @Override
    protected Boolean doInBackground(Folder... params) {
        Log.d(LOG_TAG, "doInBackground started with "+params.length+" folders");
        Cursor cursor = mContext.getContentResolver().query(Constants.FOLDERS_URI, null, null, null, null);

        List<String> folderIds = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    int colFolderIdIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_ID);
                    String folderId = cursor.getString(colFolderIdIndex);
                    folderIds.add(folderId);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }

        if (folderIds.size() != params.length) {
            return true;
        }

        for (Folder folder : params) {
            if (!folderIds.contains(folder.getId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onCancelled() {
        Log.e(LOG_TAG, "task was cancelled");
        mCaller.failure(mLastError, "Something is wrong");

    }

    @Override
    protected void onPostExecute(Boolean isNeedToUpdate) {
        Log.d(LOG_TAG, "onPostExecute ended with "+isNeedToUpdate);
        mCaller.success(isNeedToUpdate);
    }
}
