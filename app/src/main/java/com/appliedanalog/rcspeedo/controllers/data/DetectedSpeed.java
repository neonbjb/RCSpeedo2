package com.appliedanalog.rcspeedo.controllers.data;

import java.sql.Date;
import java.text.DateFormat;

public class DetectedSpeed {
	
	Date timestamp;
	double spd;
	double med_freq;
	
	static DateFormat date_format;
	static{
		//this formats the time stamps on the speeds
		date_format = DateFormat.getTimeInstance();
	}
	
	public DetectedSpeed(double s, double freq){
		this(new Date(System.currentTimeMillis()), s, freq);
	}
	
	public DetectedSpeed(Date ts, double s, double freq){
		timestamp = ts;
		spd = s;
		med_freq = freq;
	}
	
	public Date getTimestamp(){
		return timestamp;
	}
	
	public double getSpeed(){
		return spd;
	}
	
	public double getFrequencySignature(){
		return med_freq;
	}
	
	private String getFormattedSpeed(){
		return UnitManager.getInstance().getDisplaySpeed(spd);
	}
	
	public String toString(){
		return getFormattedSpeed() + "  (" + date_format.format(timestamp) + ")";
	}
}
