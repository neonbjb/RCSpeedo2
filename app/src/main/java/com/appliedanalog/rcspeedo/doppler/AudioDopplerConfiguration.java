/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

import android.util.Log;

/**
 * Class that holds configuration information for the AudioDoppler processor.
 */
public class AudioDopplerConfiguration {
    final String TAG = "ADConfiguration";

    private static final double[] EXPANSION_RATIOS = { .4, .5, .64, .8, .93,
            1., //mid
            1.15, 1.31, 1.48, 1.64, 1.79, 1.9, 2.1, 2.27, 2.36,
            2.5, 2.6, 2.7, 2.8 };

    // Statically created configurations to be used
    /**
     * Configuration that should be used normally.
     */
    public static final AudioDopplerConfiguration DEFAULT = new AudioDopplerConfiguration(500.f, 10000.f, 15, 3, 15, 55, 250.f, 20.f, 10, 10);
    /**
     * Configuration that should be used when the passes made by the phone are unusually brief (e.g. an eliptical arc is flown with the phone at the tip of one end of the arc)
     */
    public static final AudioDopplerConfiguration FAST_PASS = new AudioDopplerConfiguration(500.f, 10000.f, 15, 3, 5, 55, 250.f, 20.f, 10, 10);
    /**
     * Configuration that should be used to detect extremely fast aircraft, 200MPH+
     */
    public static final AudioDopplerConfiguration CFG_200_PLUS = new AudioDopplerConfiguration(500.f, 10000.f,
            new int[]{ 30, 30, 30, //500-2000 PEAK WIDTH
                    30, 30, 30, 30, //2000-4000
                    34, 38, 40, 43, //4000-6000
                    47, 52, 56, 60, //6000-8000
                    62, 63, 64, 65}, //8000-10000
            new int[]{ 3, 3, 3, //500-2000 FAMILY
                    4, 4, 4, 4, //2000-4000
                    7, 7, 7, 7, //4000-6000
                    8, 8, 8, 9, //6000-8000
                    10, 10, 10, 11}, //8000-10000
            10, 55, 250.f, 100.f,
            new int[]{ 16, 18, 20, //500-2000 FREQ SEPARATION
                    22, 24, 26, 28, //2000-4000
                    30, 32, 34, 36, //4000-6000
                    38, 40, 42, 44, //6000-8000
                    46, 48, 50, 52}, //8000-10000
            10);

    private float mFreqMin;
    private float mFreqMax;
    private int[] mPeakWidth;
    private int[] mFamilyBoundary;
    private int mMinTrendCountUntilCertified;
    private int mMaxDopplerWindow;
    private float mMaxSpeed;
    private float mMinSpeed;
    private int mDivisions;
    private int[] mMaxFreqSeparation;
    private int mMinTillAccept;
    private int mFrameSize = 1024; //default starting point
    private int mSamplesPerFrame = 1024; //default starting point

    /**
     * Constructs an AudioDopplerConfiguration with the specified parameters.
     * @note The parameter descriptions below are pretty sparse. See the getters for a more verbose description.
     * @param aFreqMin Minimum frequency to use in detection.
     * @param aFreqMax Maximum frequency to use in detection.
     * @param aPeakWidth Minimum peak-to-peak width to accept, expressed in terms of Fourier frames.
     * @param aFamilyBoundary The number of adjacent Fourier frames that constitutes a "family".
     * @param aMinTrendCount The minimum times a family must appear on a peak list before they constitute a "trend".
     * @param aMaxWindow Maximum number of frames that are waited until a trend is dropped.
     * @param aMaxSpeed Maximum detected speed accepted.
     * @param aMinSpeed Minimum detected speed accepted.
     * @param aFreqSep Maximum frequency separation from frame to frame that is allowed in order for a trend to "follow" the frequencies.
     * @param aMinTillAccept How many frames the algorithm must wait after a speed has been recognized to accept it as a speed.
     */
    public AudioDopplerConfiguration(float aFreqMin, float aFreqMax, int[] aPeakWidth, int[] aFamilyBoundary,
            int aMinTrendCount, int aMaxWindow, float aMaxSpeed, float aMinSpeed, int[] aFreqSeparation, int aMinTillAccept){
        mFreqMin = aFreqMin;
        mFreqMax = aFreqMax;
        mPeakWidth = aPeakWidth;
        mFamilyBoundary = aFamilyBoundary;
        mMinTrendCountUntilCertified = aMinTrendCount;
        mMaxDopplerWindow = aMaxWindow;
        mMaxSpeed = aMaxSpeed;
        mMinSpeed = aMinSpeed;
        mDivisions = aPeakWidth.length;
        mMaxFreqSeparation = aFreqSeparation;
        mMinTillAccept = aMinTillAccept;
        if(aPeakWidth.length != mDivisions || aFamilyBoundary.length != mDivisions){
            Log.v(TAG, "Critical error in AudioDopplerConfiguration: mDivisions and array length mismatch");
        }
    }

    /**
     * Constructs an AudioDopplerConfiguration with specified parameters. This variant uses the standard expansion ratios
     * to generate array values for aPeakWidth, aFamilyBoundary and aFreqSeparation.
     * @note The parameter descriptions below are pretty sparse. See the getters for a more verbose description.
     * @param aFreqMin Minimum frequency to use in detection.
     * @param aFreqMax Maximum frequency to use in detection.
     * @param aPeakWidth Minimum peak-to-peak width to accept, expressed in terms of Fourier frames.
     * @param aFamilyBoundary The number of adjacent Fourier frames that constitutes a "family".
     * @param aMinTrendCount The minimum times a family must appear on a peak list before they constitute a "trend".
     * @param aMaxWindow Maximum number of frames that are waited until a trend is dropped.
     * @param aMaxSpeed Maximum detected speed accepted.
     * @param aMinSpeed Minimum detected speed accepted.
     * @param aFreqSep Maximum frequency separation from frame to frame that is allowed in order for a trend to "follow" the frequencies.
     * @param aMinTillAccept How many frames the algorithm must wait after a speed has been recognized to accept it as a speed.
     */
    public AudioDopplerConfiguration(float aFreqMin, float aFreqMax, int aPeakWidth, int aFamilyBoundary,
            int aMinTrendCount, int aMaxWindow, float aMaxSpeed, float aMinSpeed, int aFreqSep, int aMinTillAccept){
        mFreqMin = aFreqMin;
        mFreqMax = aFreqMax;
        mMinTrendCountUntilCertified = aMinTrendCount;
        mMaxDopplerWindow = aMaxWindow;
        mMaxSpeed = aMaxSpeed;
        mMinSpeed = aMinSpeed;
        mDivisions = (int)(mFreqMax - mFreqMin) / 500;
        mPeakWidth = expand(aPeakWidth);
        mFamilyBoundary = expand(aFamilyBoundary);
        mMaxFreqSeparation = expand(aFreqSep);
        mMinTillAccept = aMinTillAccept;
    }

    /**
     * Scales configuration parameters affected by framesize accordingly this should be called
     * anytime you intend to move the framesize above or below 1024
     * @param factor
     */
    public AudioDopplerConfiguration scaleFrameSize(int aFs){
        double factor = (double)aFs / (double) mFrameSize;
        mFrameSize = aFs;
        for(int x = 0; x < mPeakWidth.length; x++){
            mPeakWidth[x] *= factor;
        }
        for(int x = 0; x < mFamilyBoundary.length; x++){
            mFamilyBoundary[x] *= factor;
        }
        for(int x = 0; x < mMaxFreqSeparation.length; x++){
            mMaxFreqSeparation[x] *= factor;
        }
        return this;
    }

    /**
     * This should be called anytime you intend to feed less than or greather than 1024 new audio
     * samples per audio doppler frame. Typically what is done in this case is only the oldest n
     * samples are deleted from the buffer and replaced with new samples.
     * @param aSampsPerFrame
     * @return
     */
    public AudioDopplerConfiguration scaleSamplesPerFrame(int aSampsPerFrame){
        double factor = (double)aSampsPerFrame / (double) mSamplesPerFrame;
        mSamplesPerFrame = aSampsPerFrame;
        mMaxDopplerWindow *= factor;
        return this;
    }

    /**
     * All frequencies below this thresholds are ignored (they are inaccurately acquired by sound hardware,
     * or, in the case of the lower, too inaccurate for calculating doppler shifts)
     */
    public float getFreqMin() {
        return mFreqMin;
    }

    /**
     * All frequencies above this thresholds are ignored (they are inaccurately acquired by sound hardware,
     * or, in the case of the lower, too inaccurate for calculating doppler shifts)
     */
    public float getFreqMax() {
        return mFreqMax;
    }

    /**
     * This defines the amount of data values to the left and right of the peak that must be less than the peak for it to be considered a peak.
     * @return
     */
    public int[] getPeakWidth() {
        return mPeakWidth;
    }

    /**
     * A data point that is trending will continue trending as long as itself or datapoints within
     * the FAMILY distance are trending. If "family" data points are used to continue the trend,
     * they shift the index towards them to record their contribution.
     */
    public int[] getFamilyBoundary() {
        return mFamilyBoundary;
    }

    /**
     * This defines the minimum amount of trend.count that must be recorded before
     * a trend is considered "certified" and is placed in the mMissingTrends list
     * upon disappearance
     */
    public int getMinTrendCountUntilCertified() {
        return mMinTrendCountUntilCertified;
    }

    /**
     * This is the maximum number of fourier frames for which a "missing" trend
     * persists before it is wiped. Note that it's related to the above constant
     * in that missing mTrends will be compared to certified mTrends, so you must wait
     * the desired window PLUS the amount of time it would take for a new trend to emerge.
     */
    public int getMaxDopplerWindow() {
        return mMaxDopplerWindow;
    }

    /**
     * This is the maximum speed that will be returned by findSpeeds() in m/s.
     */
    public float getMaxSpeed() {
        return mMaxSpeed;
    }

    /**
     * This is the minimum speed that will be returned by findSpeeds() in m/s.
     */
    public float getMinSpeed() {
        return mMinSpeed;
    }

    /**
     * This should be related to mFreqMin and mFreqMax: its the number of mDivisions there are in later parameters
     * this is done to individually adjust the parameters across the frequency spectrum.
     */
    public int getDivisions() {
        return mDivisions;
    }

    /**
     * The mMaxFreqSeparation vector determines the maximum amount of frequency spread between each frame is allowed
     *          in doppler shift detection. Higher values will aid in detecting faster speeds but will also generate more erroneous
     *          readings.
     */
    public int[] getMaxFreqSeparation() {
        return mMaxFreqSeparation;
    }

    /**
     * The mMinTillAccept variable determines how long a doppler shift has to "bottom out" before it is considered "done shifting" and the speed is calculated.
     */
    public int getMinTillAccept() {
        return mMinTillAccept;
    }

    /**
     * The Fourier frame size.
     * @return
     */
    public int getFrameSize() {
        return mFrameSize;
    }

    /**
     * The number of new samples in each frame. If this is < the frame size, samples are re-used from
     * the last frame.
     * @return
     */
    public int getSamplesPerFrame() {
        return mSamplesPerFrame;
    }

    // Some of the configuration properties for the AudioDoppler use arrays of configuration variables
    // so that different parts of the Fourier spectrum get treated differently (as the frequency spread
    // changes as you go up and down the spectrum). This method allows a constant expansion of a single
    // setting to easy configuration setting.
    private int[] expand(int aIn){
        int[] ret = new int[mDivisions];
        if(EXPANSION_RATIOS.length != mDivisions){
            Log.v(TAG, "Flattening frequency parameter curves because the configuration is out of spec.");
        }
        for(int x = 0; x < ret.length; x++){
            if(EXPANSION_RATIOS.length == mDivisions){
                ret[x] = (int)(EXPANSION_RATIOS[x] * (double)aIn);
                if(ret[x] == 0){
                    ret[x] = 1; //these parameters should never be 0..
                }
            }else{
                ret[x] = aIn;
            }
        }
        return ret;
    }
}
