/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.logs;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Container class for all log entries made against a single model.
 */
public class ModelLog {
    String mModelName;
    HashMap<String, LogGroup> mLogGroups;
	boolean filled;

    /**
     * Construct a model container.
     * @param aModelName
     */
	public ModelLog(String aModelName){
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
     * @param e
     */
	public void addEntry(LogEntry e){
        if(mLogGroups.containsKey(e.getLogGroup())) {
            LogGroup group = mLogGroups.get(e.getLogGroup());
            group.addEntry(e);
        } else {
            LogGroup group = new LogGroup(e.getLogGroup());
            mLogGroups.put(group.getGroupId(), group);
            group.addEntry(e);
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

    /**
     * Returns the number of entries in this log.
     * @return
     */
	public int getNumberLogGroups(){
		if(mLogGroups == null){
			return 0;
		}
		return mLogGroups.keySet().size();
	}
	
	final String RCSPEEDO_TEMP_DIR = "/sdcard/data/rcspeedo/";

    /**
     * Generates a CSV file with all of the entires in this log.
     * @return
     */
	public File generateSpeedLogFile() {
        try {
            File dir = new File(RCSPEEDO_TEMP_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("ERROR! Couldn't create logging output directory: '" + RCSPEEDO_TEMP_DIR + "'");
                return null;
            }
            File f = File.createTempFile("rcslog", ".csv");
            File realfile = new File(RCSPEEDO_TEMP_DIR + f.getName());
            PrintWriter pw = new PrintWriter(new FileWriter(realfile));
            //print out the header
            String logdate = null;
            pw.println(mModelName + "\n");
            for(LogGroup group : mLogGroups.values()) {
                for(LogEntry entry : group.getLogEntries()) {
                    if (entry.getType() == LogEntry.SPEED_ENTRY) {
                        SpeedLogEntry sentry = (SpeedLogEntry) entry;
                        if (logdate == null || !logdate.equals(sentry.getNiceDate())) {
                            logdate = sentry.getNiceDate();
                            pw.println("Entries for " + logdate);
                        }
                        pw.println(sentry.getNiceTime() + "," + sentry.getNiceSpeed());
                    }
                }
                pw.println();
            }

            pw.close();
            realfile.deleteOnExit();
            return realfile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
