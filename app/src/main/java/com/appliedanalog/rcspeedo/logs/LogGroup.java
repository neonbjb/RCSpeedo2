/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.logs;

import java.util.ArrayList;

/**
 * Represents a set of log entries that are all recorded in one session. In the context of this app,
 * this will generally be a set of passes at a point in time.
 */
public class LogGroup {
    String mGroupId;
    private ArrayList<LogEntry> mLogEntries;

    public LogGroup(String aId) {
        mGroupId = aId;
        mLogEntries = new ArrayList<>();
    }

    /**
     * Adds the specified LogEntry to this group.
     * @param aEntry
     */
    public void addEntry(LogEntry aEntry) {
        aEntry.setLogGroup(mGroupId);
        mLogEntries.add(aEntry);
    }

    /**
     * Returns the logging group ID.
     * @return
     */
    public String getGroupId() {
        return mGroupId;
    }

    /**
     * Returns the entries.
     * @return
     */
    public ArrayList<LogEntry> getLogEntries() {
        return mLogEntries;
    }
}
