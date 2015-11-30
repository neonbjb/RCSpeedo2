package com.appliedanalog.rcspeedo.logs;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class RCLog {
	ArrayList<LogEntry> entries = new ArrayList<LogEntry>();
	boolean filled;
	String logname;
	String modelname;
	int id = -1; //optional
	
	public RCLog(String logname, String modelname, int id){
		this(logname, modelname);
		this.id = id;
	}
	
	public RCLog(String logname, String modelname){
		this.logname = logname;
		this.modelname = modelname;
		filled = false;
	}
	
	public void fill(ArrayList<LogEntry> es){
		entries = es;
		filled = true;
	}
	
	public void addEntry(LogEntry e){
		entries.add(e);
	}
	
	public boolean isFilled(){
		return filled;
	}
	
	public Iterator<LogEntry> getIterator(){
		if(!filled) return null;
		return entries.iterator();
	}
	
	public String getModel(){
		return modelname;
	}
	
	public String getName(){
		return logname;
	}
	
	public boolean fromDatabase(){
		return id != -1;
	}
	
	public int getId(){
		return id;
	}
	
	public int getNumberEntries(){
		if(entries == null){
			return 0;
		}
		return entries.size();
	}
	
	final String RCSPEEDO_TEMP_DIR = "/sdcard/data/rcspeedo/";
	public File generateSpeedLogFile(){
		try{
			File dir = new File(RCSPEEDO_TEMP_DIR);
			if(!dir.exists() && !dir.mkdirs()){
				System.out.println("ERROR! Couldn't create logging output directory: '" + RCSPEEDO_TEMP_DIR + "'");
				return null;
			}
			File f = File.createTempFile("rcslog", ".csv");
			File realfile = new File(RCSPEEDO_TEMP_DIR + f.getName());
			PrintWriter pw = new PrintWriter(new FileWriter(realfile));
			//print out the header
			pw.println(logname + "," + modelname + "\n");
			Iterator<LogEntry> iter = entries.iterator();
			String logdate = null;
			while(iter.hasNext()){
				LogEntry entry = iter.next();
				if(entry.getType() == LogEntry.SPEED_ENTRY){
					SpeedLogEntry sentry = (SpeedLogEntry)entry;
					if(logdate == null || !logdate.equals(sentry.getNiceDate())){
						logdate = sentry.getNiceDate();
						pw.println("Entries for " + logdate);
					}
					pw.println(sentry.getNiceTime() + "," + sentry.getNiceSpeed());
				}
			}
			pw.close();
			realfile.deleteOnExit();
			return realfile;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	//This class also supports an application-wide logging state, supported by the methods below.
	private static RCLog current_log = null;
	private static boolean currently_logging = false;
	public static RCLog getCurrentLog(){
		return current_log;
	}
	
	public static void setCurrentLog(RCLog cl){
		if(currently_logging){
			stopLogging();
		}
		current_log = cl;
	}
	
	public static boolean startLogging(){
		if(current_log == null){
			return false;
		}
		currently_logging = true;
		return true;
	}
	
	public static boolean stopLogging(){
		if(!currently_logging){
			return false;
		}
		currently_logging = false;
		return true;
	}
	
	public static boolean isLogging(){
		return currently_logging;
	}
}
