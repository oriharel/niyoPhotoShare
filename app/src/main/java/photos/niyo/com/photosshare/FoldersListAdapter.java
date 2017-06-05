package photos.niyo.com.photosshare;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by oriharel on 04/06/2017.
 */

public class FoldersListAdapter extends RecyclerView.Adapter {
    private List<Folder> mFolders;

    public FoldersListAdapter(List<Folder> folders) {
        mFolders = folders;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Folder itemFolder = mFolders.get(position);
        ((FolderViewHolder)holder).bindFolder(itemFolder);
    }

    @Override
    public int getItemCount() {
        return mFolders.size();
    }
}
