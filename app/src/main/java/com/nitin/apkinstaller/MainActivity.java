package com.nitin.apkinstaller;

import android.Manifest;
import android.app.Activity;
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
import android.os.Build;
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
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.utils.Utility;
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

import static com.github.angads25.filepicker.view.FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    PackageInstaller packageInstaller;

    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;
    private ArrayList<String> splitApkApps;

    HashMap<String, List<String>> packageNameToSplitApksMapping;

    FilePickerDialog dialog;
    String packageToExport = null;

    public static final int EXTERNAL_READ_PERMISSION_GRANT_FOR_EXPORT = 113;

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


        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory().toString());
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        dialog = new FilePickerDialog(MainActivity.this,properties);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        packageInstaller =  getPackageManager().getPackageInstaller();



        button1.setOnClickListener((view)->
        {
            button1.setVisibility(View.GONE);
            getListOfApksWithSplitInstalled();
            mAdapter.notifyDataSetChanged();

        });

        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                String packageName = splitApkApps.get(position);
                Drawable icon = null;
                try {
                    icon = getPackageManager().getApplicationIcon(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                showDialog(packageName,icon);
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


    public void showDialog (final String packageName,Drawable icon) {
        new MaterialStyledDialog.Builder(this)
                .setTitle("Export Split Apks of " + packageName)
                .setDescription("Click Export to save splits of " + packageName )
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
        if (!Utility.checkStorageAccessPermissions(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((Activity) MainActivity.this).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_READ_PERMISSION_GRANT_FOR_EXPORT);
                packageToExport = packageName;
                return;
            }
        }

        // start service with package name and hashmap
        Intent intent = new Intent(this, MyIntentService.class);
        intent.putExtra("map", packageNameToSplitApksMapping);
        startService(intent);

        MyIntentService.startActionExtractApk(getApplicationContext(),packageName,packageNameToSplitApksMapping);
    }


    //Add this method to show Dialog when the required permission has been granted to the app.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {   //Show dialog if the read permission has been granted.
                        dialog.show();
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(MainActivity.this, "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
                }
            }
            case EXTERNAL_READ_PERMISSION_GRANT_FOR_EXPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: ");

                    if(packageToExport != null)
                        extractSplits(packageToExport);
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(MainActivity.this, "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
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
