package com.appliedanalog.rcspeedo.controllers;

import android.util.Log;

import com.appliedanalog.rcspeedo.doppler.AudioDoppler;
import com.appliedanalog.rcspeedo.doppler.AudioDopplerConfiguration;

import java.util.ArrayList;

/**
 * Singleton interface between the AudioDoppler class and the UI. Handles the binding of audio data from the
 * mic into the AudioDoppler when RCSpeedo is started and fires out speed events as they are
 * detected.
 */
public class DopplerController implements Runnable {

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
        public void newSpeedDetected(double aSpeedInMps);

        /**
         * Called when the highest detected speed changes.
         * @param aNewHighestSpeed New highest detected speed in meters/sec.
         */
        public void highestSpeedChanged(double aNewHighestSpeedMps);

    }

    // Properties.
    private Thread mThread;
    private AudioDoppler mDoppler;
    private MicHandler mMicHandler;
    ArrayList<DopplerListener> mSpeedListeners;
    private boolean mIsActive;

    ArrayList<Double> mSpeeds;
    double mHighestSpeed;


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
        mSpeeds = new ArrayList<Double>();
        mHighestSpeed = 0;
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
    }

    /**
     * Call to change the doppler mode.
     * @param aMode
     */
    public void setDopplerMode(AudioDopplerConfiguration aMode) {
        mDoppler.applyConfiguration(aMode.scaleFrameSize(FRAME_SIZE));
    }

    /**
     * Implements Runnable.run() - should not be called externally.
     */
    @Override
    public void run() {
        // Verify the mic will work.
        if (!mMicHandler.startRecording()) {
            error(Strings.getInstance().ERR_NO_MIC);
            return;
        }

        // Activate and notify.
        mIsActive = true;
        for (DopplerListener listener : mSpeedListeners) {
            listener.dopplerActiveStateChanged(mIsActive);
        }

        // Start up the main loop.
        final long SPEED_REPORT_INTERVAL = 500;
        long lastReportedSpeedTime = 0;
        double bestSpeed = 0;
        double bestSpeedWeight = 0;
        while (mIsActive) {
            mDoppler.audioToBuffer(mMicHandler.readFrame(), mMicHandler.getRotatingPointer());
            mDoppler.nextFrame();

            if (mDoppler.numSpeeds() > 0) {
                // Only report a new speed every SPEED_REPORT_INTERVAL ms, to prevent reporting the same pass multiple times.
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastReportedSpeedTime) > SPEED_REPORT_INTERVAL) {
                    bestSpeed = 0.;
                    bestSpeedWeight = 0.;
                    newSpeedDetected(bestSpeed);
                    lastReportedSpeedTime = currentTime;
                }
                for (int s = 0; s < mDoppler.numSpeeds(); s++) {
                    if (bestSpeedWeight < mDoppler.getSpeedWeight(s)) {
                        bestSpeedWeight = mDoppler.getSpeedWeight(s);
                        bestSpeed = mDoppler.getSpeed(s);
                    }
                }
            }
            try {
                // Don't poll continuously. @todo - It might be best to write a waiting mechanism into AudioDoppler.
                Thread.sleep(50);
            } catch (InterruptedException ie ) { }
        }
    }

    private void error(String aError) {
        for(DopplerListener listener : mSpeedListeners) {
            listener.dopplerError(aError);
        }
    }

    private void newSpeedDetected(double aSpeedInMps) {
        boolean newHighest = false;
        if(aSpeedInMps > mHighestSpeed) {
            mHighestSpeed = aSpeedInMps;
            newHighest = true;
        }
        for(DopplerListener listener : mSpeedListeners) {
            listener.newSpeedDetected(aSpeedInMps);
            if(newHighest) {
                listener.highestSpeedChanged(mHighestSpeed);
            }
        }
    }

}
