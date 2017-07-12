package photos.niyo.com.photosshare;

import android.database.Cursor;
import android.util.Log;

import photos.niyo.com.photosshare.db.PhotosShareColumns;

/**
 * Created by oriharel on 04/06/2017.
 */

public class Folder {
    public static final String LOG_TAG = Folder.class.getSimpleName();
    public static final String APP_ID = "app_id";
    private boolean isActive;

    public String getName() {
        return mName;
    }

    public Long getCreatedAt() {
        return mCreatedAt;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setCreatedAt(Long creationDate) {
        mCreatedAt = creationDate;
    }

    private String mName;
    private Long mCreatedAt;
    private String mId;
    private Long mStartDate;
    private Long mEndDate;
    private String mSharedWith;
    private String mIsRsvp;
    private String mOwners;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public Long getStartDate() {
        return mStartDate;
    }

    public void setStartDate(Long startDate) {
        this.mStartDate = startDate;
    }

    public Long getEndDate() {
        return mEndDate;
    }

    public void setEndDate(Long endDate) {
        this.mEndDate = endDate;
    }

    public String getSharedWith() {
        return mSharedWith;
    }

    public void setSharedWith(String mSharedWith) {
        Log.d(LOG_TAG, "setSharedWith for folder: "+mName+" ["+mSharedWith+"]");
        this.mSharedWith = mSharedWith;
    }

    public String isRsvp() {
        return mIsRsvp;
    }

    public void setIsRsvp(String isRsvp) {
        mIsRsvp = isRsvp;
    }

    @Override
    public String toString() {
        return "Id: "+mId+" Name: "+mName+" Start Date: "+mStartDate+" End Date: "+mEndDate+" Shared With: "+mSharedWith;
    }

    @Override
    public boolean equals(Object folder) {
        if (!(folder instanceof Folder)) {
            return false;
        }

        Folder otherFolder = (Folder) folder;

        return otherFolder.getId().equals(mId) &&
                otherFolder.getSharedWith().equals(mSharedWith) &&
                otherFolder.getStartDate().equals(mStartDate) &&
                otherFolder.getEndDate().equals(mEndDate);

    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public static Folder createFolderFromCursor(Cursor cursor, Boolean isActive) {

        int colFolderNameIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_NAME);
        String foldeName = cursor.getString(colFolderNameIndex);
        int colFolderIdIndex = cursor.getColumnIndex(PhotosShareColumns.FOLDER_ID);
        String folderId = cursor.getString(colFolderIdIndex);
        int colCreatedAtIndex = cursor.getColumnIndex(PhotosShareColumns.CREATE_AT);
        int colStartDateIndex = cursor.getColumnIndex(PhotosShareColumns.START_DATE);
        int colEndDateIndex = cursor.getColumnIndex(PhotosShareColumns.END_DATE);
        int colSharedWith = cursor.getColumnIndex(PhotosShareColumns.SHARED_WITH);
        int colOwner = cursor.getColumnIndex(PhotosShareColumns.OWNERS);
        String createdAt = cursor.getString(colCreatedAtIndex);
        Folder folder = new Folder();
        folder.setName(foldeName);
        folder.setCreatedAt(Long.valueOf(createdAt));
        folder.setStartDate(cursor.getLong(colStartDateIndex));
        folder.setEndDate(cursor.getLong(colEndDateIndex));
        folder.setIsActive(isActive);
        folder.setId(folderId);
        folder.setSharedWith(cursor.getString(colSharedWith));
        folder.setOwners(cursor.getString(colOwner));

        return folder;
    }

    public String getOwners() {
        return mOwners;
    }

    public void setOwners(String owners) {
        this.mOwners = owners;
    }
}
