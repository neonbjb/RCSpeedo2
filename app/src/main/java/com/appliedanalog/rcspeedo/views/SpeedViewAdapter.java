/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.views;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.DopplerController;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import com.appliedanalog.rcspeedo.logs.RCLog;

import java.util.ArrayList;

public class SpeedViewAdapter extends ArrayAdapter<DetectedSpeed>{

    static final String TAG = "SpeedViewAdapter";

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	public static class ViewHolder {
		public TextView mSpeedText;
		public ImageView mShareImage;
        public ImageView mDeleteImage;
		public DetectedSpeed mDetectedSpeed;

		public ViewHolder(DetectedSpeed aSpeed) {
			mDetectedSpeed = aSpeed;
		}
	}

	private final Activity mContext;
	private ArrayList<View> mViews;

	public SpeedViewAdapter(Activity context) {
		super(context, R.layout.speeds);
		mViews = new ArrayList<View>();
		this.mContext = context;
	}

	@Override
	public void add(final DetectedSpeed dspd){
		super.insert(dspd, 0);
		
		LayoutInflater inflater = mContext.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.speeds, null, true);
		
		ViewHolder holder = new ViewHolder(dspd);
		holder.mSpeedText = (TextView)rowView.findViewById(R.id.tSpeedDisp);
		holder.mShareImage = (ImageView)rowView.findViewById(R.id.bShare);
        holder.mDeleteImage = (ImageView)rowView.findViewById(R.id.bDeleteSpeed);
		rowView.setTag(holder);		
		//initialize view
		holder.mSpeedText.setText(dspd.toString());
		holder.mShareImage.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Strings s = Strings.getInstance();

                //Get model name if possible
                String pretext = s.MAIL_SPEED_PRETEXT_NO_MODEL;
                RCLog rcl = RCLog.getCurrentLog();
                if (rcl != null) {
                    pretext = s.MAIL_SPEED_PRETEXT_1 + rcl.getName() + s.MAIL_SPEED_PRETEXT_2;
                }

                Intent i = new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, s.MAIL_SPEED_SUBJECT);
                i.putExtra(Intent.EXTRA_TEXT, pretext + " " + UnitManager.getInstance().getDisplaySpeed(dspd.getSpeed()) + " " + s.MAIL_SPEED_PROMOTIONALS);
                mContext.startActivity(Intent.createChooser(i, "Share Speed"));
            }
        });
        holder.mDeleteImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DopplerController.getInstance().removeSpeed(dspd);
            }
        });

		mViews.add(0, rowView);
	}

    @Override
    public void remove(final DetectedSpeed aSpeed) {
        super.remove(aSpeed);

        View viewToRemove = null;
        for(View view : mViews) {
            ViewHolder holder = (ViewHolder)view.getTag();
            if(holder.mDetectedSpeed == aSpeed) {
                viewToRemove = view;
                break;
            }
        }

        if(viewToRemove != null) {
            mViews.remove(viewToRemove);
        }
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(position >= mViews.size()){
			return convertView;
		}
		return mViews.get(position);
	}
}
