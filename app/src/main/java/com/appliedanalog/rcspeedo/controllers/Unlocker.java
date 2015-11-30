package com.appliedanalog.rcspeedo.controllers;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Unlocker implements Runnable{
	final String TAG = "Unlocker";
	
	enum LockingSchemes{
		AlwaysOpen,
		MonthlyUnlock,
		UnlockOnce,
		AlwaysUnlock
	}
	
	final LockingSchemes lockingScheme = LockingSchemes.UnlockOnce;
	
	static final int ROOM_TEMPERATURE = 21; //default temperature
	boolean trial_version = true;
	boolean unlocked = false;
	boolean needs_unlock = true;
	boolean got_result = false;
	boolean result = false;
	Activity owner;
	String error_message;
	Strings s;
	
	long unlock_timestamp;
	final int unlock_longetivity = 60 * 1000; //one minute
	
    final long TIME_TILL_UNLOCK_EXPIRES_MS = (long)1000 * (long)(60 * 60 * 24 * 30);
	
	public Unlocker(Activity o){
		owner = o;
		s = Strings.getInstance();
		error_message = s.ERR_UNLOCKER_DEFAULT;
		
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(owner);
    	long last_unlock_time = pref.getLong("last_unlock_date", 0);
    	if(System.currentTimeMillis() - last_unlock_time > TIME_TILL_UNLOCK_EXPIRES_MS){
    		needs_unlock = true;
    	}else{
    		needs_unlock = false;
    	}
	} 
	
	public boolean unlock(){
		// Cannot access network on main thread due to Android restrictions, so we have to wait on another thread to do it.
		got_result = false;
		(new Thread(this)).start();
		while(!got_result){
			try{
				Thread.sleep(10);
			}catch(Exception e){}
		}
		return result;
	}
	
	public void run(){
    	boolean unlocked = false;
    	switch(lockingScheme){
    	case AlwaysOpen:
    		unlocked = true;
    		break;
    	case MonthlyUnlock:
        	if(needs_unlock){
        		unlocked = unlock_request();
            	if(unlocked){
            		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(owner);
                	SharedPreferences.Editor editor = pref.edit();
                	editor.putLong("last_unlock_date", System.currentTimeMillis());
                	editor.commit();
                	needs_unlock = false;
            	}
        	}else{
        		unlocked = true;
        	}
        	break;
    	case UnlockOnce:
    		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(owner);
    		String myacct = getGoogleAccount();
    		if(myacct == null){
    			error_message = s.ERR_NO_GOOGLE_ACCOUNT;
    			result = false;
    			got_result = true;
    			return;
    		}
    		if(jumble(myacct).equals(pref.getString("unlock_acct", "doesnotwork"))){
    			unlocked = true;
    		}else{
    			if(unlock_request_spec()){
    				SharedPreferences.Editor editor = pref.edit();
    				editor.putString("unlock_acct", jumble(myacct));
    				editor.commit();
    				unlocked = true;
    			}else{
    				unlocked = false;
    			}
    		}
    		break;
    	case AlwaysUnlock:
    		result = unlock_request();
    		got_result = true;
    		return;
    	}
    	
    	result = unlocked;
    	got_result = true;
	}
	
	public synchronized boolean unlock_request(){
		return unlock_request("http://www.rcspeedo.info/activate.php?gid=");
	}
	
	public synchronized boolean unlock_request_spec(){
		return unlock_request("http://www.rcspeedo.info/activate_special.php?gid=");
	}
	
	public synchronized boolean unlock_request(String baseurl){
		if(!trial_version || 
			(unlocked && (unlock_timestamp + unlock_longetivity) > System.currentTimeMillis())) return true;
		unlocked = false;
		try{
			String acct = getGoogleAccount();
			if(acct == null) return false;
			
			Log.v(TAG, "Sending out an unlock request for " + acct);
			
			String query = baseurl + acct;
			URL url = new URL(query.replace(" ", "%20"));
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(new DefaultHandler(){
				@Override
				public void startElement(String namespaceURI, String localName,
						String qName, Attributes atts) throws SAXException {
					if (localName.equals("activated")) { 
						unlocked =  atts.getValue("value").trim().equals("true");
					}
				}
			});
			xr.parse(new InputSource(url.openStream()));
			if(unlocked){
				Log.v(TAG, "RCS successfully unlocked!");
				unlock_timestamp = System.currentTimeMillis();
			}else{
				Log.v(TAG, "RCS cannot be unlocked!");
				error_message = s.ERR_UNLOCKER_DEFAULT;
			}
			return unlocked;
		}catch(Exception e){
			e.printStackTrace();
		}
		error_message = s.ERR_UNLOCKER_UNKNOWN;
		return false;
	}
	
	public String errorMessage(){
		return error_message;
	}
	
	private static String getGoogleAccount(Context c){
		Account[] accounts = AccountManager.get(c).getAccounts();
		String acct = null;
		for(int x = 0; x < accounts.length; x++){
			if(accounts[x].type.equals("com.google")){
				acct = accounts[x].name;
			}
		}
		if(acct == null){
			System.out.println("User doesnt have a google account!");
			return null;
		}
		return acct;
	}
	
	private String getGoogleAccount(){
		return getGoogleAccount(owner);
	}
	
	//rudimentary jumbling algorithm; simply makes it harder to crack the game.. certainly isnt impossible though
	private String jumble(String inc){
		return inc;
	}
}
