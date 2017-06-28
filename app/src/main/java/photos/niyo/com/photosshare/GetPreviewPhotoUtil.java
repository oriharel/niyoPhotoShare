package photos.niyo.com.photosshare;

import android.content.Context;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;

import photos.niyo.com.photosshare.tasks.DriveAPIsTask;
import photos.niyo.com.photosshare.tasks.GetActiveFolderFromDbTask;

/**
 * Created by oriharel on 28/06/2017.
 */

public class GetPreviewPhotoUtil {
    public static final String LOG_TAG = GetPreviewPhotoUtil.class.getSimpleName();

    public static void updatePhoto(final Context context, final ServiceCaller globalCaller) {
        final ServiceCaller downloadFileCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "latest active preview downloaded");
                context.getContentResolver().notifyChange(Constants.FOLDERS_URI, null);
//                jobFinished(params, false);
                globalCaller.success(data);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error downloading latest preview");
//                jobFinished(params, false);
                globalCaller.failure(data, description);
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
                                new DownloadFileTask(context,
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
//                jobFinished(params, false);
                globalCaller.failure(data, description);
            }
        };

        ServiceCaller activeFolderCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "success from GetActiveFolderFromDbTask");
                Folder activeFolder = (Folder)data;
                GetFilesListTask filesListTask = new GetFilesListTask(context,
                        getFilesListCaller);
                filesListTask.execute(activeFolder);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error getting active folder from db");
//                jobFinished(params, false);
                globalCaller.failure(data, description);
            }
        };

        GetActiveFolderFromDbTask activeFolderTask =
                new GetActiveFolderFromDbTask(context, activeFolderCaller);
        activeFolderTask.execute();
    }
}
