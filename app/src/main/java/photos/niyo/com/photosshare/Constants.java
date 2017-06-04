package photos.niyo.com.photosshare;

import android.net.Uri;

import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 04/06/2017.
 */

public class Constants {
    public static final String AUTHORITY = "com.niyo.photos.provider";
    public static String SCHEME = "content://";
    public static final String FOLDERS_URI_STR = "/folders";
    public static final Uri FOLDERS_URI = Uri.parse(SCHEME + AUTHORITY + FOLDERS_URI_STR);
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.niyo.photos.folders";

    public static final String[] FOLDERS_PROJECTION = new String[] {
            PhotosShareColumns._ID,
            PhotosShareColumns.FOLDER_ID,
            PhotosShareColumns.FOLDER_NAME,
            PhotosShareColumns.CREATE_AT,
            PhotosShareColumns.SHARED_WITH,
            PhotosShareColumns.LOCATION
    };
}
