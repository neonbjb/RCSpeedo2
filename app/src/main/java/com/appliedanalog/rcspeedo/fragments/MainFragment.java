package com.appliedanalog.rcspeedo.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.DopplerController;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import com.appliedanalog.rcspeedo.views.SpeedViewAdapter;

import java.util.Locale;

/**
 * Contains the main RCSpeedo interface as a fragment.
 */
public class MainFragment extends Fragment implements DopplerController.DopplerListener, TextToSpeech.OnInitListener {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

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

        mSpeedsAdapter = new SpeedViewAdapter(getActivity());
        mSpeeds.setAdapter(mSpeedsAdapter);

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
                SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
                mSpeed.setText(UnitManager.getInstance().getDisplaySpeed(aSpeedInMps.getSpeed()));
                mSpeedsAdapter.add(aSpeedInMps);
                if(mTtsReady && prefs.getBoolean(SettingsFragment.ENABLE_SOUND_KEY, true)) {
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
        //@todo - Remove from list.
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
}
