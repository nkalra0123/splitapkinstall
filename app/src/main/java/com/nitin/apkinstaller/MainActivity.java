package com.nitin.apkinstaller;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    PackageInstaller packageInstaller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                packageInstaller =  getPackageManager().getPackageInstaller();

                int ret = installApk("/storage/emulated/0/Download/split/");

                Log.d(TAG, "onClick: return value is " + ret);

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
            Log.d(TAG, "doCommitSession: after session commit ");
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
