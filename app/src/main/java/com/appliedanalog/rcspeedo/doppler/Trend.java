/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

/**
 * Construct which contains the data necessary to keep track of a trend of peaking FFT indices.
 */
public class Trend {
    /**
     * The FFT index this Trend is currently tracking.
     */
    public float index;
    /**
     * A unique signature used to correlate Trends from different FFT indices with each other (e.g.
     * when they are shifting due to a doppler shift).
     */
    public float sig;
    /**
     * Number of frames in which this Trend has been encountered.
     */
    public int count = 0;
    /**
     * Flag variable used by AudioDoppler.
     */
    public boolean touched;

    /**
     * Resets the count and FFT index of this Trend.
     * @param ind
     */
    public void reset(int ind){
        count = 0;
        index = ind;
    }
}
