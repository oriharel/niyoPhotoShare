package photos.niyo.com.photosshare;

import android.content.Context;
import android.util.Log;

import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetFoldersTask;
import photos.niyo.com.photosshare.tasks.InsertFoldersToDbTask;
import photos.niyo.com.photosshare.tasks.IsFoldersChangeTask;

/**
 * Created by oriharel on 28/06/2017.
 */

public class FoldersSyncUtil {
    public static final String LOG_TAG = FoldersSyncUtil.class.getSimpleName();

    public static void updateFolders(final Context context, final ServiceCaller globalCaller) {
        final ServiceCaller dbCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from InsertNewFoldersTask");
                globalCaller.success(data);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from InsertNewFoldersTask "+description);
                globalCaller.failure(data, description);
            }
        };

        final ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {

                final DriveAPIsTask.DriveApiResult result = (DriveAPIsTask.DriveApiResult)data;

                Log.d(LOG_TAG, "received success from GetFoldersTask with "
                        +result.getFolders().length+" shared folders");

                ServiceCaller needToUpdateCaller = new ServiceCaller() {
                    @Override
                    public void success(Object data) {
                        Boolean isNeedToUpdate = (Boolean)data;
                        if (isNeedToUpdate) {
                            context.getContentResolver().delete(Constants.FOLDERS_URI, null, null);
                            //TODO should be on a different thread
                            context.getContentResolver()
                                    .delete(Constants.FOLDERS_URI, null, null);

                            if (result.getFolders().length > 0) {
                                InsertFoldersToDbTask task =
                                        new InsertFoldersToDbTask(context,
                                        dbCaller);
                                task.execute(result.getFolders());
                            }
                            else {
//                                jobFinished(params, false);
                                globalCaller.success(data);
                            }

                        }
                        else {
                            Log.d(LOG_TAG, "all is synced");
                        }
                    }

                    @Override
                    public void failure(Object data, String description) {
                        Log.e(LOG_TAG, "Error from need to update query");
                        globalCaller.failure(data, description);
                    }
                };

                checkIfNeedToUpdate(result.getFolders(), needToUpdateCaller, context);

            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from GetFoldersTask with: "+description);
                globalCaller.failure(data, description);
            }
        };


        GetFoldersTask task = new GetFoldersTask(context, caller);
        task.execute();
    }

    private static void checkIfNeedToUpdate(Folder[] folders, ServiceCaller caller, Context context) {
        IsFoldersChangeTask task = new IsFoldersChangeTask(context, caller);
        task.execute(folders);
    }
}
