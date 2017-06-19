package photos.niyo.com.photosshare;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import photos.niyo.com.photosshare.tasks.DeleteFolderTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;

/**
 * Created by oriharel on 04/06/2017.
 */

public class FolderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    public static final String LOG_TAG = FolderViewHolder.class.getSimpleName();
    private ImageView mFolderImage;
    private TextView mFolderName;
    private TextView mFolderDescription;
    private Folder mFolder;
    private TextView mDeleteAction;

    public FolderViewHolder(View v) {
        super(v);

        mFolderImage = (ImageView) v.findViewById(R.id.folder_event_image);
        mFolderName = (TextView) v.findViewById(R.id.folder_event_title);
        mFolderDescription = (TextView) v.findViewById(R.id.folder_description);
        mDeleteAction = (TextView)v.findViewById(R.id.delete_folder);
        mDeleteAction.setOnClickListener(this);
    }

    //5
    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "CLICK!");
        if (v.getId() == R.id.delete_folder) {
            ServiceCaller caller = new ServiceCaller() {
                @Override
                public void success(Object data) {
                    //delete from db
                }

                @Override
                public void failure(Object data, String description) {

                }
            };
            new DeleteFolderTask(v.getContext(), caller).execute(mFolder);
        }
    }

    public void bindFolder(Folder folder) {
        Log.d(LOG_TAG, "bindFolder started");
        mFolder = folder;
        mFolderName.setText(folder.getName());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(folder.getStartDate());

        DateFormat sdf = DateFormat.getDateInstance();

        SimpleDateFormat.getDateInstance();

        mFolderDescription.setText("Start: "+sdf.format(cal.getTime()));
    }
}
