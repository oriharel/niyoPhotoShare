package photos.niyo.com.photosshare;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by oriharel on 04/06/2017.
 */

public class Folder {
    public static final String APP_ID = "app_id";

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
        this.mSharedWith = mSharedWith;
    }
}
