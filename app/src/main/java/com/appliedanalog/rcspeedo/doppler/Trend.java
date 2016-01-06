/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

public class Trend {
    public float index;
    public float sig;
    public int count = 0;
    public boolean touched;

    public void reset(int ind){
        count = 0;
        index = ind;
    }
}
