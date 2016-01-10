/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import android.content.res.Resources;
import android.util.Log;

import com.appliedanalog.rcspeedo.R;

public class Strings {
	//Main screen UI resources
	public String START_LISTENING = "Start Listening";
	public String STOP_LISTENING = "Stop Listening";
	public String START_LOGGING = "Start Logging";
	public String STOP_LOGGING = "Start Logging";
	public String LOGGING_ON_PROG = "Logging to ";
	public String LOGGING_OFF_PROG = "Not Logging";
	public String HIGH_SPEED_IND = "Highest Speed: ";
	public String TEMPERATURE_IND = "Temperature: ";
	
	//Logging screen UI resources
	public String LOGGING_ENABLED = "Logging";
	public String LOGGING_DISABLED = LOGGING_OFF_PROG;
	public String NONE_NAME = "None";
	
	//String resources used in the logger.
	public String LOG_DEFAULT_MODEL_NAME = "airplane";
	public String CRT_LOG_TITLE = "Create Log";
	public String PICK_LOG_TITLE = "Pick a Log to Use";
	
	//Error messages presented to the user.
	public String ERR_NO_MIC = "Could not use the microphone on your phone. Close app and relaunch to try again.";
	public String ERR_NO_LOGS = "You have not created any logs. Press OK to open the log manager.";
	public String ERR_NO_LOG_SELECTED = "You do not have a log selected!";
	public String ERR_CANNOT_CREATE_LOG = "Not able to create the log! Do you have an SD card inserted?";
	public String ERR_NAME_REQUIRED = "You must enter a log name.";
	public String ERR_LOG_EXISTS = "This log name already exists, please try another name!";
	public String ERR_CANNOT_LOAD_LOG = "Internal Error: Could not pull up this log.";
	public String ERR_TEMP_NOT_NUMBER = "Bad input: Temperature must be a number.";
	public String ERR_UNLOCKER_DEFAULT = "Error unlocking app.";
	public String ERR_UNLOCKER_UNKNOWN = "Unknown error unlocking app.";
	public String ERR_NO_GOOGLE_ACCOUNT = "Cannot Unlock: No Google Account";
    public String ERR_NO_SPEEDS = "There are no speeds detected to save. Use this button after some speeds have been detected.";
	
	//Strings used to send e-mail notifications.
	public String MAIL_SPEED_SUBJECT = "RCSpeedo High Speed";
	public String MAIL_SPEED_PRETEXT_NO_MODEL = "My model airplane clocked ";
	public String MAIL_SPEED_PRETEXT_1 = "My ";
	public String MAIL_SPEED_PRETEXT_2 = " clocked ";
	public String MAIL_SPEED_PROMOTIONALS = " on the Android #RCSpeedo App";
	
	//Confirmation dialogs
	public String CNFRM_TITLE = "Are you sure?";
	public String CNFRM_BODY = "Are you sure you want to delete this log?";
	
	//Basics
	public String OK = "OK";
	public String YES = "yes";
	public String NO = "no";
	public String MPS = "m/s";
	public String KPH = "Km/h";
	public String MPH = "MPH";
	public String CL = "Celsius";
	public String C = "C";
	public String FL = "Fahrenheit";
	public String F = "F";
	
	static Strings inst;
	public static Strings getInstance(){
		return inst;
	}
	
	public static void init(Resources res){
		inst = new Strings();
		inst.START_LISTENING = res.getString(R.string.start_listening);
		inst.STOP_LISTENING = res.getString(R.string.stop_listening);
		inst.START_LOGGING = res.getString(R.string.start_logging);
		inst.STOP_LOGGING = res.getString(R.string.stop_logging);
		inst.LOGGING_ON_PROG = res.getString(R.string.logging_to);
		inst.LOGGING_OFF_PROG = res.getString(R.string.logging_status_startup);
		inst.HIGH_SPEED_IND = res.getString(R.string.highest_speed);
		inst.TEMPERATURE_IND = res.getString(R.string.temperature) + ":";
		
		inst.LOGGING_ENABLED = res.getString(R.string.logging);
		inst.LOGGING_DISABLED = res.getString(R.string.logging_status_startup);
		inst.NONE_NAME = res.getString(R.string.none);
		
		inst.LOG_DEFAULT_MODEL_NAME = res.getString(R.string.airplane);
		inst.CRT_LOG_TITLE = res.getString(R.string.create_new_log);
		inst.PICK_LOG_TITLE = res.getString(R.string.pick_log);
		
		inst.ERR_NO_MIC = res.getString(R.string.err_no_mic);
		inst.ERR_NO_LOGS = res.getString(R.string.err_no_logs);
		inst.ERR_NO_LOG_SELECTED = res.getString(R.string.err_no_log_selected);
		inst.ERR_CANNOT_CREATE_LOG = res.getString(R.string.err_cannot_create_log);
		inst.ERR_NAME_REQUIRED = res.getString(R.string.err_name_required);
		inst.ERR_LOG_EXISTS = res.getString(R.string.err_log_exists);
		inst.ERR_CANNOT_LOAD_LOG = res.getString(R.string.err_cannot_load_log);
		inst.ERR_TEMP_NOT_NUMBER = res.getString(R.string.err_temp_not_number);
		inst.ERR_UNLOCKER_DEFAULT = res.getString(R.string.err_unlocker_default);
		inst.ERR_UNLOCKER_UNKNOWN = res.getString(R.string.err_unlocker_unknown);
		inst.ERR_NO_GOOGLE_ACCOUNT = res.getString(R.string.err_no_google_account);

		inst.MAIL_SPEED_SUBJECT = res.getString(R.string.mail_speed_subject);
		inst.MAIL_SPEED_PRETEXT_NO_MODEL = res.getString(R.string.mail_speed_pretext_no_model);
		inst.MAIL_SPEED_PRETEXT_1 = res.getString(R.string.mail_speed_pretext_1);
		inst.MAIL_SPEED_PRETEXT_2 = res.getString(R.string.mail_speed_pretext_2);
		inst.MAIL_SPEED_PROMOTIONALS = res.getString(R.string.mail_speed_promotionals);
		
		inst.CNFRM_TITLE = res.getString(R.string.cnfrm_title);
		inst.CNFRM_BODY = res.getString(R.string.cnfrm_body);
		
		inst.OK = res.getString(R.string.ok);
		inst.YES = res.getString(R.string.yes);
		inst.NO = res.getString(R.string.no);
		inst.MPS = res.getString(R.string.mps);
		inst.KPH = res.getString(R.string.kph);
		inst.MPH = res.getString(R.string.mph);
		inst.CL = res.getString(R.string.cl);
		inst.C = res.getString(R.string.c);
		inst.FL = res.getString(R.string.fl);
		inst.F = res.getString(R.string.f);
        inst.ERR_NO_SPEEDS = res.getString(R.string.err_no_speeds);
		Log.d("TAAG", res.getString(R.string.stop_listening));
	}
	private Strings(){
		
	}
	
	//Utility functions
	public static String camel(String in){
		if(in.length() < 2){
			return in;
		}
		char c1 = in.charAt(0);
		return Character.toUpperCase(c1) + in.substring(1);
	}
}
