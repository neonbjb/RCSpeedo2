/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers.data.logs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Container class for all log entries made against a single model.
 */
public class ModelLog {
    String mModelName;
    HashMap<Integer, LogGroup> mLogGroups;
	boolean filled;

    /**
     * Construct a model container.
     * @param aModelName
     */
	public ModelLog(String aModelName){
        mLogGroups = new HashMap<>();
		this.mModelName = aModelName;
		filled = false;
	}

    /**
     * Add all of the specified entries to the log.
     * @param aEntries
     */
	public void fill(ArrayList<LogEntry> aEntries){
        for(LogEntry entry : aEntries) {
            addEntry(entry);
        }
		filled = true;
	}

    /**
     * Add the specified entry to the log.
     * @param aEntry
     */
	public void addEntry(LogEntry aEntry){
        if(aEntry.getLogGroup() != 0) {
            if(mLogGroups.containsKey(aEntry.getLogGroup())) {
                LogGroup group = mLogGroups.get(aEntry.getLogGroup());
                group.addEntry(aEntry);
            } else {
                LogGroup group = new LogGroup(aEntry.getLogGroup());
                mLogGroups.put(group.getGroupId(), group);
                group.addEntry(aEntry);
            }
        }
	}

    /**
     * Returns whether or not this ModelLog has been loaded with the fill() method. Generally this is
     * an indicator of whether or not it was filled from a database.
     * @return
     */
	public boolean isFilled() {
        return filled;
    }

    /**
     * Returns the model name.
     * @return
     */
	public String getName(){
		return mModelName;
	}

    public Collection<LogGroup> getLogGroups() {
        return mLogGroups.values();
    }
}
