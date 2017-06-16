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
 * Created by oriharel on 15/06/2017.
 */

public class GetActiveFolderTask extends AsyncTask<Void, Void, Folder> {
    private Context mContext;
    private ServiceCaller mCaller;
    public static final String LOG_TAG = GetActiveFolderTask.class.getSimpleName();

    public GetActiveFolderTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }

    @Override
    protected Folder doInBackground(Void... params) {
        Folder result = null;
        String selection = PhotosShareColumns.START_DATE+" < "+
                Calendar.getInstance().getTimeInMillis()+" AND "+
                PhotosShareColumns.END_DATE+" > "+Calendar.getInstance().getTimeInMillis();

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

                Log.d(LOG_TAG, "[GetActiveFolderTask] found folder with id: "+
                        cursor.getString(colFolderIdIndex));
                result.setId(cursor.getString(colFolderIdIndex));
                result.setName(cursor.getString(colFolderNameIndex));
                result.setStartDate(cursor.getLong(colFolderStartDate));
                result.setEndDate(cursor.getLong(colFolderEndDate));
            }

            cursor.close();
        }

        return result;
    }

    @Override
    protected void onPostExecute(Folder folder) {
        if (folder != null && folder.getId() != null) mCaller.success(folder);
        else mCaller.failure(null, "no active folder found");
    }
}
