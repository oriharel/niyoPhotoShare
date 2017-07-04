package photos.niyo.com.photosshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import photos.niyo.com.photosshare.db.PhotosShareColumns;
import photos.niyo.com.photosshare.tasks.DeleteFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.DeleteFolderTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;

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
    private TextView mEditAction;
    private TextView mEndDateView;
    private TextView mOwner;

    public FolderViewHolder(View v) {
        super(v);

        mFolderImage = (ImageView) v.findViewById(R.id.folder_event_image);
        mFolderName = (TextView) v.findViewById(R.id.folder_event_title);
        mFolderDescription = (TextView) v.findViewById(R.id.folder_description);
        mEndDateView = (TextView) v.findViewById(R.id.endDateView);
        mDeleteAction = (TextView)v.findViewById(R.id.delete_folder);
        mEditAction = (TextView)v.findViewById(R.id.edit_folder);
        mOwner = (TextView)v.findViewById(R.id.photo_owner);
        mDeleteAction.setOnClickListener(this);
        mEditAction.setOnClickListener(this);
        v.setOnClickListener(this);
    }

    //5
    @Override
    public void onClick(final View v) {
        Log.d(LOG_TAG, "CLICK!");
        if (v.getId() == R.id.delete_folder) {

            final ServiceCaller deleteCaller = new ServiceCaller() {
                @Override
                public void success(Object data) {
                    Log.d(LOG_TAG, "folder "+mFolder.getName()+" successfully deleted form db");
                    Snackbar.make(v, "Folder "+mFolder.getName()+" successfully deleted", Snackbar.LENGTH_LONG);
                }

                @Override
                public void failure(Object data, String description) {
                    Log.e(LOG_TAG, "error deleting "+mFolder.getName()+" form db");
                }
            };

            ServiceCaller caller = new ServiceCaller() {
                @Override
                public void success(Object data) {

                    Log.d(LOG_TAG, "FolderViewHolder received success from drive api delete");
                    Log.d(LOG_TAG, "deleting "+mFolder.getName()+" from db now");
                    DeleteFolderFromDbTask task = new DeleteFolderFromDbTask(v.getContext(),
                            deleteCaller);
                    task.execute(mFolder);
                }

                @Override
                public void failure(Object data, String description) {

                }
            };
            new DeleteFolderTask(v.getContext(), caller).execute(mFolder);
        }
        else if (v.getId() == R.id.edit_folder) {
            Intent intent = new Intent(v.getContext(), CreateEvent.class);
            intent.putExtra(PhotosShareColumns.FOLDER_ID, mFolder.getId());
            v.getContext().startActivity(intent);
        }
        else {
            String url = "https://drive.google.com/drive/u/0/folders/";
            url += mFolder.getId();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            v.getContext().startActivity(intent);
        }
    }

    public void bindFolder(final Folder folder) {
        Log.d(LOG_TAG, "bindFolder started");
        mFolder = folder;
        mFolderName.setText(folder.getName());
        mOwner.setVisibility(View.GONE);
        Calendar cal = Calendar.getInstance();
        Log.d(LOG_TAG, "setting start date: "+folder.getStartDate());
        cal.setTimeInMillis(folder.getStartDate());

        DateFormat sdf = DateFormat.getDateInstance();

        SimpleDateFormat.getDateInstance();

        mFolderDescription.setText("Start: "+sdf.format(cal.getTime()));
        Log.d(LOG_TAG, "setting end date: "+folder.getEndDate());
        cal.setTimeInMillis(folder.getEndDate());
        mEndDateView.setText("End: "+sdf.format(cal.getTime()));
        String filePath = mFolderImage.getContext().getFilesDir()+"/"+
                DownloadFileTask.LATEST_FILE_NAME;

        Log.d(LOG_TAG, "Trying to load file: ("+filePath+")");
        if (mFolder.isActive()) {
            try {
                FileInputStream fileIn = mFolderImage.getContext().
                        openFileInput(DownloadFileTask.LATEST_FILE_NAME);
                Bitmap previewImageBM = BitmapFactory.decodeStream(fileIn);
                if (previewImageBM != null) {
                    Log.d(LOG_TAG, "success loading bitmap file preview");
                    mFolderImage.setImageBitmap(previewImageBM);
                    SharedPreferences prefs = mOwner.getContext().getSharedPreferences("app", Context.MODE_PRIVATE);
                    String owner = prefs.getString(Photo.PHOTO_OWNER_KEY, "");
                    Log.d(LOG_TAG, "owner in prefs is: "+owner);
                    if (!TextUtils.isEmpty(owner)) {
                        mOwner.setText(owner);
                        mOwner.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    Log.d(LOG_TAG, "fail to load file preview bitmap ("+filePath+")");
                }
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "can't find file "+DownloadFileTask.LATEST_FILE_NAME);
            }
        }




    }
}
