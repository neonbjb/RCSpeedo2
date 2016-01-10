/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.data.logs.EmptyLogEntry;
import com.appliedanalog.rcspeedo.controllers.LoggingDbController;
import com.appliedanalog.rcspeedo.controllers.data.logs.ModelLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * UI fragment that allows the user to create/delete models and review logs stored against those
 * models.
 */
public class LogManagerFragment extends Fragment {
    final String TAG = "LogManagerFragment";

    private Spinner mAvailableLogs;
    private Button mCreateModel;
    private Button mDeleteModel;
    private Button mEmailLog;
    private Strings mStrings;

    private ArrayAdapter<String> mLogs;
    private HashMap<String, ModelLog> mLogMap = new HashMap<String, ModelLog>();

    SimpleDateFormat mDateFormat; //this is used as a default log date

    //These two state variables are to handle issues with adding items to the list - when you do this
    //a bunch of listSelected() events will be thrown which will turn off logging. These help prevent this.
    boolean mIsInitializing = false;
    boolean mFirstSelect = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStrings = Strings.getInstance();
        mDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        View view = inflater.inflate(R.layout.logging, container, false);

        //instantiate local mViews
        mCreateModel = (Button) view.findViewById(R.id.bCreateLog);
        mDeleteModel = (Button) view.findViewById(R.id.bDeleteLog);
        mEmailLog = (Button) view.findViewById(R.id.bEmailLog);
        mAvailableLogs = (Spinner) view.findViewById(R.id.lLogs);
        mLogs = new ArrayAdapter<String>(getActivity(), R.layout.logs);
        mAvailableLogs.setAdapter(mLogs);

        //set up action handlers
        mCreateModel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            final Dialog createDlg = new Dialog(getActivity());
            createDlg.setContentView(R.layout.createlogdlg);
            createDlg.setTitle(mStrings.CRT_LOG_TITLE);

            final EditText tLogName = (EditText) createDlg.findViewById(R.id.tCreateLogNameDlg);
            final Button bCreateLogDlg = (Button) createDlg.findViewById(R.id.bCreateLogDlg);

            bCreateLogDlg.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                createLogCompleted(tLogName.getText().toString());
                createDlg.hide();
                }
            });
            createDlg.show();
            }
        });

        mDeleteModel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            final ModelLog clog = getSelectedLog();
            if (clog == null) {
                alert(mStrings.ERR_NO_LOG_SELECTED);
                return;
            }

            //verify that the user wants to do this
            AlertDialog.Builder verify = new AlertDialog.Builder(getActivity());
            verify.setTitle(mStrings.CNFRM_TITLE)
                .setMessage(mStrings.CNFRM_BODY)
                .setPositiveButton(mStrings.camel(mStrings.YES), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LoggingDbController ldb = LoggingDbController.getInstance(getActivity().getApplicationContext());
                        ldb.deleteModelLog(clog.getName());
                        mLogs.remove(clog.getName());
                    }
                })
                .setNegativeButton(mStrings.camel(mStrings.NO), null)
                .show();
            }
        });

        mEmailLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            ModelLog clog = getSelectedLog();
            if (clog == null) {
                alert(mStrings.ERR_NO_LOG_SELECTED);
                return;
            }
            File tlogfile = LoggingDbController.getInstance(getContext()).generateSpeedLogFile(clog);
            if (tlogfile == null) {
                alert(mStrings.ERR_CANNOT_CREATE_LOG);
                return;
            }
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/csv");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "RCSpeedo logs for model: " + clog.getName());
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + tlogfile.getAbsolutePath()));
            Log.v(TAG, "Extra stream: " + "file://" + tlogfile.getAbsolutePath());
            startActivity(Intent.createChooser(emailIntent, "Send mail.."));
            }
        });

        mAvailableLogs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (mIsInitializing) return;
                if (mFirstSelect) {
                    mFirstSelect = false;
                    return;
                }
                Log.v(TAG, "Item selected");
                LoggingDbController ldb = LoggingDbController.getInstance(getActivity().getApplicationContext());
                ModelLog log = ldb.getModelLog((String) mAvailableLogs.getSelectedItem());
                setSelectedLog(log);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                if (mIsInitializing) return;
                if (mFirstSelect) {
                    mFirstSelect = false;
                    return;
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        mIsInitializing = true;
        LoggingDbController ldb = LoggingDbController.getInstance(getActivity().getApplicationContext());
        Collection<ModelLog> alllogs = ldb.getAllModels();
        Iterator<ModelLog> iter = alllogs.iterator();
        String defaultModel = ldb.getDefaultModel();

        //refresh the logs in that folder
        mLogs.clear();
        mLogMap.clear();
        int possel = -1;
        while (iter.hasNext()) {
            ModelLog next = iter.next();
            mLogs.add(next.getName());
            mLogMap.put(next.getName(), next);

            if (defaultModel != null && next.getName().equals(defaultModel)) {
                possel = mLogs.getCount() - 1;
            }
        }
        if (possel != -1) {
            mAvailableLogs.setSelection(possel);
        }
        mIsInitializing = false;
        mFirstSelect = true;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    protected void createLogCompleted(String model) {
        if (model.trim().length() == 0) {
            alert(mStrings.ERR_NAME_REQUIRED);
            model = mStrings.LOG_DEFAULT_MODEL_NAME;
        }
        // Insert it into the database
        LoggingDbController ldb = LoggingDbController.getInstance(getActivity().getApplicationContext());
        if (ldb.getModelLog(model) != null) {
            alert(mStrings.ERR_LOG_EXISTS);
            return;
        }
        ModelLog log = new ModelLog(model);
        ldb.addLogEntry(new EmptyLogEntry(), log); // An empty entry will ensure this log shows up in the list of available logs.
        // Add it to the list of logs
        mLogs.add(model);
        mLogMap.put(model, log);
        // Aelect it (make sure selectLog() is called)
        mAvailableLogs.setSelection(mLogs.getCount() - 1);
        setSelectedLog(log);
    }

    private void setSelectedLog(ModelLog log) {
        // Set this as the default log going forwards
        LoggingDbController.getInstance(getContext()).setDefaultModel(log.getName());
    }

    private ModelLog getSelectedLog() {
        if (mAvailableLogs.getSelectedItem() == null) {
            return null;
        }
        return mLogMap.get(mAvailableLogs.getSelectedItem());
    }

    // Simple dialog generation code.
    //@todo - Update this to use the modern Android dialog paradigm.
    String mAlertText;

    void alert(String msg) {
        mAlertText = msg;
        getActivity().showDialog(DIALOG_ALERT);
    }

    void alertCritical(String msg) {
        mAlertText = msg;
        getActivity().showDialog(DIALOG_ALERT_CRITICAL);
    }

    final int DIALOG_ALERT = 0;
    final int DIALOG_ALERT_CRITICAL = 1;

    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_ALERT:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(mAlertText)
                        .setPositiveButton(mStrings.OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                dialog = builder.create();
                break;
            case DIALOG_ALERT_CRITICAL:
                builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(mAlertText)
                        .setPositiveButton(mStrings.OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //@todo - Switch fragment to main view.
                            }
                        });
                dialog = builder.create();
                break;
        }
        return dialog;
    }
}
