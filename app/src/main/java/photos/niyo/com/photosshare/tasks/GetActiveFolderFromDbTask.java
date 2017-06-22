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

public class GetActiveFolderFromDbTask extends GetFolderFromDbTask {
    public static final String LOG_TAG = GetActiveFolderFromDbTask.class.getSimpleName();

    public GetActiveFolderFromDbTask(Context context, ServiceCaller caller) {
        super(context, caller);
    }

    protected String getSelection(String... params) {
        String selection = PhotosShareColumns.START_DATE+" < "+
                Calendar.getInstance().getTimeInMillis()+" AND "+
                PhotosShareColumns.END_DATE+" > "+Calendar.getInstance().getTimeInMillis();
        return selection;
    }
}
