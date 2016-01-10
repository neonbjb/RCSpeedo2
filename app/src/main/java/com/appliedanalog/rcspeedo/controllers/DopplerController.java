/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

import android.content.SharedPreferences;
import android.util.Log;

import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.doppler.AudioDoppler;
import com.appliedanalog.rcspeedo.doppler.AudioDopplerConfiguration;

import java.util.ArrayList;

/**
 * Singleton interface between the AudioDoppler class and the UI. Handles the binding of audio data from the
 * mic into the AudioDoppler when RCSpeedo is started and fires out speed events as they are
 * detected.
 */
public class DopplerController implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener {

    // Constants.
    static final String TAG = "DopplerController";
    static final int SAMPLING_RATE = 22050; // Audio sampling rate in Hz
    static final int FRAME_SIZE = 1024; // Number of audio samples per doppler frame.

    // Types.

    /**
     * Interface used to register to handle speed detection events.
     */
    public interface DopplerListener {
        /**
         * Called when the DopplerController starts or stops for any reason.
         * @param aIsActive Whether or not the DopplerController is active.
         */
        public void dopplerActiveStateChanged(boolean aIsActive);

        /**
         * Called when an error occurs in the DopplerController.
         * @param aError Textual description of the errors.
         */
        public void dopplerError(String aError);

        /**
         * Called when a new speed is detected.
         * @param aSpeedInMps Speed in meters/sec.
         */
        public void newSpeedDetected(DetectedSpeed aSpeedInMps);

        /**
         * Called when the highest detected speed changes.
         * @param aNewHighestSpeedMps New highest detected speed in meters/sec.
         */
        public void highestSpeedChanged(DetectedSpeed aNewHighestSpeedMps);

        /**
         * Called when a speed is removed from the active speed list managed by this controller.
         * @param aSpeed
         */
        public void speedInvalidated(DetectedSpeed aSpeed);

    }

    // Properties.
    private Thread mThread;
    private AudioDoppler mDoppler;
    private MicHandler mMicHandler;
    ArrayList<DopplerListener> mSpeedListeners;
    private boolean mIsActive;

    // Object-level synchronization specifically protects this member.
    ArrayList<DetectedSpeed> mSpeeds;
    DetectedSpeed mHighestSpeed;


    // Singleton access.
    private static DopplerController mInstance = new DopplerController();

    /**
     * Fetch the Singleton instance of the DopplerController class.
     * @return
     */
    public static DopplerController getInstance() {
        return mInstance;
    }

    /**
     * Constructor.
     */
    private DopplerController() {
        mDoppler = new AudioDoppler(AudioDopplerConfiguration.DEFAULT.scaleFrameSize(FRAME_SIZE), SAMPLING_RATE);
        mMicHandler = new MicHandler(FRAME_SIZE, SAMPLING_RATE);
        mSpeedListeners = new ArrayList<DopplerListener>();
        mIsActive = false;
        mSpeeds = new ArrayList<DetectedSpeed>();
        mHighestSpeed = new DetectedSpeed(0);
        mDoppler.setTemperature(WeatherController.getInstance().getLastTemperature());
    }

    /**
     * Initializes this singleton. Should be called once on startup.
     *
     * @param aPrefs Shared preferences object which tells this controller what mode it should run in.
     */
    public void init( SharedPreferences aPrefs) {
        aPrefs.registerOnSharedPreferenceChangeListener(this);
        // Trigger the event once to get the desired binding to start with.
        onSharedPreferenceChanged(aPrefs, SettingsKeys.DOPPLER_MODE_KEY);

        // Register with the weather controller for temperature updates.
        WeatherController.getInstance().addListener(new WeatherController.TemperatureListener() {
            @Override
            public void temperatureChanged(int aNewTemp, boolean aTemperatureWasAutomaticallySet) {
                Log.v(TAG, "Temperature set: " + aNewTemp);
                mDoppler.setTemperature(aNewTemp);
            }
        });
    }

    /**
     * Call to register as a listener to speed detection events.
     * @param aNewListener
     */
    public void addSpeedListener(DopplerListener aNewListener) {
        mSpeedListeners.add(aNewListener);
    }

    /**
     * Returns whether or not RCSpeedo is running.
     * @return
     */
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * Start RCSpeedo detection thread.
     */
    public void start() {
        if (mIsActive) {
            Log.v(TAG, "DopplerController already started.");
        }
        mThread = new Thread(this, "DopplerController Thread");
        mThread.start();
    }

    /**
     * Stop RCSpeedo detection thread and join with it. This call can block.
     */
    public void stop() {
        //@todo Implement
        mIsActive = false;
        try {
            mThread.interrupt();
            mThread.join();
            mThread = null;
        } catch (InterruptedException ie) {
        }
        mMicHandler.stopRecording();
    }

    /**
     * Should be called onDestroy to powerdown the controller and release any resources (e.g. Mic handle).
     */
    public void powerdown() {
        stop();
        mMicHandler.releaseRecorder();
    }

    /**
     * Call to change the doppler mode.
     * @param aMode
     */
    public void setDopplerMode(AudioDopplerConfiguration aMode) {
        // Lock in the frame size.. @todo - Add support for different sizes
        mDoppler.applyConfiguration(aMode.scaleFrameSize(FRAME_SIZE));
    }

    /**
     * Call to clear all of the speeds being stored in this controller.
     */
    public void clearSpeeds() {
        Log.v(TAG, "clearing speeds");
        while(!mSpeeds.isEmpty()) {
            DetectedSpeed spd = mSpeeds.remove(0);
            for (DopplerListener listener : mSpeedListeners) {
                listener.speedInvalidated(spd);
            }
        }

        // Highest speed was reset too.
        mHighestSpeed = new DetectedSpeed(0);
        for (DopplerListener listener : mSpeedListeners) {
            listener.highestSpeedChanged(mHighestSpeed);
        }
    }

    /**
     * Call to remove the specified speed from the active list.
     * @param aSpeed
     */
    public void removeSpeed(DetectedSpeed aSpeed) {
        if(mSpeeds.remove(aSpeed)) {
            for (DopplerListener listener : mSpeedListeners) {
                listener.speedInvalidated(aSpeed);
            }
        }

        if(aSpeed == mHighestSpeed) {
            DetectedSpeed mHighestSpeed = mSpeeds.get(0);
            // Find new highest speed.
            for(DetectedSpeed speed : mSpeeds) {
                if(speed.getSpeed() > mHighestSpeed.getSpeed()) {
                    mHighestSpeed = speed;
                }
            }

            for (DopplerListener listener : mSpeedListeners) {
                listener.highestSpeedChanged(mHighestSpeed);
            }
        }
    }

    /**
     * Retrieve the list of detected speeds since the controller was last cleared.
     * @return
     */
    public ArrayList<DetectedSpeed> getDetectedSpeeds() {
        return mSpeeds;
    }

    /**
     * Implements Runnable.run() - should not be called externally.
     */
    @Override
    public void run() {
        // Verify the mic will work.
        if (!mMicHandler.startRecording()) {
            Log.e(TAG, "Error initializing the Mic Handler");
            error(Strings.getInstance().ERR_NO_MIC);
            return;
        } else {
            Log.v(TAG, "Successfully acquired microphone.");
        }

        // Activate and notify.
        mIsActive = true;
        for (DopplerListener listener : mSpeedListeners) {
            listener.dopplerActiveStateChanged(mIsActive);
        }

        // Start up the main loop.
        final long SPEED_REPORT_INTERVAL = 500;
        long speedDetectedTime = 0;
        double bestSpeed = 0;
        double bestSpeedWeight = 0;
        Log.v(TAG, "Entering main DopplerController processing loop.");
        while (mIsActive) {
            mDoppler.audioToBuffer(mMicHandler.readFrame(), mMicHandler.getRotatingPointer());
            mDoppler.nextFrame();

            long currentTime = System.currentTimeMillis();
            if (mDoppler.numSpeeds() > 0) {
                // Once a speed is detected, SPEED_REPORT_INTERVAL is waited to see if there are any more
                // accurate speeds to use before reporting to UI.
                if(bestSpeed == 0) {
                    speedDetectedTime = currentTime;
                }
                for (int s = 0; s < mDoppler.numSpeeds(); s++) {
                    if (bestSpeedWeight < mDoppler.getSpeedWeight(s)) {
                        bestSpeed = mDoppler.getSpeed(s);
                        bestSpeedWeight = mDoppler.getSpeedWeight(s);
                    }
                }
            }

            // If the interval has passed, report the speed and reset the state variables.
            if (bestSpeed != 0 && (currentTime - speedDetectedTime) > SPEED_REPORT_INTERVAL) {
                newSpeedDetected(bestSpeed);
                bestSpeed = 0.;
                bestSpeedWeight = 0.;
            }

            try {
                // Don't poll continuously. @todo - It might be best to write a waiting mechanism into AudioDoppler.
                Thread.sleep(50);
            } catch (InterruptedException ie ) { }
        }

        mIsActive = false;
        for (DopplerListener listener : mSpeedListeners) {
            listener.dopplerActiveStateChanged(mIsActive);
        }
    }

    // Event handlers.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(SettingsKeys.DOPPLER_MODE_KEY.equals(key)) {
            String modeSelection = sharedPreferences.getString(SettingsKeys.DOPPLER_MODE_KEY, SettingsKeys.DOPPLER_MODE_DEFAULT);
            Log.v(TAG, "DOPPLER_MODE_KEY changed - changing mode: " + modeSelection);
            if (modeSelection.equals(SettingsKeys.DOPPLER_MODE_DEFAULT)) {
                setDopplerMode(AudioDopplerConfiguration.DEFAULT);
            } else if(modeSelection.equals(SettingsKeys.DOPPLER_MODE_FAST_PASS)) {
                setDopplerMode(AudioDopplerConfiguration.FAST_PASS);
            } else if(modeSelection.equals(SettingsKeys.DOPPLER_MODE_HI_SPEED)) {
                setDopplerMode(AudioDopplerConfiguration.CFG_200_PLUS);
            }
        }
    }

    private void error(String aError) {
        for(DopplerListener listener : mSpeedListeners) {
            listener.dopplerError(aError);
        }
    }

    private synchronized void newSpeedDetected(double aSpeedInMps) {
        Log.v(TAG, "New speed detected: " + aSpeedInMps);

        DetectedSpeed speed = new DetectedSpeed(aSpeedInMps);
        mSpeeds.add(speed);
        boolean newHighest = false;
        if(aSpeedInMps > mHighestSpeed.getSpeed()) {
            mHighestSpeed = speed;
            newHighest = true;
        }
        for(DopplerListener listener : mSpeedListeners) {
            listener.newSpeedDetected(speed);
            if(newHighest) {
                listener.highestSpeedChanged(mHighestSpeed);
            }
        }
    }

}
