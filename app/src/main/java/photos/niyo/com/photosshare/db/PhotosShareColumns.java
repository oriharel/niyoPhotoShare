package photos.niyo.com.photosshare.db;

import android.provider.BaseColumns;

/**
 * Created by oriharel on 04/06/2017.
 */

public class PhotosShareColumns implements BaseColumns {
    public static final String FOLDER_ID = "folder_id";
    public static final String FOLDER_NAME = "folder_name";
    public static final String CREATE_AT = "created_at";
    public static final String SHARED_WITH = "shared_with";
    public static final String LOCATION = "location";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    public static final String IS_RSVP = "is_rsvp";
    public static final String OWNERS = "owners";
}
