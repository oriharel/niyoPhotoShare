package photos.niyo.com.photosshare;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import photos.niyo.com.photosshare.db.InsertNewFoldersTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetFoldersTask;

/**
 * Created by oriharel on 19/06/2017.
 */

public class FolderSyncService extends JobService {
    public static final String LOG_TAG = FolderSyncService.class.getSimpleName();
    @Override
    public boolean onStartJob(JobParameters params) {

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

                DriveAPIsTask.DriveApiResult result = (DriveAPIsTask.DriveApiResult)data;

                Log.d(LOG_TAG, "received success from GetFoldersTask with "
                        +result.getFolders().length+" shared folders");

                //TODO should be on a different thread
                getApplicationContext().getContentResolver().delete(Constants.FOLDERS_URI, null, null);

                if (result.getFolders().length > 0) {
                    InsertNewFoldersTask task = new InsertNewFoldersTask(getApplicationContext(),
                            dbCaller);
                    task.execute(result.getFolders());
                }
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

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
