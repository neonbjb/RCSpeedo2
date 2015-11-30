package com.appliedanalog.rcspeedo.controllers;

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
import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import com.appliedanalog.rcspeedo.logs.RCLog;

import java.util.ArrayList;

public class SpeedViewAdapter extends ArrayAdapter<DetectedSpeed>{
	private final Activity context;
	private ArrayList<View> views;

	public SpeedViewAdapter(Activity context) {
		super(context, R.layout.speeds);
		views = new ArrayList<View>();
		this.context = context;
	}
	
	int addcount = 0;
	@Override
	public void add(final DetectedSpeed dspd){
		super.insert(dspd, 0);
		
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.speeds, null, true);
		
		ViewHolder holder = new ViewHolder();
		holder.spd = (TextView)rowView.findViewById(R.id.tSpeedDisp);
		holder.share = (ImageView)rowView.findViewById(R.id.bShare);
		rowView.setTag(holder);		
		//initialize view
		holder.spd.setText(dspd.toString());
		holder.share.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Strings s = Strings.getInstance();
				
				//Get model name if possible
				String pretext = s.MAIL_SPEED_PRETEXT_NO_MODEL;
				RCLog rcl = RCLog.getCurrentLog();
				if(rcl != null){
					pretext =  s.MAIL_SPEED_PRETEXT_1 + rcl.getModel() + s.MAIL_SPEED_PRETEXT_2;
				}
				
				Intent i = new Intent(android.content.Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, s.MAIL_SPEED_SUBJECT);
				i.putExtra(Intent.EXTRA_TEXT, pretext + " " + UnitManager.getInstance().getDisplaySpeed(dspd.getSpeed()) + " " + s.MAIL_SPEED_PROMOTIONALS);
				context.startActivity(Intent.createChooser(i, "Share Speed"));
			}
		});
		views.add(0, rowView);
	}

	// static to save the reference to the outer class and to avoid access to
	// any members of the containing class
	static class ViewHolder {
		public TextView spd;
		public ImageView share;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(position >= views.size()){
			return convertView;
		}
		return views.get(position);
	}
}
