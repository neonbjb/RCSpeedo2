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

public class SpeedLogEntry extends LogEntry {
	Date time;
	double speed;
	
    static DateFormat datesdf = DateFormat.getDateInstance();
    static DateFormat timesdf = DateFormat.getTimeInstance();
    static DateFormat fullsdf = DateFormat.getDateTimeInstance();
	
	public SpeedLogEntry(){
		time = new Date(System.currentTimeMillis());
		speed = 0.;
	}
	
	public SpeedLogEntry(double s){
		this(new Date(System.currentTimeMillis()), s);
	}
	
	public SpeedLogEntry(Date t, double s){
		time = t;
		speed = s;
	}
	
	public SpeedLogEntry(String t, String s) throws ParseException{
		time = fullsdf.parse(t);
		speed = Double.parseDouble(s);
	}
	
	public String getNiceTime(){
		return timesdf.format(time);
	}
	
	public String getNiceDate(){
		return datesdf.format(time);
	}
	
	public String getNiceSpeed(){
		return UnitManager.getInstance().getDisplaySpeed(speed);
	}
	
	public String getFullDateTime(){
		return fullsdf.format(time);
	}
	
	public int getType(){
		return SPEED_ENTRY;
	}
	
	public String getMain(){
		return Double.toString(speed);
	}
	
	public String getDateTime(){
		return getFullDateTime();
	}
	
	public String getExt1(){
		return "";
	}
	
	public String getExt2(){
		return "";
	}
	
	public String getExt3(){
		return "";
	}
}
