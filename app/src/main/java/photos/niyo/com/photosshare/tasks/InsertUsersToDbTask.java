package photos.niyo.com.photosshare.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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

        Log.d(LOG_TAG, "doInBackground started with "+params.length+" users");
        for (User user :
                params) {
            Log.d(LOG_TAG, "inserting user "+user.getEmailAddress());
            ContentValues values = new ContentValues();

            values.put(UsersColumns.ID, user.getId());
            values.put(UsersColumns.EMAIL_ADDRESS, user.getEmailAddress());
            values.put(UsersColumns.DISPLAY_NAME, user.getDisplayName());
            values.put(UsersColumns.PHOTO_LINK, user.getPhotoLink());

            String selection = UsersColumns.EMAIL_ADDRESS+"='"+user.getEmailAddress()+"'";
            Log.d(LOG_TAG, "querying db selection: "+selection);
            Cursor cursor = mContext.getContentResolver().query(Constants.USERS_URI,
                    Constants.USERS_PROJECTION, selection, null, null);

            if (cursor != null) {
                if (!cursor.moveToFirst()) {
                    mContext.getContentResolver().insert(Constants.USERS_URI, values);
                }
                else {
                    Log.d(LOG_TAG, "user "+user.getEmailAddress()+" already exist");
                }
            }


        }

        return true;
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
