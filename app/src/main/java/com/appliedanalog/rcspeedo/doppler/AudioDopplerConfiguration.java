package com.appliedanalog.rcspeedo.doppler;

/**
 *
 * @author James
 */
public class AudioDopplerConfiguration {
    public float freq_min;
    public float freq_max;
    public int[] peak_width;
    public int[] family_boundary;
    public int min_trendcount_till_certified;
    public int max_doppler_window;
    public float max_speed;
    public float min_speed;
    public int divisions;
    public int[] max_freq_separation;
    public int min_till_speed_accept;
    public int frame_size = 1024; //default starting point
    public int samples_per_frame = 1024; //default starting point

    public AudioDopplerConfiguration(float freqmin, float freqmax, int[] peakwidth, int[] familyboundary,
            int mintrendcount, int maxwindow, float maxspeed, float minspeed, int[] freqsep, int mintillaccept){
        freq_min = freqmin;
        freq_max = freqmax;
        peak_width = peakwidth;
        family_boundary = familyboundary;
        min_trendcount_till_certified = mintrendcount;
        max_doppler_window = maxwindow;
        max_speed = maxspeed;
        min_speed = minspeed;
        divisions = peakwidth.length;
        max_freq_separation = freqsep;
        min_till_speed_accept = mintillaccept;
        if(peakwidth.length != divisions || familyboundary.length != divisions){
            System.out.println("Critical error in AudioDopplerConfiguration: divisions and array length mismatch");
        }
    }

    public AudioDopplerConfiguration(float freqmin, float freqmax, int peakwidth, int familyboundary,
            int mintrendcount, int maxwindow, float maxspeed, float minspeed, int freqsep, int mintillaccept){
        freq_min = freqmin;
        freq_max = freqmax;
        min_trendcount_till_certified = mintrendcount;
        max_doppler_window = maxwindow;
        max_speed = maxspeed;
        min_speed = minspeed;
        divisions = (int)(freq_max - freq_min) / 500;
        peak_width = expand(peakwidth);
        family_boundary = expand(familyboundary);
        max_freq_separation = expand(freqsep);
        min_till_speed_accept = mintillaccept;
    }

    /**
     * Scales configuration parameters affected by framesize accordingly this should be called
     * anytime you intend to move the framesize above or below 1024
     * @param factor
     */
    public AudioDopplerConfiguration scaleFrameSize(int fs){
        double factor = (double)fs / (double)frame_size;
        frame_size = fs;
        for(int x = 0; x < peak_width.length; x++){
            peak_width[x] *= factor;
        }
        for(int x = 0; x < family_boundary.length; x++){
            family_boundary[x] *= factor;
        }
        for(int x = 0; x < max_freq_separation.length; x++){
            max_freq_separation[x] *= factor;
        }
        return this;
    }

    /**
     * This should be called anytime you intend to feed less than or greather than 1024 new audio
     * samples per audio doppler frame. Typically what is done in this case is only the oldest n
     * samples are deleted from the buffer and replaced with new samples.
     * @param samps_per_frame
     * @return
     */
    public AudioDopplerConfiguration scaleSamplesPerFrame(int samps_per_frame){
        double factor = (double)samps_per_frame / (double)samples_per_frame;
        samples_per_frame = samps_per_frame;
        max_doppler_window *= factor;
        return this;
    }

    public static AudioDopplerConfiguration DEFAULT = new AudioDopplerConfiguration(500.f, 10000.f, 15, 3, 15, 55, 250.f, 20.f, 10, 10);
    public static AudioDopplerConfiguration FAST_PASS = new AudioDopplerConfiguration(500.f, 10000.f, 15, 3, 5, 55, 250.f, 20.f, 10, 10);
    public static AudioDopplerConfiguration CFG_200_PLUS = new AudioDopplerConfiguration(500.f, 10000.f,
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

    double[] expansion_ratios = { .4, .5, .64, .8, .93,
						          1., //mid
						          1.15, 1.31, 1.48, 1.64, 1.79, 1.9, 2.1, 2.27, 2.36,
						          2.5, 2.6, 2.7, 2.8 };

    private int[] expand(int in){
        int[] ret = new int[divisions];
        if(expansion_ratios.length != divisions){
            System.out.println("Flattening frequency parameter curves because the configuration is out of spec.");
        }
        for(int x = 0; x < ret.length; x++){
            if(expansion_ratios.length == divisions){
                ret[x] = (int)(expansion_ratios[x] * (double)in);
                if(ret[x] == 0){
                    ret[x] = 1; //these parameters should never be 0..
                }
            }else{
                ret[x] = in;
            }
        }
        return ret;
    }
}
