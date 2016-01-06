/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.concurrent.Semaphore;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;


public class Weather implements LocationListener{
	final String TAG = "Weather";
	static final int ROOM_TEMPERATURE = 21; //default temperature
	final int MIN_ACCURACY_METERS = 20;
	
	LocationManager locmanager;
	boolean temp_failed = false;
	boolean requesting_locs = false;
	TemperatureListener templistener;
	
	static NumberFormat format;
	static{
		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(2);
	}
	
	public Weather(LocationManager locman, TemperatureListener templisten){
		locmanager = locman;
		templistener = templisten;
	}
	
	public int pullTemperatureFromLastKnown(){
		temp_failed = false;
		Location last_loc = getLastLocation();
		if(last_loc == null){
			System.out.println("Error retrieving last location, getting correct temperature may take some time..");
			temp_failed = true;
			return ROOM_TEMPERATURE;
		}
		int temp = getTemperatureFromCoordinates(last_loc.getLatitude(), last_loc.getLongitude());
		if(temp == -1){
			temp_failed = true;
			return ROOM_TEMPERATURE;
		}
		return temp;
	}
	
	public void refreshTemperature(){
		//this will fire off location changed events, which will then forward the location along to the assigned listener.
		try{
			if(canUseGPS()){
				requesting_locs = true;
				locmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			}else{
				System.out.println("Cannot use the GPS for some reason, unable to provide accurate temperature.");
			}
		}catch(Exception e){}
	}
	
	/**
	 * Uses google weather to obtain the temperature at the passed lat/lon. 
	 * @return Temperature in Celsius. -1 if there was an error
	 */
	int _retrieved_deg_c;
	String _retrieved_hum;
	int getTemperatureFromCoordinates(double lat, double lon){
		try{
			_retrieved_deg_c = -1;
			String query = "http://api.openweathermap.org/data/2.1/find/city?lat=" + format.format(lat) + "&lon=" + format.format(lon) + "&cnt=10";
			URL url = new URL(query.replace(" ", "%20"));
			Log.v(TAG, "Fetching weather from: '" + query + "'");
			BufferedReader irdr = new BufferedReader(new InputStreamReader(url.openStream()));
			
			final String TRIGGER_STRING = "\"temp\"";
			String line = null;
			while((line = irdr.readLine()) != null){
				//see if we can find an instance of "temp"
				int loc = line.indexOf(TRIGGER_STRING);
				loc += TRIGGER_STRING.length() + 1;
				String tempstr = line.substring(loc);
				tempstr = tempstr.substring(0, tempstr.indexOf(","));
				_retrieved_deg_c = (int)(Double.parseDouble(tempstr)) - 273; //reported temperature is in Kelvins
				break;
			}
			irdr.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return _retrieved_deg_c;
	}
	
	Location getLastLocation(){
		if(locmanager == null){
			Log.e(TAG, "null location manager");
			return null;
		}
		try{
			Location loc = locmanager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(loc == null){
				return locmanager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean temperatureRetrievalFailed(){
		return temp_failed;
	}
	
	boolean canUseGPS(){
		return locmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	Semaphore loc_changed_sem = new Semaphore(1);
	public void onLocationChanged(Location location) {
		if(location.getAccuracy() > MIN_ACCURACY_METERS){
			Log.v(TAG, "Got a location with vague accuracy: " + location.getAccuracy());
			return;
		}
		try{
			loc_changed_sem.acquire();
			if(!requesting_locs) return;
			Log.v(TAG, "Got a location from GPS: lat=" + location.getLatitude() + " lon=" + location.getLongitude());
			int temp = getTemperatureFromCoordinates(location.getLatitude(), location.getLongitude());
			if(temp == -1){
				temp_failed = true;
				templistener.temperatureChanged(ROOM_TEMPERATURE);
			}else{
				templistener.temperatureChanged(temp);
			}
			locmanager.removeUpdates(this);
			requesting_locs = false;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			loc_changed_sem.release();
		}
	}
	public void onProviderDisabled(String provider) { }
	public void onProviderEnabled(String provider) { }
	public void onStatusChanged(String provider, int status, Bundle extras) { }
}
