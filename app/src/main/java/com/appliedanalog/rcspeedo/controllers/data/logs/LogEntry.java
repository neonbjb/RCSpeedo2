/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

/**
 * Class that represents a single row in the Logs database. Since these rows are of abstract types, this class also serves as
 * a factory to create any of the several LogEntry types that implement this interface.
 */
public abstract class LogEntry {
    // Types of LogEntry subclasses
	public static final int EMPTY_ENTRY = 1; /// Represents an empty log entry - used to place a model name in the database when there are no logs yet.
	public static final int SPEED_ENTRY = 2; /// Represents a speed log entry.
	
	public static final int NOT_IN_DATABASE = -1; /// If a LogEntry has this ID then it has not come from the database.
	
	private int mId = NOT_IN_DATABASE;
    private String mLogGroup;

	public static LogEntry constructLogEntry(int aId, int aType, String aLogGroup, String aMain, String aDateTime, String aExt1, String aExt2, String aExt3){
		LogEntry ret = null;
		switch(aType){
		case EMPTY_ENTRY:
			ret = new EmptyLogEntry();
			break;
		case SPEED_ENTRY:
			try{
				ret = new SpeedLogEntry(aMain, aDateTime);
			}catch(Exception e){
				e.printStackTrace();
			}
			break;
		}
		ret.mId = aId;
        ret.mLogGroup = aLogGroup;
		return ret;
	}

    /**
     * Returns the unique identifier for this log entry.
     * @return
     */
	public int getId() {
		return mId;
	}

    /**
     * Returns the grouping identifier for this log entry (which maps it to other log entries).
     * @return
     */
	public String getLogGroup() {
        return mLogGroup;
    }

    public void setLogGroup(String aNewGroup) {
        mLogGroup = aNewGroup;
    }

	public abstract int getType();
	public abstract String getMain();
	public abstract String getDateTime();
	public abstract String getExt1();
	public abstract String getExt2();
	public abstract String getExt3();
}
