package com.nitin.apkinstaller;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.nitin.apkinstaller.adapter.GalleryAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    PackageInstaller packageInstaller;

    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;
    private ArrayList<String> splitApkApps;

    HashMap<String, List<String>> packageNameToSplitApksMapping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button button1 = findViewById(R.id.button);

        splitApkApps = new ArrayList<>();
        packageNameToSplitApksMapping =  new HashMap<>();


        mAdapter = new GalleryAdapter(getApplicationContext(), splitApkApps,packageNameToSplitApksMapping);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);



        button1.setOnClickListener((view)->
        {
            button1.setVisibility(View.GONE);
            packageInstaller =  getPackageManager().getPackageInstaller();
            getListOfApksWithSplitInstalled();
            mAdapter.notifyDataSetChanged();

        });

        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                showDialog(splitApkApps.get(position));
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                installSplitApks();


                //int ret = installApk("/storage/emulated/0/Download/split/");

                //Log.d(TAG, "onClick: return value is " + ret);
                //extractSplits("com.airbnb.android",getListOfApksWithSplitInstalled());

                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    private void installSplitApks()
    {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this,properties);
        dialog.setTitle("Select a File");

        dialog.show();

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                //files is the array of the paths of files selected by the Application User.
                installApk(files);
            }
        });

    }


    public void showDialog (final String packageName) {
        Drawable icon = null;
        try
        {
             icon = getPackageManager().getApplicationIcon(packageName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }


        new MaterialStyledDialog.Builder(this)
                .setTitle("Export Split Apks!")
                .setDescription("export " + packageName)
                .setPositiveText("Export")
                .setIcon(icon)
                .setNegativeText("Cancel")
                .onNegative((dialog, which)->
                {
                    Log.d(TAG, "showDialog: dismiss" );
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Log.d("MaterialStyledDialogs", "Do something!");

                        extractSplits(packageName);
                    }
                }).show();
    }


    private HashMap<String, List<String>> getListOfApksWithSplitInstalled()
    {

        PackageManager  pm = getPackageManager();
        List<PackageInfo> pkginfoList = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for(PackageInfo packageInfo : pkginfoList)
        {
            if(packageInfo.splitNames != null) {
                ArrayList<String> splitPublicSourceDirs = new ArrayList<>(Arrays.asList(packageInfo.applicationInfo.splitPublicSourceDirs));

                splitPublicSourceDirs.add(packageInfo.applicationInfo.publicSourceDir);

                packageNameToSplitApksMapping.put(packageInfo.packageName,splitPublicSourceDirs);

                splitApkApps.add(packageInfo.packageName);
            }
        }

        Log.d(TAG, "getListOfApksWithSplitInstalled: " + splitApkApps);
        return packageNameToSplitApksMapping;
    }


    private void extractSplits(String packageName)
    {
        File myDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Splits");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private static class InstallParams {
        PackageInstaller.SessionParams sessionParams;
    }


    public int installApk(String[] files)
    {
        HashMap<String, Long> nameSizeMap = new HashMap<>();
        HashMap<String, String> filenameToPathMap = new HashMap<>();
        long totalSize = 0;
        int sessionId = 0;

        try {
            for (String file : files) {
                File listOfFile = new File(file);
                if (listOfFile.isFile()) {
                    Log.d(TAG, "installApk: " + listOfFile.getName());
                    nameSizeMap.put(listOfFile.getName(), listOfFile.length());
                    filenameToPathMap.put(listOfFile.getName(),file);
                    totalSize += listOfFile.length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        final InstallParams installParams = makeInstallParams(totalSize);

        try {
            sessionId = runInstallCreate(installParams);

            for(Map.Entry<String,Long> entry : nameSizeMap.entrySet())
            {
                runInstallWrite(entry.getValue(),sessionId, entry.getKey(), filenameToPathMap.get(entry.getKey()));
            }

            if (doCommitSession(sessionId, false )
                    != PackageInstaller.STATUS_SUCCESS) {
            }
            System.out.println("Success");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return sessionId;
    }


    public int installApk(String apkFolderPath)
    {
        HashMap<String, Long> nameSizeMap = new HashMap<>();
        long totalSize = 0;
        int sessionId = 0;
        File folder = new File(apkFolderPath);
        File[] listOfFiles = folder.listFiles();

        try {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    Log.d(TAG, "installApk: " + listOfFile.getName());
                    nameSizeMap.put(listOfFile.getName(), listOfFile.length());
                    totalSize += listOfFile.length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        final InstallParams installParams = makeInstallParams(totalSize);

        try {
            sessionId = runInstallCreate(installParams);

            for(Map.Entry<String,Long> entry : nameSizeMap.entrySet())
            {
                runInstallWrite(entry.getValue(),sessionId, entry.getKey(), apkFolderPath+entry.getKey());
            }

            if (doCommitSession(sessionId, false )
                    != PackageInstaller.STATUS_SUCCESS) {
            }
            System.out.println("Success");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return sessionId;

    }

    private int runInstallCreate(InstallParams installParams) throws RemoteException {
        final int sessionId = doCreateSession(installParams.sessionParams);
        System.out.println("Success: created install session [" + sessionId + "]");
        return sessionId;
    }

    private int doCreateSession(PackageInstaller.SessionParams params)
            throws RemoteException {

        int sessionId = 0 ;
        try {
            if(params == null)
            {
                Log.d(TAG, "doCreateSession: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!param is null");
            }
            sessionId = packageInstaller.createSession(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessionId;
    }

    private int runInstallWrite(long size, int sessionId , String splitName ,String path ) throws RemoteException {
        long sizeBytes = -1;

        String opt;
        sizeBytes = size;
        return doWriteSession(sessionId, path, sizeBytes, splitName, true /*logSuccess*/);
    }


    private int doWriteSession(int sessionId, String inPath, long sizeBytes, String splitName,
                               boolean logSuccess) throws RemoteException {
        if ("-".equals(inPath)) {
            inPath = null;
        } else if (inPath != null) {
            final File file = new File(inPath);
            if (file.isFile()) {
                sizeBytes = file.length();
            }
        }

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            session = packageInstaller.openSession(sessionId);

            if (inPath != null) {
                in = new FileInputStream(inPath);
            }

            out = session.openWrite(splitName, 0, sizeBytes);

            int total = 0;
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);

            if (logSuccess) {
                System.out.println("Success: streamed " + total + " bytes");
            }
            return PackageInstaller.STATUS_SUCCESS;
        } catch (IOException e) {
            System.err.println("Error: failed to write; " + e.getMessage());
            return PackageInstaller.STATUS_FAILURE;
        } finally {
            try {
                out.close();
                in.close();
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    
    private int doCommitSession(int sessionId, boolean logSuccess) throws RemoteException {
        PackageInstaller.Session session = null;
        try {
            try {
                session = packageInstaller.openSession(sessionId);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent callbackIntent = new Intent(getApplicationContext(), APKInstallService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
            session.close();

            System.out.println("install request sent");

            Log.d(TAG, "doCommitSession: " + packageInstaller.getMySessions());
            Log.d(TAG,
                    "doCommitSession: after session commit ");
            return 1;
        } finally {
            session.close();
        }
    }


    private InstallParams makeInstallParams(long totalSize ) {
        final PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        final InstallParams params = new InstallParams();
        params.sessionParams = sessionParams;
        String opt;
        sessionParams.setSize(totalSize);
        return params;
    }
}
