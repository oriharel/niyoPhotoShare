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
    private String _id;
    private Long _startDate;
    private Long _endDate;

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public Long getStartDate() {
        return _startDate;
    }

    public void setStartDate(Long startDate) {
        this._startDate = startDate;
    }

    public Long getEndDate() {
        return _endDate;
    }

    public void setEndDate(Long endDate) {
        this._endDate = endDate;
    }
}
