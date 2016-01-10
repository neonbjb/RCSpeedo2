/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.appliedanalog.rcspeedo.controllers.data.logs.LogEntry;
import com.appliedanalog.rcspeedo.controllers.data.logs.LogGroup;
import com.appliedanalog.rcspeedo.controllers.data.logs.ModelLog;
import com.appliedanalog.rcspeedo.controllers.data.logs.SpeedLogEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;

/**
 * Controller class that manages interaction with the SQLite database to persist logging data.
 */
public class LoggingDbController extends SQLiteOpenHelper{
    final String TAG = "LogDbController";

	private static final String TBL = "logsv2";
	private static final String INSERT_FIELDS = "(model,loggroup,type,date,main,ext1,ext2,ext3)";
	private static final String APP_TABLE_CREATE = "create table " + TBL + " (id integer auto_increment primary key, " +	
									"model varchar(100), loggroup int default 0, type integer default 1, date text, main text, ext1 text, ext2 text, ext3 text);";
    private final String RCSPEEDO_TEMP_DIR = "/sdcard/data/rcspeedo/";
	
	static LoggingDbController sInstance = null;

    /**
     * Fetches the Singleton instance.
     * @param context Activity context.
     * @return
     */
	public static LoggingDbController getInstance(Context aContext){
		if(sInstance == null){
			sInstance = new LoggingDbController(aContext);
		}
		return sInstance;
	}
	
	private LoggingDbController(Context aContext){
		super(aContext, TBL, null, 2);
	}

    /**
     * Add an entry to the specified ModelLog and the backing database. This entry will persist after this method is called.
     * @param aEntry
     * @param aLog
     */
	public void addLogEntry(LogEntry aEntry, ModelLog aLog){
        aLog.addEntry(aEntry);
		final String sql = "insert into " + TBL + " " + INSERT_FIELDS + " values ('" + aLog.getName() +
                "', '" + aEntry.getLogGroup() + "', '" + aEntry.getType() + "', '" + aEntry.getDateTime() + "', '" + aEntry.getMain() +
                "', '" + aEntry.getExt1() + "', '" + aEntry.getExt2() + "', '" + aEntry.getExt3() + "');";
        Log.v(TAG, sql);
        getWritableDatabase().execSQL(sql);
	}

    /**
     * Deletes all entries correspnoding to the specified model.
     * @param aModel
     */
	public void deleteModelLog(String aModel){
		getWritableDatabase().execSQL("delete from " + TBL + " where model='" + aModel + "';");
	}

    /**
     * Retrieve a collection of all the available models and their respective entries.
     * @return
     */
	public Collection<ModelLog> getAllModels(){
		HashMap<String, ModelLog> logs = new HashMap<String, ModelLog>();
		Cursor curs = getReadableDatabase().rawQuery("select * from " + TBL + ";", null);
		if(!curs.moveToFirst()) return logs.values();
		do{
			String name = curs.getString(1);
            int type = curs.getInt(3);

            if(!logs.containsKey(name)) {
                logs.put(name, new ModelLog(name));
            }

            if(type == LogEntry.EMPTY_ENTRY) {
                continue;
            }

            // Add any entry that is not empty.
			ModelLog log = logs.get(name);
			try{
				log.addEntry(LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getInt(2), curs.getString(5),
														curs.getString(4), curs.getString(6),
									      				curs.getString(7), curs.getString(8)));
			}catch(Exception e){
				System.out.println("Parse exception with a log date!");
				e.printStackTrace();
			}
		}while(curs.moveToNext());
		curs.close();
		return logs.values();
	}

    /**
     * Gets the specified Model, filled with its log entries.
     * @param aModel
     * @return
     */
	public ModelLog getModelLog(String aModel){
		Cursor curs = getReadableDatabase().rawQuery("select * from " + TBL + " where model='" + aModel + "';", null);
		ModelLog log = null;
		if(!curs.moveToFirst()) return null;
		do{
			if(log == null){
				log = new ModelLog(curs.getString(1));
			}
			try{
				log.addEntry(
						LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getInt(2), curs.getString(5),
												   curs.getString(4), curs.getString(6),
								      			   curs.getString(7), curs.getString(8)));
			}catch(Exception e){
				System.out.println("Parse exception with a log date!");
				e.printStackTrace();
			}
		}while(curs.moveToNext());
		curs.close();
		return log;
	}

    /**
     * Generates a CSV file with all of the entries bound to the specified model.
     * @param aLog The model to scan.
     * @return
     */
    public File generateSpeedLogFile(ModelLog aLog) {
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
            pw.println(aLog.getName() + "\n");
            for(LogGroup group : aLog.getLogGroups()) {
                // Sort the group for the most appropriate display.
                group.sort();
                for(LogEntry entry : group.getLogEntries()) {
                    if (entry.getType() == LogEntry.SPEED_ENTRY) {
                        SpeedLogEntry sentry = (SpeedLogEntry) entry;
                        pw.println(sentry.getNiceDate() + "," + sentry.getNiceTime() + "," + sentry.getNiceSpeed());
                    } else if (entry.getType() == LogEntry.GROUP_INFO_ENTRY) {
                        pw.println(entry.getMain());
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

    /**
     * Generates a GroupId value that is unique in the backing database.
     * @return
     */
    public int generateGroupId() {
        // Start with a good candidate.
        int potentialGroupId = (int) System.currentTimeMillis();
        // Check that it is unique
        while(!isIdUnique(potentialGroupId)) {
            potentialGroupId++;
        }
        return potentialGroupId;
    }

    // Interface methods for SQLiteOpenHelper.

    @Override
    public void onCreate(SQLiteDatabase aDb){
        aDb.execSQL(APP_TABLE_CREATE);
    }
	
	@Override
	public void onUpgrade(SQLiteDatabase aDb, int oldVersion, int newVersion) {
		//called when alter table is needed, will be implemented if/when it is needed
	}

    private boolean isIdUnique(int aId) {
        // IDs of 0 are invalid
        if(aId == 0) {
            return false;
        }

        try {
            Cursor curs = getReadableDatabase().rawQuery("select id from " + TBL + " where loggroup='" + aId + "';", null);
            boolean result = curs.moveToFirst();
            curs.close();
            return !result;
        } catch(Exception e) {
            e.printStackTrace();
            // Make a wild guess.
            return true;
        }
    }
}
