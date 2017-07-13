package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Calendar;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 22/06/2017.
 */

public class GetFolderFromDbTask extends AsyncTask<String, Void, Folder> {

    private Context mContext;
    private ServiceCaller mCaller;
    public static final String LOG_TAG = GetFolderFromDbTask.class.getSimpleName();

    public GetFolderFromDbTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }

    @Override
    protected Folder doInBackground(String... params) {
        Log.d(LOG_TAG, "doInBackground started");
        Folder result = null;
        String selection = getSelection(params);

        Log.d(LOG_TAG, String.format("querying db for active folder %s", selection));

        Cursor cursor = mContext.getContentResolver().query(Constants.FOLDERS_URI,
                Constants.FOLDERS_PROJECTION,
                selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = new Folder();
                int colFolderIdIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_ID);
                int colFolderNameIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_NAME);
                int colFolderStartDate = cursor.getColumnIndex(PhotosShareColumns.START_DATE);
                int colFolderEndDate = cursor.getColumnIndex(PhotosShareColumns.END_DATE);
                int colFolderSharedWithIndex = cursor.getColumnIndex(PhotosShareColumns.SHARED_WITH);
                int colOwnersIndex = cursor.getColumnIndex(PhotosShareColumns.OWNERS);

                result.setId(cursor.getString(colFolderIdIndex));
                result.setName(cursor.getString(colFolderNameIndex));
                result.setStartDate(cursor.getLong(colFolderStartDate));
                result.setEndDate(cursor.getLong(colFolderEndDate));
                result.setSharedWith(cursor.getString(colFolderSharedWithIndex));
                result.setOwners(cursor.getString(colOwnersIndex));

                Log.d(LOG_TAG, "[GetActiveFolderTask] found folder: "+result);
            }
            else {
                Log.d(LOG_TAG, "cursor is empty");
            }

            cursor.close();
        }
        else {
            Log.e(LOG_TAG, "Error cursor from getFolder is null");
        }

        return result;
    }

    @Override
    protected void onPostExecute(Folder folder) {
        if (folder != null && folder.getId() != null) {
            mCaller.success(folder);
        }
        else {
            Log.d(LOG_TAG, "no active folder");
            if (folder == null) {
                Log.d(LOG_TAG, "folder is null");
            }
            else if (folder.getId() == null) {
                Log.e(LOG_TAG, "folder id is null");
            }
            mCaller.failure(null, "no active folder found");
        }
    }

    protected String getSelection(String... params) {
        String folderId = params[0];
        return PhotosShareColumns.FOLDER_ID+"='"+folderId+"'";
    }
}
