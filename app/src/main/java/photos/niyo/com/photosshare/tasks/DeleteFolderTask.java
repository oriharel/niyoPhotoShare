package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;

/**
 * Created by oriharel on 18/06/2017.
 */

public class DeleteFolderTask extends DriveAPIsTask {
    public static final String LOG_TAG = DeleteFolderTask.class.getSimpleName();
    public DeleteFolderTask(Context context, ServiceCaller caller) {
        super(context, caller);
    }

    @Override
    protected DriveApiResult actualDoInBackground(Folder... params) {
        Folder folder = params[0];
        Log.d(LOG_TAG, "actualDoInBackground started with "+folder.getName());
        DriveApiResult result = new DriveApiResult();
        try {
            mService.files().delete(folder.getId()).execute();
            Log.d(LOG_TAG, "folder "+folder.getName()+" deleted");
            result.setResult(true);
        } catch (IOException e) {
            String message = "An error occurred: " + e;
            Log.e(LOG_TAG, message);
            result.setResult(false);
            result.setMessage(message);
            result.setException(e);
        }

        return result;
    }
}
