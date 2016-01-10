/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a set of log entries that are all recorded in one session. In the context of this app,
 * this will generally be a set of passes at a point in mTime.
 */
public class LogGroup {
    int mGroupId;
    private ArrayList<LogEntry> mLogEntries;

    public LogGroup(int aId) {
        mGroupId = aId;
        mLogEntries = new ArrayList<>();
    }

    /**
     * Adds the specified LogEntry to this group.
     * @param aEntry
     */
    public void addEntry(SpeedLogEntry aEntry) {
        aEntry.setLogGroup(mGroupId);
        mLogEntries.add(aEntry);
    }

    /**
     * Returns the logging group ID.
     * @return
     */
    public int getGroupId() {
        return mGroupId;
    }

    /**
     * Sorts the log entries in this group. See LogEntry.compareTo for information on how entries are
     * sorted.
     */
    public void sort() {
        Collections.sort(mLogEntries);
    }

    /**
     * Returns the entries.
     * @return
     */
    public ArrayList<LogEntry> getLogEntries() {
        return mLogEntries;
    }
}
