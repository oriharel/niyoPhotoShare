package photos.niyo.com.photosshare.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by oriharel on 04/06/2017.
 */

public class PhotosShareDbHelper extends SQLiteOpenHelper {

    public static final String FOLDERS_TABLE_NAME = "photosShareTable";
    public static final String USERS_TABLE_NAME = "users";
    public static final String LOG_TAG = PhotosShareDbHelper.class.getSimpleName();

    private static final String FOLDERS_TABLE_CREATE =
            "create table " + FOLDERS_TABLE_NAME + " ("
                    + PhotosShareColumns._ID + " integer primary key autoincrement, "
                    + PhotosShareColumns.FOLDER_ID + " TEXT, "
                    + PhotosShareColumns.FOLDER_NAME + " TEXT, "
                    + PhotosShareColumns.CREATE_AT + " BIGINT, "
                    + PhotosShareColumns.START_DATE + " BIGINT, "
                    + PhotosShareColumns.END_DATE + " BIGINT, "
                    + PhotosShareColumns.SHARED_WITH + " TEXT, "
                    + PhotosShareColumns.LOCATION + " Decimal(9,6));";

    private static final String USERS_TABLE_CREATE =
            "create table " + FOLDERS_TABLE_NAME + " ("
                    + UsersColumns._ID + " integer primary key autoincrement, "
                    + UsersColumns.ID + " TEXT, "
                    + UsersColumns.EMAIL_ADDRESS + " TEXT, "
                    + UsersColumns.DISPLAY_NAME + " TEXT, "
                    + UsersColumns.PHOTO_LINK + " TEXT);";

    public PhotosShareDbHelper(Context context,
                               String name,
                               SQLiteDatabase.CursorFactory factory,
                               int version) {
        super(context, name, factory, version);
        Log.d(LOG_TAG, "constructor called");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(LOG_TAG, "onCreate started FOLDERS_TABLE_CREATE "+ FOLDERS_TABLE_CREATE);
        Log.d(LOG_TAG, "onCreate started USERS_TABLE_CREATE "+ USERS_TABLE_CREATE);
        sqLiteDatabase.execSQL(FOLDERS_TABLE_CREATE);
        sqLiteDatabase.execSQL(USERS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
