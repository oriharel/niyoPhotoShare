package photos.niyo.com.photosshare.db;

/**
 * Created by oriharel on 23/06/2017.
 */

public class User {
    private String mId;
    private String mEmailAddress;
    private String mDisplayName;
    private String mPhotoLink;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    public void setEmailAddress(String mEmailAddress) {
        this.mEmailAddress = mEmailAddress;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    public String getPhotoLink() {
        return mPhotoLink;
    }

    public void setPhotoLink(String mPhotoLink) {
        this.mPhotoLink = mPhotoLink;
    }
}
