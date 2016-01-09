/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Class that represents a log entry for a specific detected speed.
 */
public class SpeedLogEntry extends LogEntry {
	private Date mTime;
    private double mSpeed;
    private String mLogGroup;
	
    static DateFormat sDateFormat = DateFormat.getDateInstance();
    static DateFormat sTimeFormat = DateFormat.getTimeInstance();
    static DateFormat sDateTimeFormat = DateFormat.getDateTimeInstance();

    /**
     * Constructor.
     */
	public SpeedLogEntry(){
		mTime = new Date(System.currentTimeMillis());
		mSpeed = 0.;
	}

    /**
     * Creates a log entry with the specified speed, occurring right now.
     * @param aSpeed Speed in m/s.
     * @param aLogGroup The group this log entry belongs to.
     */
	public SpeedLogEntry(double aSpeed, String aLogGroup){
		this(new Date(System.currentTimeMillis()), aSpeed, aLogGroup);
	}

    /**
     * Creates a log entry with the specified speed, occurring at the specified time.
     * @param aTime Time when the detection occurred.
     * @param aSpeed Speed in m/s.
     * @param aLogGroup The group this log entry belongs to.
     */
	public SpeedLogEntry(Date aTime, double aSpeed, String aLogGroup){
		mTime = aTime;
		mSpeed = aSpeed;
        mLogGroup = aLogGroup;
	}

    /**
     * Creates a log entry from a string-formatted time value.
     * @param aTime Time when the detection occurred as a string.
     * @param aSpeed Speed in m/s.
     * @param aLogGroup The group this log entry belongs to.
     * @throws ParseException
     */
	public SpeedLogEntry(String aTime, String aSpeed, String aLogGroup) throws ParseException{
		mTime = sDateTimeFormat.parse(aTime);
		mSpeed = Double.parseDouble(aSpeed);
        mLogGroup = aLogGroup;
	}

    /**
     * Get the time when the speed detection occurred.
     * @return
     */
	public String getNiceTime(){
		return sTimeFormat.format(mTime);
	}

    /**
     * Get the date when the speed detection occurred.
     * @return
     */
	public String getNiceDate(){
		return sDateFormat.format(mTime);
	}

    /**
     * Gets the speed, formatted in the locally specified units.
     * @return
     */
	public String getNiceSpeed(){
		return UnitManager.getInstance().getDisplaySpeed(mSpeed);
	}

    /**
     * Set the group that this entry belongs to.
     * @param aNewGroup
     */
    public void setLogGroup(String aNewGroup) {
        mLogGroup = aNewGroup;
    }

    // Overridden from LogEntry.
    @Override
	public int getType(){
		return SPEED_ENTRY;
	}

    @Override
    public String getLogGroup() {
        return mLogGroup;
    }

    @Override
	public String getMain(){
		return Double.toString(mSpeed);
	}

    @Override
	public String getDateTime(){
		return sDateTimeFormat.format(mTime);
	}

    @Override
	public String getExt1(){
		return "";
	}

    @Override
	public String getExt2(){
		return "";
	}

    @Override
	public String getExt3(){
		return "";
	}
}
