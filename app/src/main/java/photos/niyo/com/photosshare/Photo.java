package photos.niyo.com.photosshare;

/**
 * Created by oriharel on 30/06/2017.
 */

public class Photo {
    private String mName;
    private String mOwner;
    private Long mDateTaken;
    public static final String PHOTO_OWNER_KEY = "photo_owner";

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String mOwner) {
        this.mOwner = mOwner;
    }

    public Long getDateTaken() {
        return mDateTaken;
    }

    public void setDateTaken(Long mDateTaken) {
        this.mDateTaken = mDateTaken;
    }
}
