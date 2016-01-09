/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

import java.util.ArrayList;

import android.util.Log;

/**
 * This class contains the primary logic behind the app - converting a PCM audio stream into a list
 * of speeds.
 */
public class AudioDoppler {
    final String TAG = "AudioDoppler";
    final boolean DEBUG = true;

    FFT mFft;
    int mSamplingFreq;
    float[] mDist;
    int mFrameCount = 0;
    double mTemperature;
    AudioDopplerConfiguration mConfig;

    // First pass properties - For finding frequency amplitude peaks.
    // By the nature of peaks, there cannot be more than one peak for every mConfig.getPeakWidth() * 2 data values, and the frequency distribution is inherently less than mDist.length / 2
    int[] mPeaks;
    int mNumPeaks = 0;

    // Second pass properties - Finding trends in the peaked frequencies.
    final int WOUNDED_LIFE_EXPECTANCY = 3;
    Trend[] mTrends = null;
    ArrayList<MissingTrend> mWoundedTrends = new ArrayList<MissingTrend>(); //this is a list of trends that retains a short lifespan before 'graduating' to a full on missing trend.
    ArrayList<MissingTrend> mMissingTrends = new ArrayList<MissingTrend>();

    // Third pass properties - Finding speeds from trends that exhibit a doppler shift.
    static final double MIN_SPEED_WEIGHT = .125;
    double[] mCalculatedSpeeds;
    //this is the source frequency from which the above speeds were calculated. it is used to scale the calculated speed to favor speeds
    //derived from higher frequencies.
    int[] mCalculatedSpeedsSource;
    int mNumSpeeds = 0;

    /**
     * Constructs an AudioDoppler object.
     * @param aConfig
     * @param aSamplingFreq
     */
    public AudioDoppler(AudioDopplerConfiguration aConfig, int aSamplingFreq) {
        mConfig = aConfig;
        mSamplingFreq = aSamplingFreq;
        mTemperature = 27; // 80F - A good middle of the line guess for when temperature is not explicitly set.

        initArrays();
    }

    /**
     * Change the configuration construct this AudioDoppler is using to detect speeds.
     * @param config
     */
    public void applyConfiguration(AudioDopplerConfiguration config) {
        mConfig = config;
        if (mDist != null && mDist.length != mConfig.getFrameSize()) {
            initArrays();
        }
    }

    /**
     * Transfers the specified audio data to the internal buffer.
     * @param aIn
     */
    public void audioToBuffer(short[] aIn) {
        int skip = aIn.length / mDist.length;
        for (int x = 0, y = 0; x < aIn.length; x += skip, y++) {
            mDist[y] = aIn[x];
        }
    }

    /**
     * This variation of audioToBuffer allows a rotating audio buffer to be passed
     * into the audiodoppler, the "starting" index of the rotating buffer is passed
     * in as aStartLoc.
     * @param aIn
     * @param aStartLoc
     */
    public void audioToBuffer(short[] aIn, int aStartLoc) {
        int skip = aIn.length / mDist.length;
        int y = 0;
        for (int x = aStartLoc; x < aIn.length; x += skip, y++) {
            mDist[y] = aIn[x];
        }
        for (int x = 0; x < aStartLoc; x += skip, y++) {
            mDist[y] = aIn[x];
        }
    }

    /**
     * Advances to the next FFT frame.
     *
     * @param dist
     */
    public void nextFrame() {
        if (mDist.length != mConfig.getFrameSize()) {
            debug("Invalid frame size passed to nextFrame()");
            return;
        }

        mFrameCount++;
        mFft.forward(mDist);
        findPeaks(mFft.getSpectrum(), mFft.freqToIndex(mConfig.getFreqMin()), mFft.freqToIndex(mConfig.getFreqMax()));
        updateTrends(mFft.getSpectrum(), mNumPeaks);
        calculateSpeeds();
    }

    public void setTemperature(double aTemperature) {
        Log.v(TAG, "AudioDoppler.setmTemperature(" + aTemperature + ")");
        mTemperature = aTemperature;
    }

    /**
     * this function is responsible for generating a "weighting" system for frequencies with detected speeds.
     * the weighting will apply mConfig.getMinSpeed()_WEIGHT to FREQ_LOW, and a weight of 1.0 to FREQ_HIGH, with a continuous
     * linear weight in between
     *
     * @param aIndex The location in the speeds array to weight
     * @return
     */
    public double getSpeedWeight(int aIndex) {
        return ((1 - MIN_SPEED_WEIGHT) * ((mCalculatedSpeedsSource[aIndex] - mConfig.getFreqMin()) / (mConfig.getFreqMax() - mConfig.getFreqMin()))) + MIN_SPEED_WEIGHT;
    }

    /**
     * Fetch the stored detected speed at the specified index.
     * @param aIndex
     * @return
     */
    public double getSpeed(int aIndex) {
        return mCalculatedSpeeds[aIndex];
    }

    /**
     * Fetch the number of available detected speeds for this frame.
     * @return
     */
    public int numSpeeds() {
        return mNumSpeeds;
    }


    private double getSpeed(double aFreq1, double aFreq2, double aTemperature, double aHumidity) {
        double sos = (331 + .606 * aTemperature); //current mTemperature is in Centigrade, this returns m/s
        return aFreq1 / ((aFreq1 + aFreq2) / 2) * sos - sos;
    }

    /**
     * This function returns the frequency as adjusted by the ratios of the passed index 'aIn'.
     *
     * @param aIn
     * @return
     */
    private double fuzzyFreq(double aIn) {
        double ciel_freq = mFft.indexToFreq((int) Math.ceil(aIn));
        int floor = (int) Math.floor(aIn);
        double flor_freq = mFft.indexToFreq(floor);
        double ratio = aIn - floor;
        return (ciel_freq * ratio + flor_freq * (1. - ratio));
    }


    private void initArrays() {
        mFft = new FFT(mConfig.getFrameSize(), 22500);
        mDist = new float[mConfig.getFrameSize()];
        mPeaks = new int[mConfig.getFrameSize() / 4];
        mTrends = new Trend[mConfig.getFrameSize() / 2];
        for (int x = 0; x < mTrends.length; x++) {
            mTrends[x] = new Trend();
            mTrends[x].index = x;
        }
        mCalculatedSpeeds = new double[mConfig.getFrameSize() / 4];
        mCalculatedSpeedsSource = new int[mConfig.getFrameSize() / 4];
    }

    private void findPeaks(float[] aSpec, int aStart, int aEnd) {
        int p_point = 0;
        float pros_peak = 0.f;
        for (int x = aStart + mConfig.getPeakWidth()[0]; x < aEnd - mConfig.getPeakWidth()[mConfig.getPeakWidth().length - 1]; x++) {
            pros_peak = aSpec[x];
            //check the values around this prospective peak, continue if they are not smaller
            boolean peak_found = true;
            int cond = mConfig.getPeakWidth()[mConfig.getDivisions() * (x - aStart) / aEnd];
            for (int i = 1; peak_found && i <= cond; i++) {
                if (aSpec[x + i] >= pros_peak || aSpec[x - i] >= pros_peak) {
                    peak_found = false;
                }
            }
            if (peak_found) {
                mPeaks[p_point] = x;
                p_point++;
            }
        }
        mNumPeaks = p_point;
    }

    private void updateTrends(float[] aSpec, int aNumPeaks) {
        //clean out the mMissingTrends array
        for (int x = 0; x < mMissingTrends.size(); x++) {
            mMissingTrends.get(x).count--;
            if (mMissingTrends.get(x).count <= 0) {
                mMissingTrends.remove(x);
                x--;
            }
        }

        //clear out any wounded trends that have truly died
        for (int x = 0; x < mWoundedTrends.size(); x++) {
            MissingTrend mt = mWoundedTrends.get(x);
            if (mt.count <= 0) {
                if (mt.oldCount >= mConfig.getMinTrendCountUntilCertified()) {
                    debug("[" + mFrameCount + "]: Trend at " + (mt.origIndex - mConfig.getFamilyBoundary()[mConfig.getDivisions() * mt.origIndex / mTrends.length]) + " lost!");
                    mt.count = mConfig.getMaxDopplerWindow();
                    mMissingTrends.add(mt);
                }
                mWoundedTrends.remove(x);
                x--;
            } else {
                mt.count--;
            }
        }

        //this variable points to the current 'peak' being referenced
        int c_peak_ptr = 0;
        //initialize the first FAMILY data points as 'untouched'
        for (int x = 0; x < mConfig.getFamilyBoundary()[0]; x++) {
            mTrends[x].touched = false;
        }
        int next_trend_reset = mConfig.getFamilyBoundary()[0];
        for (int x = 0; x < mTrends.length; x++) {
            int div = mConfig.getDivisions() * x / mTrends.length;
            //initialize the "new guys" in the current family as 'untouched'
            for (; next_trend_reset < mTrends.length && next_trend_reset <= x + mConfig.getFamilyBoundary()[div]; next_trend_reset++) {
                mTrends[next_trend_reset].touched = false;
            }
            //did the guy that just left the current family stay untouched?
            if (x >= mConfig.getFamilyBoundary()[div] && !mTrends[x - mConfig.getFamilyBoundary()[div]].touched) {
                //does the trend have enough count to be certified and saved as 'wounded'?
                Trend tsel = mTrends[x - mConfig.getFamilyBoundary()[div]];
                if (tsel.count >= (mConfig.getMinTrendCountUntilCertified() / 2)) {
                    mWoundedTrends.add(new MissingTrend(tsel, x - mConfig.getFamilyBoundary()[div], WOUNDED_LIFE_EXPECTANCY));
                }
                //reset the index
                tsel.reset(x - mConfig.getFamilyBoundary()[div]);
            }
            //is the current pointer also a peak pointer?
            if (c_peak_ptr < aNumPeaks && x == mPeaks[c_peak_ptr]) {
                //see if there is a wounded trend we can revive
                for (int i = 0; i < mWoundedTrends.size(); i++) {
                    MissingTrend mt = mWoundedTrends.get(i);
                    if (Math.abs(mt.origIndex - x) < mConfig.getFamilyBoundary()[div]) {
                        //revive the trend
                        mTrends[mt.origIndex].count = mConfig.getMinTrendCountUntilCertified() + WOUNDED_LIFE_EXPECTANCY - mt.count;
                        mWoundedTrends.remove(i);

                        //also, remove any trending info building up elsewhere in the family due to this guy's absense
                        for (int f = 1; f < mConfig.getFamilyBoundary()[div]; f++) {
                            mTrends[mt.origIndex + f].reset(mt.origIndex + f);
                            mTrends[mt.origIndex - f].reset(mt.origIndex - f);
                        }
                        break;
                    }
                }

                //find the highest trend within the immediate family
                int highest_trend = mTrends[x].count;
                int highest_trend_index = x;
                for (int f = 0; f < mConfig.getFamilyBoundary()[div]; f++) {
                    if (x - f >= 0 && mTrends[x - f].count > highest_trend) {
                        highest_trend_index = x - f;
                        highest_trend = mTrends[highest_trend_index].count;
                    }
                    if (x + f < mTrends.length && mTrends[x + f].count > highest_trend) {
                        highest_trend_index = x + f;
                        highest_trend = mTrends[highest_trend_index].count;
                    }
                }
                //ignore it if there are two trends in one family.
                if (mTrends[highest_trend_index].touched) {
                    debug("Merged trend at " + highest_trend_index);
                    continue;
                }
                //apply this new trend to the family member
                mTrends[highest_trend_index].index = (mTrends[highest_trend_index].index * (float) mTrends[highest_trend_index].count + (float) x) /
                        (mTrends[highest_trend_index].count + 1); //this is the current avg plus the new mark.
                mTrends[highest_trend_index].count++;
                mTrends[highest_trend_index].sig = aSpec[highest_trend_index];
                //mark the family member that received the trend
                mTrends[highest_trend_index].touched = true;

                if (mTrends[highest_trend_index].count == mConfig.getMinTrendCountUntilCertified()) {
                    debug("[" + mFrameCount + "]: Trend at " + highest_trend_index + " peaked!");
                }

                //increment the peak pointer
                c_peak_ptr++;
            }
        }
    }

    final int MAX_MISSING_FREQS_ACCEPTED = 3;

    private void calculateSpeeds() {
        mNumSpeeds = 0;
        //for each missing trend, we need to find new "interests", or see if the current
        //"interest" is accruing trend counts.
        for (int x = 0; x < mMissingTrends.size(); x++) {
            MissingTrend trend = mMissingTrends.get(x);
            if (trend.timeSinceLastInterest >= MAX_MISSING_FREQS_ACCEPTED) {
                mMissingTrends.remove(x);
                x--;
                debug("missing trend died prematurely!");
                continue;
            }

            //find the latest closest frequency that is trending
            int i_limit = trend.currentInterest - mConfig.getMaxFreqSeparation()[mConfig.getDivisions() * trend.currentInterest / mTrends.length];
            if (i_limit < 0) i_limit = 0;
            for (int i = trend.currentInterest; i >= i_limit; i--) {
                if (mTrends[i].count > 0) {
                    //found a trending frequency nearby! is it in the family?
                    if (trend.origIndex - i < mConfig.getFamilyBoundary()[mConfig.getDivisions() * i / mTrends.length]) {
                        //well it appears that this trend has re-appeared, take it out of the missing list and add the counts together
                        mTrends[trend.origIndex].count = trend.oldCount + mTrends[i].count;
                        mTrends[i].count = 0;
                        mMissingTrends.remove(x);
                        x--;
                        break;
                    }
                    //well then, is it going to GO THE DISTANCE?
                    if (trend.currentInterest == i && mTrends[i].count > mConfig.getMinTillAccept()) {
                        //well then we found us a speed!
                        double nspeed = getSpeed(fuzzyFreq(trend.index), fuzzyFreq(mTrends[i].index), mTemperature, 0.);
                        if (nspeed > mConfig.getMinSpeed() && nspeed < mConfig.getMaxSpeed()) {
                            mCalculatedSpeeds[mNumSpeeds] = nspeed;
                            mCalculatedSpeedsSource[mNumSpeeds] = (int) ((fuzzyFreq(trend.index) + fuzzyFreq(mTrends[i].index)) / 2.);
                            mNumSpeeds++;
                        }
                        //don't need this missing trend anymore
                        debug("[" + mFrameCount + "]: Found speed for missing trend " + trend.origIndex + " with " + i + ": " + nspeed + "MPH");
                        debug("       Frequencies " + mFft.indexToFreq(trend.origIndex) + " to " + mFft.indexToFreq(i));
                        mMissingTrends.remove(x);
                        x--;
                        break;
                    }
                    if (trend.currentInterest == i) {
                        //the trend is persisting still
                        trend.timeSinceLastInterest = -1;
                        trend.sig = mTrends[i].sig;
                    } else {
                        trend.currentInterest = i;
                        trend.sig = mTrends[i].sig;
                        trend.timeSinceLastInterest = -1;
                        break;
                    }
                }
            }
            trend.timeSinceLastInterest++;
        }
    }

    private void debug(String aMsg) {
        if (DEBUG) {
            Log.v(TAG, aMsg);
        }
    }
}
