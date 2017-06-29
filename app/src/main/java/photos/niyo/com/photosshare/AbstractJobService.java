package photos.niyo.com.photosshare;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by oriharel on 29/06/2017.
 */

public abstract class AbstractJobService extends JobService{
    private Boolean isEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPreferences.getBoolean("master_switch", true);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (isEnabled()) {
            return doJob(params);
        }
        else {
            jobFinished(params, false);
            return false;
        }
    }

    protected abstract boolean doJob(JobParameters params);
}
