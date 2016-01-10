/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data;

import com.appliedanalog.rcspeedo.controllers.UnitManager;

import java.sql.Date;
import java.text.DateFormat;

/**
 * Container class that holds a detected speed. This is managed by the SpeedLogController and is
 * used by the list view adapter and log manager.
 */
public class DetectedSpeed {
	private Date mTimeStamp;
	private double mSpeed;
	
	static DateFormat mDateFormat;
	static{
		//this formats the time stamps on the speeds
		mDateFormat = DateFormat.getTimeInstance();
	}

	/**
	 * Construct a DetectedSpeed that was JUST detected.
	 * @param s Detected speed in m/s.
	 */
	public DetectedSpeed(double s){
		this(new Date(System.currentTimeMillis()), s);
	}

	/**
	 * Construct a DetectedSpeed that was detected in the past.
	 * @param ts The date representing when this speed was detected.
	 * @param s Speed in m/s.
	 */
	public DetectedSpeed(Date ts, double s){
		mTimeStamp = ts;
		mSpeed = s;
	}

	/**
	 * Returns when this speed was detected.
	 * @return
	 */
	public Date getTimestamp(){
		return mTimeStamp;
	}

	/**
	 * Returns the speed, in m/s.
	 * @return
	 */
	public double getSpeed(){
		return mSpeed;
	}

	/**
	 * Returns the speed formatted with the locally specified units.
	 * @return
	 */
	private String getFormattedSpeed(){
		return UnitManager.getInstance().getDisplaySpeed(mSpeed);
	}

	/**
	 * Returns a string containing the formatted speed and when it was detected.
	 * @return
	 */
	public String toString(){
		return getFormattedSpeed() + "  (" + mDateFormat.format(mTimeStamp) + ")";
	}
}
