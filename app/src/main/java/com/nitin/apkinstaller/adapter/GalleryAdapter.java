package com.nitin.apkinstaller.adapter;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nitin.apkinstaller.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.MyViewHolder> {

    private Context mContext;
    private static final String TAG = "GalleryAdapter";
    private ArrayList<String> splitApkApps;
    HashMap<String, List<String>> packageNameToSplitApksMapping;


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView packageName;
        public TextView description;

        ImageView icon;

        public MyViewHolder(View view) {
            super(view);
            packageName = (TextView) view.findViewById(R.id.title);
            icon = (ImageView) view.findViewById(R.id.imageView);
            description = view.findViewById(R.id.splitsdescription);
        }
    }


    public GalleryAdapter(Context context, ArrayList<String> splitApkApps, HashMap<String, List<String>> packageNameToSplitApksMapping) {
        mContext = context;
        this.splitApkApps = splitApkApps;
        this.packageNameToSplitApksMapping = packageNameToSplitApksMapping;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_thumbnail, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Log.d(TAG, "onBindViewHolder: " + splitApkApps.get(position));
        String packageName = splitApkApps.get(position);

        holder.packageName.setText(packageName);

        holder.description.setText(packageNameToSplitApksMapping.get(packageName).toString());

        try
        {
            Drawable icon = mContext.getPackageManager().getApplicationIcon(splitApkApps.get(position));
            holder.icon.setImageDrawable(icon);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount() returned: " + splitApkApps.size());
        return splitApkApps.size();
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private GalleryAdapter.ClickListener clickListener;

        public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final GalleryAdapter.ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}