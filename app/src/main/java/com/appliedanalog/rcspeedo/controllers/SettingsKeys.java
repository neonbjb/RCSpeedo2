/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.controllers;

/**
 * Class that contains all possible Settings keys that can be used in RCSpeedo.
 */
public class SettingsKeys {

    // Boolean setting for whether or not speed speaking is enabled.
    public static final String ENABLE_SOUND_KEY = "sounds_enable";

    // Doppler mode setting - enumeration.
    public static final String DOPPLER_MODE_KEY = "change_mode";

    // Speed unit (K/H, M/S, MPH)
    public static final String SPEED_UNIT_KEY = "speed_unit";

    // Whether or not to use GPS to derive local weather.
    public static final String ENABLE_GPS_KEY = "enable_gps";

    // User-set temperature for when GPS weather is disabled or unavailable.
    public static final String HARD_TEMP_KEY = "hard_temp";

    // The temperature unit to use. (C or F)
    public static final String TEMP_UNIT_KEY = "temperature_unit";

    // Default logging model.
    public static final String DEFAULT_MODEL = "default_model";

    // These bindings come from strings.xml - Any modifications there should be made here too.
    // @todo - Make these bindings in code.
    public static final String DOPPLER_MODE_DEFAULT = "default_mode";
    public static final String DOPPLER_MODE_HI_SPEED = "hi_speed_mode";
    public static final String DOPPLER_MODE_FAST_PASS = "fast_pass_mode";
}
