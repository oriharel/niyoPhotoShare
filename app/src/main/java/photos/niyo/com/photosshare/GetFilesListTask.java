package photos.niyo.com.photosshare;

import android.content.Context;
import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;

import photos.niyo.com.photosshare.tasks.DriveAPIsTask;

/**
 * Created by oriharel on 27/06/2017.
 */

public class GetFilesListTask extends DriveAPIsTask {
    public static final String LOG_TAG = GetFilesListTask.class.getSimpleName();
    public GetFilesListTask(Context context, ServiceCaller caller) {
        super(context, caller);
    }

    @Override
    protected DriveApiResult actualDoInBackground(Folder... params) throws IOException {
        Folder parentFolder = params[0];
        Log.d(LOG_TAG, "actualDoInBackground started with folder: ("+parentFolder.getName()+")");
        DriveApiResult result = new DriveApiResult();
        FileList fileList = mService.files().list()
                .setQ("'"+parentFolder.getId()+"' in parents")
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, appProperties)")
                .execute();
        result.setFileList(fileList);
        result.setResult(true);
        return result;
    }
}
