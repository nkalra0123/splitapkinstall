package com.faith.apkinstaller;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class MyIntentService extends IntentService {
    private static final String TAG = "MyIntentService";
    private static final String ACTION_EXPORT_APK = "com.faith.apkinstaller.action.EXPORT_APK";

    private static final String PACKAGENAME = "com.faith.apkinstaller.extra.PACKAGENAME";
    private static final String PACKAGE_NAME_TO_SPLIT_APKS_MAPPING = "com.faith.apkinstaller.extra.PACKAGE_NAME_TO_SPLIT_APKS_MAPPING";

    public MyIntentService() {
        super("MyIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionExtractApk(Context context, String param1, HashMap<String, List<String>> packageNameToSplitApksMapping) {
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_EXPORT_APK);
        intent.putExtra(PACKAGENAME, param1);
        intent.putExtra(PACKAGE_NAME_TO_SPLIT_APKS_MAPPING, packageNameToSplitApksMapping);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_EXPORT_APK.equals(action)) {
                final String param1 = intent.getStringExtra(PACKAGENAME);
                HashMap<String, List<String>> hashMap = (HashMap<String, List<String>>)intent.getSerializableExtra(PACKAGE_NAME_TO_SPLIT_APKS_MAPPING);
                handleActionExtractApk(param1, hashMap);
            }
        }
    }



    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionExtractApk(String param1, HashMap<String, List<String>> packageNameToSplitApksMapping) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Extracting Apk")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Extracing split apk of " + param1 + " in Progress"))
                        .setContentText("In Progress")
                        .setChannelId("export_sound")
                        .setSmallIcon(R.drawable.ic_launcher_background);
        NotificationManager mNotificationManager;
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(1, mBuilder.build());
        extractSplits(param1,packageNameToSplitApksMapping);
        mBuilder.setContentText("Apk extraction Done");
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Extracing split apk of " + param1 + " Done "));

        mNotificationManager.notify(1, mBuilder.build());
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private void extractSplits(String packageName,HashMap<String, List<String>> packageNameToSplitApksMapping)
    {
        File myDirectory = new File(Environment.getExternalStorageDirectory(), "Splits");
        myDirectory.mkdir();

        File apkFolder = new File(myDirectory,packageName);
        apkFolder.mkdir();

        List<String> splits = packageNameToSplitApksMapping.get(packageName);

        for(String filePath : splits)
        {
            File src = new File(filePath);
            Log.d(TAG, "extractSplits: src " + src);

            File dst = new File(apkFolder,filePath.substring(filePath.lastIndexOf("/")));
            Log.d(TAG, "extractSplits: dst " + dst);

            try {
                copy(src,dst);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}