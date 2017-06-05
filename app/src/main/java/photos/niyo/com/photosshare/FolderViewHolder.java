package photos.niyo.com.photosshare;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by oriharel on 04/06/2017.
 */

public class FolderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    public static final String LOG_TAG = FolderViewHolder.class.getSimpleName();
    private ImageView mItemImage;
    private TextView mItemDate;
    private TextView mItemDescription;
    private Folder mFolder;

    public FolderViewHolder(View v) {
        super(v);

        mItemImage = (ImageView) v.findViewById(R.id.item_image);
        mItemDate = (TextView) v.findViewById(R.id.item_date);
        mItemDescription = (TextView) v.findViewById(R.id.item_description);
        v.setOnClickListener(this);
    }

    //5
    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "CLICK!");
    }

    public void bindFolder(Folder folder) {
        Log.d(LOG_TAG, "bindFolder started");
        mFolder = folder;
        mItemDate.setText(folder.getCreatedAt().toString());
        mItemDescription.setText(folder.getName());
    }
}
