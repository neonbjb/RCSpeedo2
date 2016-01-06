package com.appliedanalog.rcspeedo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.DopplerController;
import com.appliedanalog.rcspeedo.controllers.Strings;

/**
 * Contains the main RCSpeedo interface as a fragment.
 */
public class MainFragment extends Fragment implements DopplerController.DopplerListener {
    private TextView mSpeed;
    private TextView mHighestSpeed;
    private TextView mTemperature;
    private TextView mLoggingStatus;
    private Button mAction;
    private Button mStartLogging;
    private ListView mSpeeds;
    private ProgressBar mIsActive;

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mSpeed = (TextView)view.findViewById(R.id.tSpeed);
        mHighestSpeed = (TextView)view.findViewById(R.id.tHighestSpeed);
        mTemperature = (TextView)view.findViewById(R.id.tTemperature);
        mLoggingStatus = (TextView)view.findViewById(R.id.tLogInUse);
        mSpeeds = (ListView)view.findViewById(R.id.lSpeeds);
        mIsActive = (ProgressBar)view.findViewById(R.id.pListening);

        mAction = (Button)view.findViewById(R.id.bStart);
        mStartLogging = (Button)view.findViewById(R.id.bStartLogging);
        mAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DopplerController.getInstance().isActive()) {
                    // Stop can sleep while waiting for the controller to terminate.
                    (new Thread("DopplerController Termination Waiter"){
                        public void run() {
                            DopplerController.getInstance().stop();
                        }
                    }).start();
                } else {
                    DopplerController.getInstance().start();
                }
            }
        });

        return view;
    }

    // Event handler implementations from DopplerController.DopplerListener.
    @Override
    public void dopplerActiveStateChanged(final boolean aIsActive) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(aIsActive) {
                    mAction.setText(Strings.getInstance().STOP_LISTENING);
                    mIsActive.setVisibility(View.VISIBLE);
                } else {
                    mAction.setText(Strings.getInstance().START_LISTENING);
                    mIsActive.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void dopplerError(final String aError) {
        //@todo - Implement
    }

    @Override
    public void newSpeedDetected(final double aSpeedInMps) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                /*mSpeed.setText(UnitManager.getInstance().getDisplaySpeed(aSpeedInMps));
                if(mTtsReady && mPrefs.getBoolean(SettingsFragment.ENABLE_SOUND_KEY)) {
                    String term = UnitManager.getInstance().getVocalSpeed(aSpeedInMps);
                    mTts.speak(term, TextToSpeech.QUEUE_FLUSH, null);
                }*/
            }
        });
    }

    @Override
    public void highestSpeedChanged(final double aNewHighestSpeedMps) {

    }
}
