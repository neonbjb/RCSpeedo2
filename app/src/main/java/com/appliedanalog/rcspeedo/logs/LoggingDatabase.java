/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.logs;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collection;
import java.util.HashMap;

/**
 * Controller class that manages interaction with the SQLite database to persist logging data.
 */
public class LoggingDatabase extends SQLiteOpenHelper{
	private static final String TBL = "logsv2";
	private static final String INSERT_FIELDS = "(model,loggroup,type,date,main,ext1,ext2,ext3)";
	private static final String APP_TABLE_CREATE = "create table " + TBL + " (id integer auto_increment primary key, " +	
									"model varchar(100), loggroup varchar(100), type integer, date text, main text, ext1 text, ext2 text, ext3 text);";
	
	static LoggingDatabase sInstance = null;

    /**
     * Fetches the Singleton instance.
     * @param context Activity context.
     * @return
     */
	public static LoggingDatabase getInstance(Context aContext){
		if(sInstance == null){
			sInstance = new LoggingDatabase(aContext);
		}
		return sInstance;
	}
	
	private LoggingDatabase(Context aContext){
		super(aContext, TBL, null, 2);
	}

    /**
     * Add an entry to the specified ModelLog. This entry will persist after this method is called.
     * @param aEntry
     * @param aLog
     */
	public void addLogEntry(LogEntry aEntry, ModelLog aLog){
		getWritableDatabase().execSQL("insert into " + TBL + " " + INSERT_FIELDS + " values ('" + aLog.getName() +
                "', '" + aEntry.getLogGroup() + "', '" + aEntry.getType() + "', '" + aEntry.getDateTime() + "', '" + aEntry.getMain() +
                "', '" + aEntry.getExt1() + "', '" + aEntry.getExt2() + "', '" + aEntry.getExt3() + "');");
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
			double spd = curs.getDouble(4);
			if(!logs.containsKey(name)){
				logs.put(name, new ModelLog(name));
			}
			if(spd == -1.) continue; //this is just a stub
			ModelLog log = logs.get(name);
			try{
				log.addEntry(LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getString(2), curs.getString(4),
														curs.getString(5), curs.getString(6),
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
						LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getString(2), curs.getString(4),
												   curs.getString(5), curs.getString(6),
								      			   curs.getString(7), curs.getString(8)));
			}catch(Exception e){
				System.out.println("Parse exception with a log date!");
				e.printStackTrace();
			}
		}while(curs.moveToNext());
		curs.close();
		return log;
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
}
