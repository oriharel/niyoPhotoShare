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

public class GetPreviewPhotoService extends JobService {
    public static final String LOG_TAG = GetPreviewPhotoService.class.getSimpleName();
    @Override
    public boolean onStartJob(final JobParameters params) {

        Log.d(LOG_TAG, "onStartJob started");

        final ServiceCaller downloadFileCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "latest active preview downloaded");
                getApplicationContext().getContentResolver().notifyChange(Constants.FOLDERS_URI, null);
                jobFinished(params, false);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error downloading latest preview");
                jobFinished(params, false);
            }
        };

        final ServiceCaller getFilesListCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "received success from GetFilesListTask");
                DriveAPIsTask.DriveApiResult result = (DriveAPIsTask.DriveApiResult)data;
                FileList fileList = result.getFileList();
                List<File> files = fileList.getFiles();

                if (files != null) {
                    Log.d(LOG_TAG, "GetFilesListTask returned "+files.size()+" files");
                    if (files.size() > 0) {
                        DownloadFileTask downloadFileTask =
                                new DownloadFileTask(getApplicationContext(),
                                downloadFileCaller, files.get(0).getId());
                        downloadFileTask.execute();
                    }
                }
                else {
                    Log.e(LOG_TAG, "GetFilesListTask returned null in files list");
                }

            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error getting files list");
                jobFinished(params, false);
            }
        };

        ServiceCaller activeFolderCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "success from GetActiveFolderFromDbTask");
                Folder activeFolder = (Folder)data;
                GetFilesListTask filesListTask = new GetFilesListTask(getApplicationContext(),
                        getFilesListCaller);
                filesListTask.execute(activeFolder);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error getting active folder from db");
                jobFinished(params, false);
            }
        };

        GetActiveFolderFromDbTask activeFolderTask =
                new GetActiveFolderFromDbTask(getApplicationContext(), activeFolderCaller);
        activeFolderTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
