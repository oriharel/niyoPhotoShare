package photos.niyo.com.photosshare.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.User;
import photos.niyo.com.photosshare.db.UsersColumns;

/**
 * Created by oriharel on 23/06/2017.
 */

public class InsertUsersToDbTask extends AsyncTask<User, Void, Boolean> {
    public static final String LOG_TAG = InsertUsersToDbTask.class.getSimpleName();
    private Context mContext;
    private ServiceCaller mCaller;

    public InsertUsersToDbTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }

    @Override
    protected Boolean doInBackground(User... params) {

        Log.d(LOG_TAG, "doInBackground started");
        for (User user :
                params) {
            ContentValues values = new ContentValues();

            values.put(UsersColumns.ID, user.getId());
            values.put(UsersColumns.EMAIL_ADDRESS, user.getEmailAddress());
            values.put(UsersColumns.DISPLAY_NAME, user.getDisplayName());
            values.put(UsersColumns.PHOTO_LINK, user.getPhotoLink());

            mContext.getContentResolver().insert(Constants.USERS_URI, values);
        }

        return null;
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
