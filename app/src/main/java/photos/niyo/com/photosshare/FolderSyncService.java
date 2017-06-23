package photos.niyo.com.photosshare;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import photos.niyo.com.photosshare.tasks.DeleteFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetFoldersTask;
import photos.niyo.com.photosshare.tasks.InsertFoldersToDbTask;
import photos.niyo.com.photosshare.tasks.IsFoldersChangeTask;

/**
 * Created by oriharel on 19/06/2017.
 */

public class FolderSyncService extends JobService {
    public static final String LOG_TAG = FolderSyncService.class.getSimpleName();
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(LOG_TAG, "onStartJob started");

        final ServiceCaller dbCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from InsertNewFoldersTask");
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from InsertNewFoldersTask "+description);
            }
        };

        ServiceCaller caller = new ServiceCaller() {
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
                            //TODO should be on a different thread
                            getApplicationContext().getContentResolver()
                                    .delete(Constants.FOLDERS_URI, null, null);

                            if (result.getFolders().length > 0) {
                                InsertFoldersToDbTask task = new InsertFoldersToDbTask(getApplicationContext(),
                                        dbCaller);
                                task.execute(result.getFolders());
                            }
                        }
                        else {
                            Log.d(LOG_TAG, "all is synced");
                        }
                    }

                    @Override
                    public void failure(Object data, String description) {
                        Log.e(LOG_TAG, "Error from need to update query");
                    }
                };

                checkIfNeedToUpdate(result.getFolders(), needToUpdateCaller);

            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received error from GetFoldersTask with: "+description);
            }
        };


        GetFoldersTask task = new GetFoldersTask(getApplicationContext(), caller);
        task.execute();
        return false;
    }

    private void checkIfNeedToUpdate(Folder[] folders, ServiceCaller caller) {
        IsFoldersChangeTask task = new IsFoldersChangeTask(getApplicationContext(), caller);
        task.execute(folders);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
