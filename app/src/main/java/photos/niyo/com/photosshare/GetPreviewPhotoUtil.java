package photos.niyo.com.photosshare;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
                DriveAPIsTask.DriveApiResult result = (DriveAPIsTask.DriveApiResult)data;
                String fileOwner = result.getMessage();
                SharedPreferences prefs = context.getSharedPreferences("app",
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Photo.PHOTO_OWNER_KEY, fileOwner);
                editor.apply();
                globalCaller.success(data);
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "Error downloading latest preview");
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
                        File fileToDownload = files.get(0);
                        Map<String, String> props = fileToDownload.getAppProperties();
                        String ownerEmail = null;
                        if (props != null) {
                            ownerEmail = props.get("owner");
                            Log.d(LOG_TAG, "found photo owner: "+ownerEmail);
                        }
                        else {
                            Log.d(LOG_TAG, "no owner was found");
                        }

                        DownloadFileTask downloadFileTask =
                                new DownloadFileTask(context,
                                        downloadFileCaller,
                                        fileToDownload.getId(),
                                        ownerEmail);
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
