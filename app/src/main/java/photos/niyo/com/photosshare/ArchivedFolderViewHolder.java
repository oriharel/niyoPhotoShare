package photos.niyo.com.photosshare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by oriharel on 09/07/2017.
 */

public class ArchivedFolderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    private ImageView mFolderImage;
    private TextView mFolderName;
    private Folder mFolder;
    public static final String LOG_TAG = ArchivedFolderViewHolder.class.getSimpleName();

    public ArchivedFolderViewHolder(View itemView) {
        super(itemView);

        mFolderImage = (ImageView) itemView.findViewById(R.id.archived_folder_event_image);
        mFolderName = (TextView) itemView.findViewById(R.id.archived_folder_event_title);
        itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String url = "https://drive.google.com/drive/u/0/folders/";
        url += mFolder.getId();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        v.getContext().startActivity(intent);
    }

    public void bindFolder(final Folder folder) {
        Log.d(LOG_TAG, "bindFolder started");
        mFolder = folder;
        mFolderName.setText(folder.getName());

//        try {
//            FileInputStream fileIn = mFolderImage.getContext().
//                    openFileInput(DownloadFileTask.LATEST_FILE_NAME);
//            Bitmap previewImageBM = BitmapFactory.decodeStream(fileIn);
//            if (previewImageBM != null) {
//                Log.d(LOG_TAG, "success loading bitmap file preview");
//                mFolderImage.setImageBitmap(previewImageBM);
//            }
//            else {
//                String filePath = mFolderImage.getContext().getFilesDir()+"/"+
//                        DownloadFileTask.LATEST_FILE_NAME;
//                Log.d(LOG_TAG, "fail to load file preview bitmap ("+filePath+")");
//            }
//        } catch (FileNotFoundException e) {
//            Log.d(LOG_TAG, "can't find file "+DownloadFileTask.LATEST_FILE_NAME);
//        }
    }
}
