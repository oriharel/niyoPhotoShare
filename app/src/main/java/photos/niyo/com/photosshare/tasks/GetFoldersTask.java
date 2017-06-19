package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.util.Log;

import com.google.api.client.util.StringUtils;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;

import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;

/**
 * Created by oriharel on 19/06/2017.
 */

public class GetFoldersTask extends DriveAPIsTask {
    public static final String LOG_TAG = GetFoldersTask.class.getSimpleName();
    public GetFoldersTask(Context context, ServiceCaller caller) {
        super(context, caller);
    }

    @Override
    protected DriveApiResult actualDoInBackground(Folder... params) throws IOException {
        DriveApiResult result = new DriveApiResult();
        String queryString = "appProperties has {key='"+Folder.APP_ID+"' and value='"+mContext.getPackageName()+"'}";
        Log.d(LOG_TAG, "actualDoInBackground started with q="+queryString);
        FileList fileList = mService.files().list()
                .setPageSize(30)
                .setFields("nextPageToken, files(id, kind, parents, name, appProperties)")
                .setQ(queryString)
                .execute();
        List<File> files = fileList.getFiles();
        Log.d(LOG_TAG, "after mService.execute. result: "+files.size());
        Folder[] folders = new Folder[files.size()];
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            Log.d(LOG_TAG, "found: "+String.format("%s %s %s (%s)\n",
                    file.getName(), file.getKind(), file.getParents(), file.getId()));
            Folder folder = new Folder();
            folder.setId(file.getId());
            folder.setName(file.getName());
            String startDateStr = file.getAppProperties().get("start_date");
            Log.d(LOG_TAG, String.format("folder %s has start_date of %s",
                    file.getName(), startDateStr));
            folder.setStartDate(Long.valueOf(startDateStr));
            String endDateStr = file.getAppProperties().get("end_date");
            Log.d(LOG_TAG, String.format("folder %s has start_date of %s",
                    file.getName(), endDateStr));
            String createAtStr = file.getAppProperties().get("created_at");
            folder.setCreatedAt(Long.valueOf(createAtStr));
            folders[i] = folder;
        }
        result.setFolders(folders);
        result.setResult(true);
        return result;
    }
}
