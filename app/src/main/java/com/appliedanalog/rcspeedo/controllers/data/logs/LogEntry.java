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
public abstract class LogEntry implements Comparable<LogEntry> {
    // Types of LogEntry subclasses
    public static final int EMPTY_ENTRY = 1; /// Represents an empty log entry - used to place a model name in the database when there are no logs yet.
    public static final int SPEED_ENTRY = 2; /// Represents a mSpeed log entry.
    public static final int GROUP_INFO_ENTRY = 3; /// Provides extra information about a log group.

    public static final int NOT_IN_DATABASE = -1; /// If a LogEntry has this ID then it has not come from the database.

    private int mId = NOT_IN_DATABASE;

    public static LogEntry constructLogEntry(int aId, int aType, int aLogGroup, String aMain, String aDateTime, String aExt1, String aExt2, String aExt3) {
        LogEntry ret = null;
        switch (aType) {
            case EMPTY_ENTRY:
                ret = new EmptyLogEntry();
                break;
            case SPEED_ENTRY:
                try {
                    ret = new SpeedLogEntry(aLogGroup, aMain, aDateTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case GROUP_INFO_ENTRY:
                ret = new GroupInfoEntry(aLogGroup, aMain);
                break;
        }
        ret.mId = aId;
        return ret;
    }

    /**
     * Performs automatic ordering of all current log entry types, so they can be sorted in an array.
     *
     * @param aOther
     * @return
     */
    @Override
    public int compareTo(LogEntry aOther) {
        // Group info entries come before all other entries.
        if (aOther.getType() == GROUP_INFO_ENTRY) {
            if (getType() != GROUP_INFO_ENTRY) {
                return 1;
            } else {
                return 0;
            }
        } else if(getType() == GROUP_INFO_ENTRY) {
            return -1;
        } else if(aOther.getType() == SPEED_ENTRY && getType() == SPEED_ENTRY) {
            // Speed entries are only compareable to each other.
            SpeedLogEntry sleThis = (SpeedLogEntry)this;
            SpeedLogEntry other = (SpeedLogEntry)aOther;
            return sleThis.getTime().compareTo(other.getTime());
        }

        // If this point is reached, it means that neither of the entries is a GROUP_INFO_ENTRY and at least one of them
        // is either an EMPTY_ENTRY or not a recognized entry type. Either way we don't know how to sort
        // it so just return 0.
        return 0;
    }

    /**
     * Returns the unique identifier for this log entry.
     *
     * @return
     */
    public int getId() {
        return mId;
    }

    /**
     * Returns an integer identifying the type of entry this is.
     *
     * @return
     */
    public abstract int getType();

    /**
     * Returns the group that this LogEntry belongs to. Should return 0 if grouping is not supported.
     *
     * @return
     */
    public abstract int getLogGroup();

    /**
     * Returns the main textual information this entry stores.
     *
     * @return
     */
    public abstract String getMain();

    /**
     * Retursn the date and time when this entry was recorded.
     *
     * @return
     */
    public abstract String getDateTime();

    /**
     * Returns additional information about this entry.
     *
     * @return
     */
    public abstract String getExt1();

    /**
     * Returns additional information about this entry.
     *
     * @return
     */
    public abstract String getExt2();

    /**
     * Returns additional information about this entry.
     *
     * @return
     */
    public abstract String getExt3();
}
