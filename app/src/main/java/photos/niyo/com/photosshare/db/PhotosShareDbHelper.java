package photos.niyo.com.photosshare.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by oriharel on 04/06/2017.
 */

public class PhotosShareDbHelper extends SQLiteOpenHelper {

    public static final String APP_TABLE_NAME = "photosShareTable";
    public static final String LOG_TAG = PhotosShareDbHelper.class.getSimpleName();

    private static final String TABLE_APP_CREATE =
            "create table " + APP_TABLE_NAME + " ("
                    + PhotosShareColumns._ID + " integer primary key autoincrement, "
                    + PhotosShareColumns.FOLDER_ID + " TEXT, "
                    + PhotosShareColumns.FOLDER_NAME + " TEXT, "
                    + PhotosShareColumns.CREATE_AT + " BIGINT, "
                    + PhotosShareColumns.SHARED_WITH + " ARRAY, "
                    + PhotosShareColumns.LOCATION + " Decimal(9,6));";

    public PhotosShareDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        Log.d(LOG_TAG, "constructor called");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(LOG_TAG, "onCreate started "+TABLE_APP_CREATE);
        sqLiteDatabase.execSQL(TABLE_APP_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
