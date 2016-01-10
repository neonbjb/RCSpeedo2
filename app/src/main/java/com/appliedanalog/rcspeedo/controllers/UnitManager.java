/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.data.UnitType;

/**
 * Controller class that manages the app-wide unit settings for temperatures and speeds.
 */
public class UnitManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    final String TAG = "UnitManager";

    UnitType mCurrentTempType = TempTypes[0];
    UnitType mCurrentSpeedType = SpeedTypes[0];
    Context mContext = null;
    Strings mStrings;

    static UnitType[] TempTypes = new UnitType[]{
            new UnitType("Fahrenheit", "°F", (9. / 5.), 32),
            new UnitType("Celsius", "°C", 1., 0.)
    };
    static UnitType[] SpeedTypes = new UnitType[]{
            new UnitType("MPH", "MPH", 2.24, 0.),
            new UnitType("Km/h", "Km/h", 3.6, 0.),
            new UnitType("m/mStrings", "m/mStrings", 1., 0.)
    };

    private static UnitManager sInstance;

    /**
     * Fetch the singleton instance.
     * @return
     */
    public static UnitManager getInstance() {
        if (sInstance == null) {
            sInstance = new UnitManager();
        }
        return sInstance;
    }

    private UnitManager() {
    }

    /**
     * Should be called on application start-up to initialize this object with the proper settings.
     * @param aContext App context.
     */
    public void init(Context aContext) {
        mContext = aContext;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadFromPreferences(prefs);
        prefs.registerOnSharedPreferenceChangeListener(this);
        mStrings = Strings.getInstance();

        //re-initialize with localized strings
        TempTypes = new UnitType[]{
                new UnitType(mStrings.FL, "°" + mStrings.F, (9. / 5.), 32),
                new UnitType(mStrings.CL, "°" + mStrings.C, 1., 0.)
        };
        SpeedTypes = new UnitType[]{
                new UnitType(mStrings.MPH, mStrings.MPH, 2.24, 0.),
                new UnitType(mStrings.KPH, mStrings.KPH, 3.6, 0.),
                new UnitType(mStrings.MPS, mStrings.MPS, 1., 0.)
        };
    }

    /**
     * Fetches the UnitType instance mapped to the corresponding string.
     * @param aTempUnit
     * @return
     */
    public UnitType getTempUnit(String aTempUnit) {
        return TempTypes[getTempTypeIndex(aTempUnit)];
    }

    /**
     * Fetches the UnitType instance mapped to the corresponding string.
     * @param aSpeedUnit
     * @return
     */
    public UnitType getSpeedUnit(String aSpeedUnit) {
        return SpeedTypes[getSpeedTypeIndex(aSpeedUnit)];
    }

    /**
     * Sets the temperature unit.
     * @param aTempType
     */
    public void setTempType(String aTempType) {
        setTempType(getTempTypeIndex(aTempType));
    }

    /**
     * Sets the speed unit.
     * @param aSpeedType
     */
    public void setSpeedType(String aSpeedType) {
        setSpeedType(getSpeedTypeIndex(aSpeedType));
    }

    /**
     * Returns a human-readable temperature string for the given temperature.
     * @param aTemp
     * @return
     */
    public String getTemperature(double aTemp) {
        return mCurrentTempType.getNeat(aTemp);
    }

    /**
     * Returns just the specified temperature, converted to the locally specified units.
     * @param aTemp
     * @return
     */
    public double getRawTemperature(double aTemp) {
        return mCurrentTempType.doConversion(aTemp);
    }

    /**
     * Returns a human-readable speed string for the given speed.
     * @param aSpeed
     * @return
     */
    public String getDisplaySpeed(double aSpeed) {
        return mCurrentSpeedType.getNeat(aSpeed);
    }

    /**
     * Gets a speed string that can be spoken by TTS.
     * @param aSpeed
     * @return
     */
    public String getVocalSpeed(double aSpeed) {
        return mCurrentSpeedType.getVocal(aSpeed);
    }

    /**
     * Takes in temperatures with the current unit setting and
     * converts it back to the base unit (Celsius)
     *
     * @param aTemp
     * @return
     */
    public double getBaseTemp(double aTemp) {
        return mCurrentTempType.backConversion(aTemp);
    }

    // Event handlers.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences aSharedPrefs, String aKey) {
        if(aKey.equals(SettingsKeys.TEMP_UNIT_KEY) || aKey.equals(SettingsKeys.SPEED_UNIT_KEY)) {
            loadFromPreferences(aSharedPrefs);
        }
    }

    private void loadFromPreferences(SharedPreferences aPrefs) {
        int ttind, stind;
        String tname = aPrefs.getString(SettingsKeys.TEMP_UNIT_KEY, null);
        if (tname == null) {
            ttind = 0;
        } else {
            ttind = getTempTypeIndex(tname);
        }
        mCurrentTempType = TempTypes[ttind];
        tname = aPrefs.getString(SettingsKeys.SPEED_UNIT_KEY, null);
        if (tname == null) {
            stind = 0;
        } else {
            stind = getSpeedTypeIndex(tname);
        }
        if (stind == -1) {
            Log.e(TAG, "Error loading preferred unit type: " + tname);
            stind = 0;
        }
        mCurrentSpeedType = SpeedTypes[stind];

        //apply localization to the speed types
        Resources res = mContext.getResources();
        String[] tts = res.getStringArray(R.array.available_temp_units);
        for (int x = 0; x < tts.length; x++) {
            TempTypes[x].name = tts[x];
        }
        String[] sts = res.getStringArray(R.array.available_speed_units);
        String[] vsts = res.getStringArray(R.array.available_speed_units_vocal);
        for (int x = 0; x < sts.length; x++) {
            SpeedTypes[x].name = sts[x];
            SpeedTypes[x].vocal = vsts[x];
        }
        Log.v(TAG, "loadFromPrefs; speedUnit=" + mCurrentSpeedType.name + " tempUnit=" + mCurrentTempType.name);
    }

    private int getTempTypeIndex(String aName) {
        for (int x = 0; x < TempTypes.length; x++) {
            if (TempTypes[x].name.equals(aName)) {
                return x;
            }
        }
        return -1;
    }

    private int getSpeedTypeIndex(String aSpeedType) {
        for (int x = 0; x < SpeedTypes.length; x++) {
            if (SpeedTypes[x].name.equals(aSpeedType)) {
                return x;
            }
        }
        return -1;
    }

    private void setTempType(int aTempType) {
        mCurrentTempType = TempTypes[aTempType];
        Log.v(TAG, "SetTempType: " + mCurrentTempType.name);
        if (mContext != null) {
            SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            ed.putString(SettingsKeys.TEMP_UNIT_KEY, TempTypes[aTempType].name);
            ed.commit();
        }
    }

    private void setSpeedType(int aSpeedType) {
        mCurrentSpeedType = SpeedTypes[aSpeedType];
        if (mContext != null) {
            SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            ed.putString(SettingsKeys.SPEED_UNIT_KEY, SpeedTypes[aSpeedType].name);
            ed.commit();
        }
    }
}
