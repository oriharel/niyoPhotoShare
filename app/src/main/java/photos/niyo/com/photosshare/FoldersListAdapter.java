package photos.niyo.com.photosshare;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by oriharel on 04/06/2017.
 */

public class FoldersListAdapter extends RecyclerView.Adapter {
    public static final String LOG_TAG = FoldersListAdapter.class.getSimpleName();
    private List<Folder> mFolders;

    public FoldersListAdapter(List<Folder> folders) {
        Log.d(LOG_TAG, "FoldersListAdapter constructor called");
        mFolders = folders;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(LOG_TAG, "onCreateViewHolder started");
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.d(LOG_TAG, "onBindViewHolder started");
        Folder itemFolder = mFolders.get(position);
        ((FolderViewHolder)holder).bindFolder(itemFolder);
    }

    @Override
    public int getItemCount() {
        return mFolders.size();
    }
}
