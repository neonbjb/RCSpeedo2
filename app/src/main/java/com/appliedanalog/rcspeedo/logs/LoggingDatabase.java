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

public class LoggingDatabase extends SQLiteOpenHelper{
	private static final String TBL = "logs";	
	private static final String INSERT_FIELDS = "(log,model,type,date,main,ext1,ext2,ext3)";
	private static final String APP_TABLE_CREATE = "create table " + TBL + " (id integer auto_increment primary key, " +	
									"log text, model text, type integer, date text, main text, ext1 text, ext2 text, ext3 text);";
	
	static LoggingDatabase singleton = null;
	public static LoggingDatabase getData(Context context){
		if(singleton == null){
			singleton = new LoggingDatabase(context);
		}
		return singleton;
	}
	
	private LoggingDatabase(Context context){
		super(context, TBL, null, 2);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL(APP_TABLE_CREATE);
	}

	public void addLogEntry(LogEntry entry, RCLog log){
		getWritableDatabase().execSQL("insert into " + TBL + " " + INSERT_FIELDS + " values ('" + log.getName() + 
				"', '" + log.getModel() + "', '" + entry.getType() + "', '" + entry.getDateTime() + "', '" + entry.getMain() +
				"', '" + entry.getExt1() + "', '" + entry.getExt2() + "', '" + entry.getExt3() + "');");
	}
	
	public void deleteLog(String lname){
		getWritableDatabase().execSQL("delete from " + TBL + " where log='" + lname + "';");
	}
	
	public Collection<RCLog> getAllLogs(){
		HashMap<String, RCLog> logs = new HashMap<String, RCLog>();
		Cursor curs = getReadableDatabase().rawQuery("select * from " + TBL + ";", null);
		if(!curs.moveToFirst()) return logs.values();
		do{
			int id = curs.getInt(0);
			String name = curs.getString(1);
			String model = curs.getString(2);
			double spd = curs.getDouble(4);
			if(!logs.containsKey(name)){
				logs.put(name, new RCLog(name, model, id));
			}
			if(spd == -1.) continue; //this is just a stub
			RCLog log = logs.get(name);
			try{
				log.addEntry(LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getString(4), 
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
	
	public RCLog getLog(String logname){
		Cursor curs = getReadableDatabase().rawQuery("select * from " + TBL + " where log='" + logname + "';", null);
		RCLog log = null;
		if(!curs.moveToFirst()) return null;
		do{
			if(log == null){
				log = new RCLog(curs.getString(1), curs.getString(2), curs.getInt(0));
			}
			try{
				log.addEntry(
						LogEntry.constructLogEntry(curs.getInt(0), curs.getInt(3), curs.getString(4), 
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
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//called when alter table is needed, will be implemented if/when it is needed
	}
}
