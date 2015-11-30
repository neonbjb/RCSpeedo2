package com.appliedanalog.rcspeedo.controllers.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.fragments.Settings;

public class UnitManager {
	final String TAG = "UnitManager";
	
	private static UnitManager inst;
	
	public static UnitManager getInstance(){
		if(inst == null){
			inst = new UnitManager();
		}
		return inst;
	}
	
	private UnitManager(){}
	
	static UnitType[] TempTypes = new UnitType[]{
		new UnitType("Fahrenheit", "°F", (9./5.), 32),
		new UnitType("Celsius", "°C", 1., 0.)
	};
	static UnitType[] SpeedTypes = new UnitType[]{
		new UnitType("MPH", "MPH", 2.24, 0.),
		new UnitType("Km/h", "Km/h", 3.6, 0.),
		new UnitType("m/s", "m/s", 1., 0.)
	};
	
	UnitType currentTempType = TempTypes[0];
	UnitType currentSpeedType = SpeedTypes[0];
	Context context = null;
	Strings s;
	
	public void init(Context c){
		context = c;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		loadFromPreferences(prefs);
		s = Strings.getInstance();
		
		//re-initialize with localized strings
		TempTypes = new UnitType[]{
			new UnitType(s.FL, "°" + s.F, (9./5.), 32),
			new UnitType(s.CL, "°" + s.C, 1., 0.)
		};
		SpeedTypes = new UnitType[]{
			new UnitType(s.MPH, s.MPH, 2.24, 0.),
			new UnitType(s.KPH, s.KPH, 3.6, 0.),
			new UnitType(s.MPS, s.MPS, 1., 0.)
		};
	}
	
	public void loadFromPreferences(SharedPreferences prefs){
		int ttind, stind;
		String tname = prefs.getString(Settings.TEMP_UNIT_KEY, null);
		if(tname == null){
			ttind = 0;
		}else{
			ttind = getTempTypeIndex(tname);
		}
		currentTempType = TempTypes[ttind];
		tname = prefs.getString(Settings.SPEED_UNIT_KEY, null);
		if(tname == null){
			stind = 0;
		}else{
			stind = getSpeedTypeIndex(tname);
		}
		if(stind == -1){
			Log.e(TAG, "Error loading preferred unit type: " + tname);
			stind = 0;
		}
		currentSpeedType = SpeedTypes[stind];
		
		//apply localization to the speed types
		Resources res = context.getResources();
		String[] tts = res.getStringArray(R.array.available_temp_units);
		for(int x = 0; x < tts.length; x++){
			TempTypes[x].name = tts[x];
		}
		String[] sts = res.getStringArray(R.array.available_speed_units);
		String[] vsts = res.getStringArray(R.array.available_speed_units_vocal);
		for(int x = 0; x < sts.length; x++){
			SpeedTypes[x].name = sts[x];
			SpeedTypes[x].vocal = vsts[x];
		}
		Log.v(TAG, "loadFromPrefs; speedUnit=" + currentSpeedType.name + " tempUnit=" + currentTempType.name);
	}
	
	private int getTempTypeIndex(String name){
		for(int x = 0; x < TempTypes.length; x++){
			if(TempTypes[x].name.equals(name)){
				return x;
			}
		}
		return -1;
	}
	
	public UnitType getTempUnit(String name){
		return TempTypes[getTempTypeIndex(name)];
	}
	
	private int getSpeedTypeIndex(String name){
		for(int x = 0; x < SpeedTypes.length; x++){
			if(SpeedTypes[x].name.equals(name)){
				return x;
			}
		}
		return -1;
	}
	
	public UnitType getSpeedUnit(String name){
		return SpeedTypes[getSpeedTypeIndex(name)];
	}
	
	public void setTempType(String name){
		setTempType(getTempTypeIndex(name));
	}
	
	public void setTempType(int ttind){
		currentTempType = TempTypes[ttind];
		Log.v(TAG, "SetTempType: " + currentTempType.name);
		if(context != null){
			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
			ed.putString(Settings.TEMP_UNIT_KEY, TempTypes[ttind].name);
			ed.commit();
		}
	}
	
	public void setSpeedType(String name){
		setSpeedType(getSpeedTypeIndex(name));
	}
	
	public void setSpeedType(int stind){
		currentSpeedType = SpeedTypes[stind];
		if(context != null){
			SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
			ed.putString(Settings.SPEED_UNIT_KEY, SpeedTypes[stind].name);
			ed.commit();
		}
	}
	
	public String getTemperature(double in){
		return currentTempType.getNeat(in);
	}
	
	public double getRawTemperature(double in){
		return currentTempType.doConversion(in);
	}
	
	public String getDisplaySpeed(double in){
		return currentSpeedType.getNeat(in);
	}
	
	public String getVocalSpeed(double in){
		return currentSpeedType.getVocal(in);
	}
	
	/**
	 * Takes in temperatures with the current unit setting and
	 * converts it back to the base unit (Celsius)
	 * @param in
	 * @return
	 */
	public double getBaseTemp(double in){
		return currentTempType.backConversion(in);
	}
}
