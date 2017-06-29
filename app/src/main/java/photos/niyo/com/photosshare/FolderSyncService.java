package photos.niyo.com.photosshare;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

import photos.niyo.com.photosshare.tasks.DeleteFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetFoldersTask;
import photos.niyo.com.photosshare.tasks.InsertFoldersToDbTask;
import photos.niyo.com.photosshare.tasks.IsFoldersChangeTask;

/**
 * Created by oriharel on 19/06/2017.
 */

public class FolderSyncService extends AbstractJobService {
    public static final String LOG_TAG = FolderSyncService.class.getSimpleName();
    public static final String LAST_SYNC_KEY = "last_sync_folders";

    @Override
    public boolean doJob(final JobParameters params) {
        Log.d(LOG_TAG, "onStartJob started");

        ServiceCaller caller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from FoldersSyncUtil");
                jobFinished(params, false);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "received failure from FoldersSyncUtil");
                jobFinished(params, true);
            }
        };

        FoldersSyncUtil.updateFolders(getApplicationContext(), caller);

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_SYNC_KEY, Calendar.getInstance().getTimeInMillis());
        editor.apply();
        return true;
    }



    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
