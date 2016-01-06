/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.doppler;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;


public class AudioDoppler {
	final String TAG = "AudioDoppler";
    final boolean DEBUG = true;
    
    /**
     * All frequencies above and below these thresholds are ignored (they are inaccurately acquired by sound hardware,
     * or, in the case of the lower, too inaccurate for calculating doppler shifts)
     */
    float freq_min;
    float freq_max;

    //this should be related to freq_min and freq_max: its the number of divisions there are in later parameters
    //this is done to individually adjust the parameters across the frequency spectrum
    int divisions;

    /**
     * The amount of samples per 'frame' where the 'frames' are pushed through fourier.
     */
    FFT fft;
    int frame_size;
    int sampling_freq;
    float[] dist;
    int frame_count = 0;

    //this defines the amount of data values to the left and right of the peak that must be less than the peak for it to be considered a peak.
    int[] peak_width;
    //by the nature of peaks, there cannot be more than one peak for every PEAK_WIDTH * 2 data values, and the frequency distribution is inherently less than dist.length / 2
    int[] peaks;
    int num_peaks = 0;

    /**
     * a data point that is trending will continue trending as long as itself or datapoints within
     * the FAMILY distance are trending. If "family" data points are used to continue the trend,
     * they shift the index towards them to record their contribution.
     */
    int[] family_boundary;
    Trend[] trends = null;

    /**
     * When a trend originally goes missing, it will have WOUNDED_LIFE_EXPECTANCY time to reappear at
     * no penalty, after which it will become a "missing trend". Trends will only be established as missing
     * trends if they pass the constant COUNT_TILL_WOUNDED, which is a little less stringent than the certified
     * trend marker.
     */
    int count_till_wounded = 10;
    ArrayList<MissingTrend> wounded_trends = new ArrayList<MissingTrend>(); //this is a list of trends that retains a short lifespan before 'graduating' to a full on missing trend.
    final int WOUNDED_LIFE_EXPECTANCY = 3;

    /**
     * This defines the minimum amount of trend.count that must be recorded before
     * a trend is considered "certified" and is placed in the missing_trends list
     * upon disappearance
     */
    int min_trendcount_till_certified;
    
    /**
     * This is the maximum number of fourier frames for which a "missing" trend
     * persists before it is wiped. Note that it's related to the above constant
     * in that missing trends will be compared to certified trends, so you must wait
     * the desired window PLUS the amount of time it would take for a new trend to emerge.
     */
    int max_doppler_window;
    ArrayList<MissingTrend> missing_trends = new ArrayList<MissingTrend>();

    /**
     * This is the maximum and minimum speed that will be returned by findSpeeds()
     */
    float max_speed;
    float min_speed;
    double[] calculated_speeds;
    //this is the source frequency from which the above speeds were calculated. it is used to scale the calculated speed to favor speeds
    //derived from higher frequencies.
    int[] calculated_speeds_source;
    int num_speeds = 0;

    /**
     * The following two variables relate to the calculateSpeeds() function.
     * The max_freq_separation vector determines the maximum amount of frequency spread between each frame is allowed
     *          in doppler shift detection. Higher values will aid in detecting faster speeds but will also generate more erroneous
     *          readings.
     * The min_till_speed_accept variable determines how long a doppler shift has to "bottom out" before it is considered "done
     * shifting" and the speed is calculated.
     */
    int[] max_freq_separation;
    int min_till_speed_accept;

    double temperature;

    public AudioDoppler(AudioDopplerConfiguration config, int sampling_freq){
        applyConfiguration(config);
        this.frame_size = config.frame_size;
        this.sampling_freq = sampling_freq;
        temperature = 27;

        initArrays();
    }
    
    private void initArrays(){
        fft = new FFT(frame_size, 22500);
        dist = new float[frame_size];
        peaks = new int[frame_size / 4];
        trends = new Trend[frame_size / 2];
        for(int x = 0; x < trends.length; x++){
            trends[x] = new Trend();
            trends[x].index = x;
        }
        calculated_speeds = new double[frame_size / 4];
        calculated_speeds_source = new int[frame_size / 4];
    }

    public void applyConfiguration(AudioDopplerConfiguration config){
        this.frame_size = config.frame_size;
        if(dist != null && dist.length != frame_size){
        	initArrays();
        }
        freq_min = config.freq_min;
        freq_max = config.freq_max;
        peak_width = config.peak_width;
        family_boundary = config.family_boundary;
        min_trendcount_till_certified = config.min_trendcount_till_certified;
        count_till_wounded = min_trendcount_till_certified / 2;
        max_doppler_window = config.max_doppler_window;
        max_speed = config.max_speed;
        min_speed = config.min_speed;
        divisions = config.divisions;
        max_freq_separation = config.max_freq_separation;
        min_till_speed_accept = config.min_till_speed_accept;
    }

    public void audioToBuffer(short[] ain){
    	int skip = ain.length / dist.length;
    	for(int x = 0, y = 0; x < ain.length; x += skip, y++){
            dist[y] = ain[x];
    	}
    }

    /**
     * This variation of audioToBuffer allows a rotating audio buffer to be passed
     * into the audiodoppler, the "starting" index of the rotating buffer is passed
     * in as startloc
     */
    public void audioToBuffer(short[] ain, int startloc){
        int skip = ain.length / dist.length;
        int y = 0;
        for(int x = startloc; x < ain.length; x+= skip, y++){
            dist[y] = ain[x];
        }
        for(int x = 0; x < startloc; x += skip, y++){
            dist[y] = ain[x];
        }
    }

    PrintWriter fft_capture = null;

    /**
     * Advances the next frame
     * @param dist
     */
    public void nextFrame(){
        if(dist.length != frame_size){
            debug("Invalid frame size passed to nextFrame()");
            return;
        }

        frame_count++;
        fft.forward(dist);
        findPeaks(fft.getSpectrum(), fft.freqToIndex(freq_min), fft.freqToIndex(freq_max));
        updateTrends(fft.getSpectrum(), num_peaks);
        calculateSpeeds();
    }

    public int freqToIndex(float freq){
        return fft.freqToIndex(freq);
    }

    public float indexToFreq(int ind){
        return fft.indexToFreq(ind);
    }

    public float[] getFrameBuffer(){
        return dist;
    }

    public int getFrameCount(){
        return frame_count;
    }

    public int getPeakCount(){
        return num_peaks;
    }

    public int getPeak(int x){
        return peaks[x];
    }

    public int getTrendCount(){
        return trends.length;
    }

    public Trend getTrend(int x){
        return trends[x];
    }

    public float getFreqMin(){
        return freq_min;
    }

    public float getFreqMax(){
        return freq_max;
    }

    public int getMinTrendcountTillCertified(){
        return min_trendcount_till_certified;
    }

    public int getMaxDopplerWindow(){
        return max_doppler_window;
    }

    public int getFamilyBoundary(int x){
        return family_boundary[x];
    }

    public void setTemperature(double t){
    	Log.v(TAG, "AudioDoppler.setTemperature(" + t + ")");
        temperature = t;
    }

    public double getTemperature(){
        return temperature;
    }

    public int numDivisions(){
        return divisions;
    }

    static final double MIN_SPEED_WEIGHT = .125;
    /**
     *  this function is responsible for generating a "weighting" system for frequencies with detected speeds.
    	the weighting will apply MIN_SPEED_WEIGHT to FREQ_LOW, and a weight of 1.0 to FREQ_HIGH, with a continuous
    	linear weight in between
     * @param x The location in the speeds array to weight
     * @return
     */
    public double getSpeedWeight(int x){
    	return ((1 - MIN_SPEED_WEIGHT) * ((calculated_speeds_source[x] - freq_min) / (freq_max - freq_min))) + MIN_SPEED_WEIGHT;
    }

    public double getSpeed(int x){
    	return calculated_speeds[x];
    }

    public int numSpeeds(){
    	return num_speeds;
    }

    public double getAverageDominantSpeed(){
        if(num_speeds == 0){
            return -1.0;
        }
        double avg = 0.;
        for(int x = 0; x < num_speeds; x++){
            avg += calculated_speeds[x];
        }
        return avg / num_speeds;
    }


    double getSpeed(double freq1, double freq2, double temperature, double humidity){
        double sos = (331 + .606 * temperature); //current temperature is in Centigrade, this returns m/s
        return freq1 / ((freq1 + freq2) / 2) * sos - sos;
    }

    /**
     * This function returns the frequency as adjusted by the ratios of the passed index 'in'.
     * @param in
     * @return
     */
    public double fuzzyFreq(double in){
        double ciel_freq = fft.indexToFreq((int)Math.ceil(in));
        int floor = (int)Math.floor(in);
        double flor_freq = fft.indexToFreq(floor);
        double ratio = in - floor;
        return (ciel_freq * ratio + flor_freq * (1. - ratio));
    }

    void findPeaks(float[] spec, int start, int end){
        int p_point = 0;
        float pros_peak = 0.f;
        for(int x = start + peak_width[0]; x < end - peak_width[peak_width.length - 1]; x++){
            pros_peak = spec[x];
            //check the values around this prospective peak, continue if they are not smaller
            boolean peak_found = true;
            int cond = peak_width[divisions * (x - start) / end];
            for(int i = 1; peak_found && i <= cond; i++){
                if(spec[x + i] >= pros_peak || spec[x - i] >= pros_peak){
                    peak_found = false;
                }
            }
            if(peak_found){
                peaks[p_point] = x;
                p_point++;
            }
        }
        num_peaks = p_point;
    }

    void updateTrends(float[] spec, int num_peaks){
        //clean out the missing_trends array
        for(int x = 0; x < missing_trends.size(); x++){
            missing_trends.get(x).count--;
            if(missing_trends.get(x).count <= 0){
                missing_trends.remove(x);
                x--;
            }
        }
        
        //clear out any wounded trends that have truly died
        for(int x = 0; x < wounded_trends.size(); x++){
            MissingTrend mt = wounded_trends.get(x);
            if(mt.count <= 0){
                if(mt.old_count >= min_trendcount_till_certified){
                    debug("[" + frame_count + "]: Trend at " + (mt.orig_index - family_boundary[divisions * mt.orig_index / trends.length]) + " lost!");
                    mt.count = max_doppler_window;
                    missing_trends.add(mt);
                }
                wounded_trends.remove(x); x--;
            }else{
                mt.count--;
            }
        }

        //this variable points to the current 'peak' being referenced
        int c_peak_ptr = 0;
        //initialize the first FAMILY data points as 'untouched'
        for(int x = 0; x < family_boundary[0]; x++){
            trends[x].touched = false;
        }
        int next_trend_reset = family_boundary[0];
        for(int x = 0; x < trends.length; x++){
            int div = divisions * x / trends.length;
            //initialize the "new guys" in the current family as 'untouched'
            for(; next_trend_reset < trends.length && next_trend_reset <= x + family_boundary[div]; next_trend_reset++){
                trends[next_trend_reset].touched = false;
            }
            //did the guy that just left the current family stay untouched?
            if(x >= family_boundary[div] && !trends[x - family_boundary[div]].touched){
                //does the trend have enough count to be certified and saved as 'wounded'?
                Trend tsel = trends[x - family_boundary[div]];
                if(tsel.count >= count_till_wounded){
                    wounded_trends.add(new MissingTrend(tsel, x - family_boundary[div], WOUNDED_LIFE_EXPECTANCY));
                }
                //reset the index
                tsel.reset(x - family_boundary[div]);
            }
            //is the current pointer also a peak pointer?
            if(c_peak_ptr < num_peaks && x == peaks[c_peak_ptr]){
                //see if there is a wounded trend we can revive
                for(int i = 0; i < wounded_trends.size(); i++){
                    MissingTrend mt = wounded_trends.get(i);
                    if(Math.abs(mt.orig_index - x) < family_boundary[div]){
                        //revive the trend
                        trends[mt.orig_index].count = min_trendcount_till_certified + WOUNDED_LIFE_EXPECTANCY - mt.count;
                        wounded_trends.remove(i);

                        //also, remove any trending info building up elsewhere in the family due to this guy's absense
                        for(int f = 1; f < family_boundary[div]; f++){
                            trends[mt.orig_index + f].reset(mt.orig_index + f);
                            trends[mt.orig_index - f].reset(mt.orig_index - f);
                        }
                        break;
                    }
                }
                
                //find the highest trend within the immediate family
                int highest_trend = trends[x].count;
                int highest_trend_index = x;
                for(int f = 0; f < family_boundary[div]; f++){
                    if(x - f >= 0 && trends[x - f].count > highest_trend){
                        highest_trend_index = x - f;
                        highest_trend = trends[highest_trend_index].count;
                    }
                    if(x + f < trends.length && trends[x + f].count > highest_trend){
                        highest_trend_index = x + f;
                        highest_trend = trends[highest_trend_index].count;
                    }
                }
                //ignore it if there are two trends in one family.
                if(trends[highest_trend_index].touched){
                    debug("Merged trend at " + highest_trend_index);
                    continue;
                }
                //apply this new trend to the family member
                trends[highest_trend_index].index = (trends[highest_trend_index].index * (float)trends[highest_trend_index].count + (float)x) /
                                                    (trends[highest_trend_index].count + 1); //this is the current avg plus the new mark.
                trends[highest_trend_index].count++;
                trends[highest_trend_index].sig = spec[highest_trend_index];
                //mark the family member that received the trend
                trends[highest_trend_index].touched = true;

                if(trends[highest_trend_index].count == min_trendcount_till_certified){
                    debug("[" + frame_count + "]: Trend at " + highest_trend_index + " peaked!");
                }

                //increment the peak pointer
                c_peak_ptr++;
            }
        }
    }

    final int MAX_MISSING_FREQS_ACCEPTED = 3;
    void calculateSpeeds(){
        num_speeds = 0;
        //for each missing trend, we need to find new "interests", or see if the current
        //"interest" is accruing trend counts.
        for(int x = 0; x < missing_trends.size(); x++){
            MissingTrend trend = missing_trends.get(x);
            if(trend.time_since_last_interest >= MAX_MISSING_FREQS_ACCEPTED){
                missing_trends.remove(x);
                x--;
                debug("missing trend died prematurely!");
                continue;
            }

            //find the latest closest frequency that is trending
            int i_limit = trend.current_interest - max_freq_separation[divisions * trend.current_interest / trends.length];
            if(i_limit < 0) i_limit = 0;
            for(int i = trend.current_interest; i >= i_limit; i--){
                if(trends[i].count > 0){
                    //found a trending frequency nearby! is it in the family?
                    if(trend.orig_index - i < family_boundary[divisions * i / trends.length]){
                        //well it appears that this trend has re-appeared, take it out of the missing list and add the counts together
                        trends[trend.orig_index].count = trend.old_count + trends[i].count;
                        trends[i].count = 0;
                        missing_trends.remove(x); x--;
                        break;
                    }
                    //well then, is it going to GO THE DISTANCE?
                    if(trend.current_interest == i && trends[i].count > min_till_speed_accept){
                        //well then we found us a speed!
                        double nspeed = getSpeed(fuzzyFreq(trend.index), fuzzyFreq(trends[i].index), temperature, 0.);
                        if(nspeed > min_speed && nspeed < max_speed){
                            calculated_speeds[num_speeds] = nspeed;
                            calculated_speeds_source[num_speeds] = (int)((fuzzyFreq(trend.index) + fuzzyFreq(trends[i].index)) / 2.);
                            num_speeds++;
                        }
                        //don't need this missing trend anymore
                        debug("[" + frame_count + "]: Found speed for missing trend " + trend.orig_index + " with " + i + ": " + nspeed + "MPH");
                        debug("       Frequencies " + indexToFreq(trend.orig_index) + " to " + indexToFreq(i));
                        missing_trends.remove(x); x--;
                        break;
                    }
                    if(trend.current_interest == i){
                        //the trend is persisting still
                        trend.time_since_last_interest = -1;
                        trend.sig = trends[i].sig;
                    }else{
                        trend.current_interest = i;
                        trend.sig = trends[i].sig;
                        trend.time_since_last_interest = -1;
                        break;
                    }
                }
            }
            trend.time_since_last_interest++;
        }
    }

    void debug(String msg){
        if(DEBUG){
            Log.v(TAG, msg);
        }
    }
}
