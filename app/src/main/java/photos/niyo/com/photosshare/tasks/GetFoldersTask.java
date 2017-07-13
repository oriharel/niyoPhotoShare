package photos.niyo.com.photosshare.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.util.StringUtils;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import photos.niyo.com.photosshare.Folder;
import photos.niyo.com.photosshare.ServiceCaller;
import photos.niyo.com.photosshare.db.User;

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
                .setFields("nextPageToken, files(id, kind, parents, name, owners, appProperties, permissions)")
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
            Log.d(LOG_TAG, String.format("folder %s has end_date of %s",
                    file.getName(), endDateStr));
            folder.setEndDate(Long.valueOf(endDateStr));
            String createAtStr = file.getAppProperties().get("created_at");
            folder.setCreatedAt(Long.valueOf(createAtStr));
            folder.setSharedWith(getWriters(file));
            folder.setOwners(getOwners(file));
            folders[i] = folder;
        }
        result.setFolders(folders);
        result.setResult(true);
        return result;
    }

    private String getOwners(File file) {
        List<com.google.api.services.drive.model.User> owners = file.getOwners();
        List<String> ownerEmails = new ArrayList<>();
        for (com.google.api.services.drive.model.User user :
                owners) {
            ownerEmails.add(user.getEmailAddress());
        }

        return TextUtils.join(",", ownerEmails);
    }

    private String getWriters(File file) {
        Log.d(LOG_TAG, "getWriters started");
        List<Permission> permissions = file.getPermissions();
        List<String> result = new ArrayList<>();
        List<User> writers = new ArrayList<>();

        Log.d(LOG_TAG, "received "+permissions.size()+" permissions");

        for (Permission permission :
                permissions) {
            String emailAddress = permission.getEmailAddress();
            Log.d(LOG_TAG, "permission is for "+emailAddress);
            String displayName = permission.getDisplayName();
            String photoLink = permission.getPhotoLink();
            String userId = permission.getId();
            result.add(emailAddress);
            User user = new User();
            user.setId(userId);
            user.setEmailAddress(emailAddress);
            user.setDisplayName(displayName);
            user.setPhotoLink(photoLink);
            writers.add(user);
        }

        ServiceCaller insertUsersCaller = new ServiceCaller() {
            @Override
            public void success(Object data) {
                Log.d(LOG_TAG, "writers were updated to db");
            }

            @Override
            public void failure(Object data, String description) {
                Log.e(LOG_TAG, "error in update db with writers");
            }
        };

        InsertUsersToDbTask task = new InsertUsersToDbTask(mContext, insertUsersCaller);
        task.execute(writers.toArray(new User[writers.size()]));
        Log.d(LOG_TAG, "writers result is "+result);
        return TextUtils.join(",", GetUsersFromDbTask.padEmailAddress(result));
    }
}
