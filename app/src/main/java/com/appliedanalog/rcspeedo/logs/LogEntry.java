package com.appliedanalog.rcspeedo.logs;

import java.util.Date;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public abstract class LogEntry {
	public static final int EMPTY_ENTRY = 1;
	public static final int SPEED_ENTRY = 2;
	
	public static final int NOT_IN_DATABASE = -1; //if a LogEntry has this ID then it has not come from the database.
	
	int id = NOT_IN_DATABASE;
	
	public static LogEntry constructLogEntry(int id, int type, String main, String datetime, String ext1, String ext2, String ext3){
		LogEntry ret = null;
		switch(type){
		case EMPTY_ENTRY:
			ret = new EmptyLogEntry();
			break;
		case SPEED_ENTRY:
			try{
				ret = new SpeedLogEntry(main, datetime);
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		}
		ret.id = id;
		return ret;
	}
	
	public int getId(){
		return id;
	}
	
	public abstract int getType();
	public abstract String getMain();
	public abstract String getDateTime();
	public abstract String getExt1();
	public abstract String getExt2();
	public abstract String getExt3();
}
