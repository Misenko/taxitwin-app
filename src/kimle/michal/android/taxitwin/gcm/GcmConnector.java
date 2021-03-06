package kimle.michal.android.taxitwin.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import kimle.michal.android.taxitwin.R;

public class GcmConnector {

    private final Context context;
    private GoogleCloudMessaging gcm;
    private static final int TTL = 180;
    private static final String SENDER_ID = "275458664476";
    private static final String GCM_SERVER = "@gcm.googleapis.com";
    private static final String LOG = "GcmConnector";
    private final AtomicInteger messageId;

    public GcmConnector(Context context) {
        this.context = context;
        messageId = new AtomicInteger(new Random(System.currentTimeMillis()).nextInt(Integer.MAX_VALUE));
    }

    public void connect() {
        if (gcm == null) {
            gcm = GoogleCloudMessaging.getInstance(context);
        }
        if (!isRegistred()) {
            register();
        }
    }

    private boolean isRegistred() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String gcmId = pref.getString(context.getResources().getString(R.string.pref_gcm_id), "");
        return !gcmId.isEmpty();
    }

    private void register() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    String regid = gcm.register(SENDER_ID);
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(context.getResources().getString(R.string.pref_gcm_id), regid);
                    editor.commit();
                } catch (IOException ex) {
                    Log.e(LOG, ex.getMessage());
                }

                return null;
            }
        }.execute(null, null, null);
    }

    public void send(Bundle data) {
        connect();
        new AsyncTask<Bundle, Void, Void>() {
            @Override
            protected Void doInBackground(Bundle... data) {
                try {
                    String id = Integer.toString(messageId.incrementAndGet());
                    gcm.send(SENDER_ID + GCM_SERVER, id, TTL, data[0]);
                } catch (IOException ex) {
                    Log.e(LOG, ex.getMessage());
                }

                return null;
            }
        }.execute(data, null, null);
    }
}
