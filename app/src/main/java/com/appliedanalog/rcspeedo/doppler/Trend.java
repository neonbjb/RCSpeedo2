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
