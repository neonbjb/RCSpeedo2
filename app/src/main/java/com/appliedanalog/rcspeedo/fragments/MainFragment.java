/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.DopplerController;
import com.appliedanalog.rcspeedo.controllers.LoggingDbController;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.WeatherController;
import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.controllers.UnitManager;
import com.appliedanalog.rcspeedo.controllers.data.logs.GroupInfoEntry;
import com.appliedanalog.rcspeedo.controllers.data.logs.ModelLog;
import com.appliedanalog.rcspeedo.controllers.data.logs.SpeedLogEntry;
import com.appliedanalog.rcspeedo.views.SpeedViewAdapter;

import java.util.Collection;
import java.util.Locale;

import static com.appliedanalog.rcspeedo.controllers.SettingsKeys.ENABLE_SOUND_KEY;

/**
 * Contains the main RCSpeedo interface as a fragment.
 */
public class MainFragment extends Fragment implements DopplerController.DopplerListener,
                                                      WeatherController.TemperatureListener,
                                                      TextToSpeech.OnInitListener {
    final String TAG = "MainFragment";

    // UI Elements
    private TextView mSpeed;
    private TextView mHighestSpeed;
    private TextView mTemperature;
    private TextView mStatus;
    private Button mAction;
    private ListView mSpeeds;
    private SpeedViewAdapter mSpeedsAdapter;

    // Functional components.
    private TextToSpeech mTts;
    private boolean mTtsReady;

    /**
     * Default constructor.
     */
    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the fragment is created.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTts = new TextToSpeech(getActivity().getApplicationContext(), this);
        PowerManager powerManager = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);

        // Register for DopplerController updates.
        DopplerController.getInstance().addSpeedListener(this);

        // Register for temperature updates.
        WeatherController.getInstance().addListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        mSpeed = (TextView)view.findViewById(R.id.tSpeed);
        mHighestSpeed = (TextView)view.findViewById(R.id.tHighestSpeed);
        mTemperature = (TextView)view.findViewById(R.id.tTemperature);
        mStatus = (TextView)view.findViewById(R.id.tExtraStatus);
        mSpeeds = (ListView)view.findViewById(R.id.lSpeeds);

        mAction = (Button)view.findViewById(R.id.bStart);
        mAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DopplerController.getInstance().isActive()) {
                    Log.v(TAG, "Stopping the DopplerController..");

                    // Stop can sleep while waiting for the controller to terminate.
                    (new Thread("DopplerController Termination Waiter"){
                        public void run() {
                            DopplerController.getInstance().stop();
                        }
                    }).start();
                } else {
                    Log.v(TAG, "Starting the DopplerController..");
                    DopplerController.getInstance().start();
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton)view.findViewById(R.id.fabSaveLog);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DopplerController.getInstance().getDetectedSpeeds().size() <= 0) {
                    Toast toast = Toast.makeText(getContext(), Strings.getInstance().ERR_NO_SPEEDS, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    showSaveLogDialog();
                }
            }
        });

        mSpeedsAdapter = new SpeedViewAdapter(getActivity());
        mSpeeds.setAdapter(mSpeedsAdapter);

        // Initialize temperature.
        temperatureChanged(WeatherController.getInstance().getLastTemperature(), WeatherController.getInstance().isTemperatureDerivedFromLocation());

        return view;
    }

    // Event handler implementations from DopplerController.DopplerListener.
    @Override
    public void dopplerActiveStateChanged(final boolean aIsActive) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(aIsActive) {
                    mAction.setText(Strings.getInstance().STOP_LISTENING);
                } else {
                    mAction.setText(Strings.getInstance().START_LISTENING);
                }
            }
        });
    }

    @Override
    public void dopplerError(final String aError) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(!aError.isEmpty()) {
                    Toast toast = Toast.makeText(MainFragment.this.getContext(), aError, Toast.LENGTH_LONG);
                    toast.show();
                    // Show error in the logging status label too.
                    mStatus.setText(aError);
                }
            }
        });
    }

    @Override
    public void newSpeedDetected(final DetectedSpeed aSpeedInMps) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                mSpeed.setText(UnitManager.getInstance().getDisplaySpeed(aSpeedInMps.getSpeed()));
                mSpeedsAdapter.add(aSpeedInMps);
                if(mTtsReady && prefs.getBoolean(ENABLE_SOUND_KEY, true)) {
                    String term = UnitManager.getInstance().getVocalSpeed(aSpeedInMps.getSpeed());

                    // Using the deprecated speak for backwards compatibility.
                    mTts.speak(term, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    public void highestSpeedChanged(final DetectedSpeed aNewHighestSpeedMps) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mHighestSpeed.setText(Strings.getInstance().HIGH_SPEED_IND +
                        UnitManager.getInstance().getDisplaySpeed(aNewHighestSpeedMps.getSpeed()));
            }
        });
    }

    @Override
    public void speedInvalidated(final DetectedSpeed aSpeed) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mSpeedsAdapter.remove(aSpeed);
            }
        });
    }

    // Event handlers for TextToSpeech.OnInitListener
    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            if(mTts.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_MISSING_DATA){
                Intent installTts = new Intent();
                installTts.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTts);
            }
            mTtsReady = true;
            mTts.setLanguage(Locale.getDefault());
        }
    }

    // Event handlers for WeatherController.
    @Override
    public void temperatureChanged(final int aNewTemp, final boolean aTemperatureWasAutomaticallySet) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mTemperature.setText(Strings.getInstance().TEMPERATURE_IND + UnitManager.getInstance().getTemperature(aNewTemp));
                if(aTemperatureWasAutomaticallySet) {
                    mTemperature.setTextColor(Color.parseColor("#003300"));
                } else {
                    mTemperature.setTextColor(Color.parseColor("#000066"));
                }
            }
        });
    }

    /**
     * Show a dialog to let the user to pick the desired log and start logging to it.
     */
    private void showSaveLogDialog(){
        final LoggingDbController ldb = LoggingDbController.getInstance(getContext());
        Collection<ModelLog> models = ldb.getAllModels();
        if(models.size() <= 0){
            Toast toast = Toast.makeText(getContext(), Strings.getInstance().ERR_NO_LOGS, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        final Dialog createDlg = new Dialog(getActivity());
        createDlg.setContentView(R.layout.picklogdlg);
        createDlg.setTitle(Strings.getInstance().PICK_LOG_TITLE);

        final Spinner lAvailableLogs = (Spinner)createDlg.findViewById(R.id.lAvailableLogs);
        final Button bPickLog = (Button)createDlg.findViewById(R.id.bPickLog);
        final Button bCancelLogging = (Button)createDlg.findViewById(R.id.bCancelLogging);
        final EditText tExtraInfo = (EditText)createDlg.findViewById(R.id.tExtraInfo);

        //Set up spinner adapter
        ArrayAdapter<String> laLogs = new ArrayAdapter<String>(getContext(), R.layout.logs);
        lAvailableLogs.setAdapter(laLogs);
        laLogs.clear();
        for(ModelLog next : models) {
            laLogs.add(next.getName());
        }

        //Add action handlers for button.
        bCancelLogging.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createDlg.hide();
            }
        });

        bPickLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String logname = lAvailableLogs.getSelectedItem().toString();
                ModelLog log = ldb.getModelLog(logname);
                if(log == null) {
                    Toast toast = Toast.makeText(getContext(), Strings.getInstance().ERR_NO_LOGS, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                int groupId = ldb.generateGroupId();
                String extraInfo = tExtraInfo.getText().toString();
                if(!extraInfo.trim().isEmpty()) {
                    ldb.addLogEntry(new GroupInfoEntry(groupId, extraInfo), log);
                }
                for (DetectedSpeed speed : DopplerController.getInstance().getDetectedSpeeds()) {
                    ldb.addLogEntry(new SpeedLogEntry(speed.getTimestamp(), speed.getSpeed(), groupId), log);
                }
                DopplerController.getInstance().clearSpeeds();
                createDlg.hide();
            }
        });
        createDlg.show();
    }
}
