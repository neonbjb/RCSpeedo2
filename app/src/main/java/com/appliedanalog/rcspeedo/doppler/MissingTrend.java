/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

public class MissingTrend extends Trend{
    public int orig_index;
    public int current_interest;
    public int time_since_last_interest;
    public int old_count;

    public MissingTrend(Trend orig, int orig_ind, int ncount){
        orig_index = orig_ind;
        current_interest = orig_ind;
        old_count = orig.count;
        count = ncount;
        index = orig.index;
        time_since_last_interest = 0;
        sig = orig.sig;
    }
}
