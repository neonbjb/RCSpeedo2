/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

/**
 * Log entry that provides extra information about a log group.
 */
public class GroupInfoEntry extends LogEntry {

    private int mLogGroup;
    private String mComments;

    /**
     * Constructs a GroupInfoEntry object.
     * @param aLogGroup The log group this entry belongs to.
     * @param aComments A string of info describing this group.
     */
    public GroupInfoEntry(int aLogGroup, String aComments) {
        mLogGroup = aLogGroup;
        mComments = aComments;
    }

    // Implementations of LogEntry methods.

    @Override
    public int getType() {
        return LogEntry.GROUP_INFO_ENTRY;
    }

    @Override
    public int getLogGroup() {
        return mLogGroup;
    }

    @Override
    public String getMain() {
        return mComments;
    }

    @Override
    public String getDateTime() {
        return null;
    }

    @Override
    public String getExt1() {
        return null;
    }

    @Override
    public String getExt2() {
        return null;
    }

    @Override
    public String getExt3() {
        return null;
    }
}
