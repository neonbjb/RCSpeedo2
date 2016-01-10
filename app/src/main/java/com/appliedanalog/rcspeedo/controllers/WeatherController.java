/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Singleton controller class that manages setting the local temperature. This temperature is
 * used by the AudioDoppler class to generate more accurate speed readings.
 */
public class WeatherController implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    // Constants.
    static final String TAG = "WeatherController";

    static final String OPEN_WEATHER_API_KEY = "08e6a1cfce8119deb63723641566cf76";
    static final int ROOM_TEMPERATURE = 21; // Default temperature
    static final int MIN_ACCURACY_METERS = 100;

    // Types.

    /**
     * Listener type for registering for new temperature reading events.
     */
    public interface TemperatureListener {
        /**
         * Emitted when the temperature setting changes.
         * @param aNewTemp New temperature in degrees C.
         * @param aTemperatureWasAutomaticallySet Whether or not the temperature was derived automatically using the GPS location.
         */
        public void temperatureChanged(int aNewTemp, boolean aTemperatureWasAutomaticallySet);
    }

    // Properties.
    LocationManager mLocManager;
    boolean mWaitingOnLocation = false;
    ArrayList<TemperatureListener> mTemperatureListeners;
    int mLastTemperature = ROOM_TEMPERATURE;
    boolean mLastTemperatureDerivedAutomatically = false;

    // These properties correlate with settings values.
    boolean mAutomaticallyFetchTemperature = true;
    int mManualTemperature = ROOM_TEMPERATURE;

    static NumberFormat format;

    static {
        format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
    }

    private static WeatherController sInstance;

    /**
     * Returns the Singleton instance.
     * @return
     */
    public static WeatherController getInstance() {
        if (sInstance == null) {
            sInstance = new WeatherController();
        }
        return sInstance;
    }

    private WeatherController() {
        mTemperatureListeners = new ArrayList<>();
    }

    /**
     * Initializes this singleton. Should be called once on startup.
     *
     * @param aLocman Location manager to use to get GPS position updates when necessary.
     */
    public void init(LocationManager aLocman, SharedPreferences aPrefs) {
        mLocManager = aLocman;

        // This controller actually works semi-autonomously by binding to settings changes and updating
        // the temperature dynamically based on user settings.
        aPrefs.registerOnSharedPreferenceChangeListener(this);
        mAutomaticallyFetchTemperature = aPrefs.getBoolean(SettingsKeys.ENABLE_GPS_KEY, true);
        mManualTemperature = fetchManualTemperatureFromPrefs(aPrefs);
        refreshTemperature();
    }

    public synchronized void addListener(TemperatureListener aListener) {
        mTemperatureListeners.add(aListener);
    }

    /**
     * Can be used in lieu of the listener class to fetch the most recently available temperature reading
     * (or the manually set temperature, if applicable).
     * @return Temperature in degrees C.
     */
    public synchronized int getLastTemperature() {
        return mLastTemperature;
    }

    /**
     * Returns whether or not the temperature provided by getLastTemperature() was provided automatically
     * by a weather service polled using the user's location. If it returns false, the temperature
     * provided was manually set.
     * @return
     */
    public synchronized boolean isTemperatureDerivedFromLocation() {
        return mLastTemperatureDerivedAutomatically;
    }

    /**
     * This should be called to turn on the GPS, refresh the user's position, and grab a weather update
     * at that position. Since this app will only be used for short periods of time, generally this will
     * only be necessary at app start-up.
     */
    public void refreshTemperature() {
        if(!mAutomaticallyFetchTemperature) {
            Log.v(TAG, "refreshTemperature - auto temp disabled, using manual temp.");
            setTemperature(mManualTemperature, false);
        } else {
            Log.v(TAG, "refreshTemperature - automatically fetching temperature.");
            try {
                if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mWaitingOnLocation = true;
                    mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                } else {
                    Log.w(TAG, "Cannot use the GPS for some reason, unable to provide accurate temperature.");
                    setTemperature(mManualTemperature, false);
                }
            } catch (SecurityException e) {
                // Permission not given by user; manual temperature to be used instead.
                setTemperature(mManualTemperature, false);
            }
            // @todo - Add a timer mechanism to abort the location provider if it takes too long.
        }
    }

    /**
     * Uses the openweathermap API to obtain the temperature at the passed lat/lon.
     *
     * @param lat Latitude location in degrees.
     * @param lon Longitude location in degrees.
     */
    private void fetchTemperatureFromCoordinates(double lat, double lon) {
        boolean success = false;
        try {
            String query = "http://api.openweathermap.org/data/2.5/weather?lat=" + format.format(lat) + "&lon="
                    + format.format(lon) + "&appid=" + OPEN_WEATHER_API_KEY;
            URL url = new URL(query.replace(" ", "%20"));
            BufferedReader irdr = new BufferedReader(new InputStreamReader(url.openStream()));

            // @todo - Just use a proper JSON processor.

            final String TRIGGER_STRING = "\"temp\"";
            String line = null;
            while ((line = irdr.readLine()) != null) {
                //see if we can find an instance of "temp"
                int loc = line.indexOf(TRIGGER_STRING);
                loc += TRIGGER_STRING.length() + 1;
                String tempstr = line.substring(loc);
                tempstr = tempstr.substring(0, tempstr.indexOf(","));
                int retrievedTempC = (int) (Double.parseDouble(tempstr)) - 273; //reported temperature is in Kelvins
                success = true;
                setTemperature(retrievedTempC, true);
                break;
            }
            irdr.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(!success) {
                Log.v(TAG, "fetchTemperatureFromCoordinates - Failed to fetch temperature for some reason. Returning manual temp.");
                setTemperature(mManualTemperature, false);
            }
        }
    }

    private synchronized void setTemperature(int aTempC, boolean aTempIsAutomaticallySet) {
        Log.v(TAG, "Setting temperature to " + aTempC + " auto=" + aTempIsAutomaticallySet);

        // Only do stuff if this value has actually changed.
        if((mLastTemperature != aTempC) || (aTempIsAutomaticallySet != mLastTemperatureDerivedAutomatically)) {
            mLastTemperature = aTempC;
            mLastTemperatureDerivedAutomatically = aTempIsAutomaticallySet;

            for(TemperatureListener listener : mTemperatureListeners) {
                listener.temperatureChanged(mLastTemperature, mLastTemperatureDerivedAutomatically);
            }
        }
    }

    private int fetchManualTemperatureFromPrefs(SharedPreferences aPrefs) {
        int temp = ROOM_TEMPERATURE;
        // Old versions of the app stored this as a string.
        // @todo - Purge old invalid values and use new stuff.
        try{
            String manualTempPref = aPrefs.getString(SettingsKeys.HARD_TEMP_KEY, Integer.toString(ROOM_TEMPERATURE));
            Log.v(TAG, "Manual temperature: " + manualTempPref);
            temp = Integer.parseInt(manualTempPref);

            // The user enters the temperature setting using his/her native units. Convert that into degrees C.
            temp = (int)UnitManager.getInstance().getBaseTemp(temp);
        } catch(Exception e) {
            Log.e(TAG, "Error parsing manual temperature string from settings.");
        }
        return temp;
    }

    // Event handlers.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(SettingsKeys.ENABLE_GPS_KEY.equals(key)) {
            Log.v(TAG, "ENABLE_GPS_KEY changed - refreshing temperature.");
            mAutomaticallyFetchTemperature = sharedPreferences.getBoolean(SettingsKeys.ENABLE_GPS_KEY, true);
            refreshTemperature();
        } else if (SettingsKeys.HARD_TEMP_KEY.equals(key)) {
            mManualTemperature = fetchManualTemperatureFromPrefs(sharedPreferences);
            Log.v(TAG, "HARD_TEMP_KEY changed - refreshing temperature: " + mManualTemperature);
            refreshTemperature();
        }
    }

    public void onLocationChanged(final Location aLocation) {
        // Since locations appear on the main thread, on which network access is disabled, we'll need
        // to offload this work onto another thread.
        // @todo - Possibly use threadpools or similar.
        (new Thread("Temperature fetcher thread"){
            public void run() {
                if (aLocation.getAccuracy() > MIN_ACCURACY_METERS) {
                    Log.v(TAG, "Got a location with vague accuracy: " + aLocation.getAccuracy());
                    return;
                }
                try {
                    if (!mWaitingOnLocation) return;
                    mWaitingOnLocation = false;
                    mLocManager.removeUpdates(WeatherController.this);

                    Log.v(TAG, "Got a location from GPS: lat=" + aLocation.getLatitude() + " lon=" + aLocation.getLongitude());
                    fetchTemperatureFromCoordinates(aLocation.getLatitude(), aLocation.getLongitude());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
