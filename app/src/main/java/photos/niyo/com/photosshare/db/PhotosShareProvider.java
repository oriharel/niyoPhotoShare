package photos.niyo.com.photosshare.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

import photos.niyo.com.photosshare.AndroidUtil;
import photos.niyo.com.photosshare.Constants;

public class PhotosShareProvider extends ContentProvider {
    public static final String LOG_TAG = PhotosShareDbHelper.class.getSimpleName();
    private PhotosShareDbHelper _dbHelper;
    private static final String DATABASE_NAME = "photosShare.db";
    private static final int DATABASE_VERSION = 1;
    private static final int FOLDERS_URI_CODE = 1;

    private static final UriMatcher sUriMatcher;
    private static HashMap<String, String> sFoldersProjectionMap;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Constants.AUTHORITY, Constants.FOLDERS_URI_STR, FOLDERS_URI_CODE);

        sFoldersProjectionMap = new HashMap<>();
        sFoldersProjectionMap.put(PhotosShareColumns._ID, PhotosShareColumns._ID);
        sFoldersProjectionMap.put(PhotosShareColumns.FOLDER_ID, PhotosShareColumns.FOLDER_ID);
        sFoldersProjectionMap.put(PhotosShareColumns.FOLDER_NAME, PhotosShareColumns.FOLDER_NAME);
        sFoldersProjectionMap.put(PhotosShareColumns.CREATE_AT, PhotosShareColumns.CREATE_AT);
        sFoldersProjectionMap.put(PhotosShareColumns.SHARED_WITH, PhotosShareColumns.SHARED_WITH);
        sFoldersProjectionMap.put(PhotosShareColumns.LOCATION, PhotosShareColumns.LOCATION);
        sFoldersProjectionMap.put(PhotosShareColumns.START_DATE, PhotosShareColumns.START_DATE);
        sFoldersProjectionMap.put(PhotosShareColumns.END_DATE, PhotosShareColumns.END_DATE);

    }
    public PhotosShareProvider() {
    }

    private PhotosShareDbHelper getDbHelper(){
        return _dbHelper;
    }

    private void setDbHelper(PhotosShareDbHelper helper) {
        _dbHelper = helper;
    }

    private SQLiteDatabase getWritableDb()
    {
        return getDbHelper().getWritableDatabase();
    }

    private SQLiteDatabase getReadableDb() {
        return getDbHelper().getReadableDatabase();
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(LOG_TAG, "delete is called");
        // Opens the database object in "write" mode.
        SQLiteDatabase db = getWritableDb();

        int count;

        count = db.delete(
                PhotosShareDbHelper.APP_TABLE_NAME,  // The database table name
                selection,                     // The incoming where clause column names
                selectionArgs                  // The incoming where clause values
        );
        return count;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        /**
         * Chooses the MIME type based on the incoming URI pattern
         */
        switch (sUriMatcher.match(uri)) {

            // If the pattern is for home state, returns the general content type.
            case FOLDERS_URI_CODE:
                return Constants.CONTENT_TYPE;

            // If the URI pattern doesn't match any permitted patterns, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(LOG_TAG, "insert started "+uri);
        String table = PhotosShareDbHelper.APP_TABLE_NAME;
        getWritableDb().insert(table, null, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }

    @Override
    public boolean onCreate() {
        Log.d(LOG_TAG, "onCreate started");
        Context context = getContext();

        setDbHelper(new PhotosShareDbHelper(context, DATABASE_NAME, null, DATABASE_VERSION));
        return getWritableDb() != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(LOG_TAG, "query started with "+uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(PhotosShareDbHelper.APP_TABLE_NAME);

        qb.setProjectionMap(sFoldersProjectionMap);

        Log.d(LOG_TAG, "going to query with selection "+selection);
        Log.d(LOG_TAG, "projection is "+ AndroidUtil.getArrayAsString(projection));
        Log.d(LOG_TAG, "selectionArgs is "+AndroidUtil.getArrayAsString(selectionArgs));
        Log.d(LOG_TAG, "sort order is "+sortOrder);
        String orderBy = PhotosShareColumns.CREATE_AT;

        Cursor cursor = qb.query(getReadableDb(), projection, selection, selectionArgs, null, null, orderBy);

        Log.d(LOG_TAG, "got " + cursor.getCount()
                + " results from uri " + uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // Opens the database object in "write" mode.
        SQLiteDatabase db = getWritableDb();
        int count;

        // Does the update based on the incoming URI pattern
        switch (sUriMatcher.match(uri)) {

            // If the incoming URI matches the general notes pattern, does the update based on
            // the incoming data.
            case FOLDERS_URI_CODE:

                // Does the update and returns the number of rows updated.
                count = db.update(
                        PhotosShareDbHelper.APP_TABLE_NAME, // The database table name.
                        values,                   // A map of column names and new values to use.
                        selection,                    // The where clause column names.
                        selectionArgs                 // The where clause column values to select on.
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /*Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows updated.
        return count;
    }
}
