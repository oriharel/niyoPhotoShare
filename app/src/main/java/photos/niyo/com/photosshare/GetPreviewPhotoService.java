package photos.niyo.com.photosshare;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;

import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;
import photos.niyo.com.photosshare.tasks.UpdateFolderInDbTask;

/**
 * Created by oriharel on 27/06/2017.
 */

public class GetPreviewPhotoService extends AbstractJobService {
    public static final String LOG_TAG = GetPreviewPhotoService.class.getSimpleName();
    @Override
    public boolean doJob(final JobParameters params) {

        Log.d(LOG_TAG, "onStartJob started");
        GetPreviewPhotoUtil.updatePhoto(getApplicationContext(), new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "returned success from GetPreviewPhotoUtil");
                jobFinished(params, false);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "returned failure from GetPreviewPhotoUtil");
                jobFinished(params, true);
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
