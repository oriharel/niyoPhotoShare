package photos.niyo.com.photosshare;

import android.net.Uri;

import photos.niyo.com.photosshare.db.PhotosShareColumns;
import photos.niyo.com.photosshare.db.UsersColumns;

/**
 * Created by oriharel on 04/06/2017.
 */

public class Constants {
    public static final String AUTHORITY = "com.niyo.photos.provider";
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    public static String SCHEME = "content://";
    public static final String FOLDERS_URI_STR = "/folders";
    public static final String USERS_URI_STR = "/users";
    public static final Uri FOLDERS_URI = Uri.parse(SCHEME + AUTHORITY + FOLDERS_URI_STR);
    public static final Uri USERS_URI = Uri.parse(SCHEME + AUTHORITY + USERS_URI_STR);
    public static final String FOLDERS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.niyo.photos.folders";
    public static final String USERS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.niyo.photos.users";

    public static final String[] FOLDERS_PROJECTION = new String[] {
            PhotosShareColumns._ID,
            PhotosShareColumns.FOLDER_ID,
            PhotosShareColumns.FOLDER_NAME,
            PhotosShareColumns.CREATE_AT,
            PhotosShareColumns.SHARED_WITH,
            PhotosShareColumns.LOCATION,
            PhotosShareColumns.START_DATE,
            PhotosShareColumns.END_DATE
    };

    public static final String[] USERS_PROJECTION = new String[] {
            UsersColumns._ID,
            UsersColumns.ID,
            UsersColumns.EMAIL_ADDRESS,
            UsersColumns.DISPLAY_NAME,
            UsersColumns.PHOTO_LINK
    };
}
