package photos.niyo.com.photosshare;

import java.util.Date;

/**
 * Created by oriharel on 04/06/2017.
 */

public class Folder {
    public String getName() {
        return _name;
    }

    public Long getCreatedAt() {
        return _createdAt;
    }

    public void setName(String name) {
        _name = name;
    }

    public void setCreatedAt(Long creationDate) {
        _createdAt = creationDate;
    }

    private String _name;
    private Long _createdAt;
}
