/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

/**
 * Construct which holds data for a Trend which has gone missing. The idea is that sometimes trends
 * will go 'missing' but are actually undergoing a Doppler shift. These are the cases that we are
 * trying to detect.
 */
public class MissingTrend extends Trend{
    /**
     * Original FFT index this trend was on.
     */
    public int origIndex;
    /**
     * The FFT index the AudioDoppler currently suspects this Trend is at.
     */
    public int currentInterest;
    /**
     * Time since the trend was last confirmed.
     */
    public int timeSinceLastInterest;

    /**
     * The previous count of the trend before it went missing.
     */
    public int oldCount;

    public MissingTrend(Trend orig, int orig_ind, int ncount){
        origIndex = orig_ind;
        currentInterest = orig_ind;
        oldCount = orig.count;
        count = ncount;
        index = orig.index;
        timeSinceLastInterest = 0;
        sig = orig.sig;
    }
}
