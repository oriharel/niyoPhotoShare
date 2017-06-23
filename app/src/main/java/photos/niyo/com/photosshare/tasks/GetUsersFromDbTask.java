package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import photos.niyo.com.photosshare.Constants;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.User;
import photos.niyo.com.photosshare.db.UsersColumns;

/**
 * Created by oriharel on 23/06/2017.
 */

public class GetUsersFromDbTask extends AsyncTask<String, Void, User[]> {
    public static final String LOG_TAG = GetUsersFromDbTask.class.getSimpleName();
    private Context mContext;
    private ServiceCaller mCaller;

    public GetUsersFromDbTask(Context context, ServiceCaller caller) {
        mContext = context;
        mCaller = caller;
    }
    @Override
    protected User[] doInBackground(String... params) {
        String userEmailsStr = TextUtils.join(",", padEmailAddress(params));
        String selection = UsersColumns.EMAIL_ADDRESS+" in ("+userEmailsStr+")";
        Log.d(LOG_TAG, "doInBackground started with selector: "+selection);
        Cursor cursor = mContext.getContentResolver().query(Constants.USERS_URI,
                Constants.USERS_PROJECTION, selection, null, UsersColumns.DISPLAY_NAME);
        List<User> result = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                while (!cursor.isAfterLast()) {
                    User user = new User();
                    int colUserIdIndex = cursor.getColumnIndex(UsersColumns.ID);
                    int colEmailIndex = cursor.getColumnIndex(UsersColumns.EMAIL_ADDRESS);
                    int colDisplayNameIndex = cursor.getColumnIndex(UsersColumns.DISPLAY_NAME);
                    int colPhotoLinkIndex = cursor.getColumnIndex(UsersColumns.PHOTO_LINK);

                    Log.d(LOG_TAG, "[GetUsersFromDbTask] found user with id: "+
                            cursor.getString(colUserIdIndex));
                    user.setId(cursor.getString(colUserIdIndex));
                    user.setEmailAddress(cursor.getString(colEmailIndex));
                    user.setDisplayName(cursor.getString(colDisplayNameIndex));
                    user.setPhotoLink(cursor.getString(colPhotoLinkIndex));
                    result.add(user);
                    cursor.moveToNext();
                }

            }

            cursor.close();
        }
        return result.toArray(new User[result.size()]);
    }

    private String[] padEmailAddress(String[] params) {
        String[] result = new String[params.length];
        for (int i = 0; i < params.length; i++){
            result[i] = "'"+params[i]+"'";
        }

        return result;
    }

    @Override
    protected void onPostExecute(User[] users) {
        if (users != null) {
            mCaller.success(users);
        }
        else {
            mCaller.failure(null, "something is wrong fetching users");
        }
    }
}
