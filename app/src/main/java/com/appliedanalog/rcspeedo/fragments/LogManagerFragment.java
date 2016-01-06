package com.appliedanalog.rcspeedo.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.appliedanalog.rcspeedo.R;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.logs.EmptyLogEntry;
import com.appliedanalog.rcspeedo.logs.LoggingDatabase;
import com.appliedanalog.rcspeedo.logs.RCLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class LogManagerFragment extends Fragment {
	
	TextView tModelName;
	TextView tLoggingStatus;
	TextView tNumberEntries;
	Spinner lAvailableLogs;
	Button bCreateLog;
	Button bDeleteLog;
	Button bEmailLog;
	Strings s;
	
	ArrayAdapter<String> laLogs;
    HashMap<String, RCLog> logMap = new HashMap<String, RCLog>();
	
    SimpleDateFormat date_format; //this is used as a default log date
    
    /** Called when the activity is first created. */
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		s = Strings.getInstance();
		date_format = new SimpleDateFormat("MM/dd/yyyy");
        super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.logging, container, false);
        
        //instantiate local views
        tModelName = (TextView)view.findViewById(R.id.lModelName);
        tLoggingStatus = (TextView)view.findViewById(R.id.lLoggingStatus);
		tNumberEntries = (TextView)view.findViewById(R.id.lNumberEntries);
		bCreateLog = (Button)view.findViewById(R.id.bCreateLog);
        bDeleteLog = (Button)view.findViewById(R.id.bDeleteLog);
        bEmailLog = (Button)view.findViewById(R.id.bEmailLog);
        lAvailableLogs = (Spinner)view.findViewById(R.id.lLogs);
        laLogs = new ArrayAdapter<String>(getActivity(), R.layout.logs);
        lAvailableLogs.setAdapter(laLogs);

        //set up action handlers
        bCreateLog.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
            	final Dialog createDlg = new Dialog(getActivity());
            	createDlg.setContentView(R.layout.createlogdlg);
            	createDlg.setTitle(s.CRT_LOG_TITLE);
            	
            	final EditText tLogName = (EditText)createDlg.findViewById(R.id.tCreateLogNameDlg);
            	final EditText tModelName = (EditText)createDlg.findViewById(R.id.tCreateLogModelDlg);
            	final Button bCreateLogDlg = (Button)createDlg.findViewById(R.id.bCreateLogDlg);

            	bCreateLogDlg.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						createLogCompleted(tLogName.getText().toString(), tModelName.getText().toString());
						createDlg.hide();
					}
				});
            	createDlg.show();
            }
        });
        
        bDeleteLog.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
            	final RCLog clog = getSelectedLog();
            	if(clog == null){
            		alert(s.ERR_NO_LOG_SELECTED);
            		return;
            	}
            	
            	//verify that the user wants to do this
            	AlertDialog.Builder verify = new AlertDialog.Builder(getActivity());
            	verify.setTitle(s.CNFRM_TITLE)
            		  .setMessage(s.CNFRM_BODY)
            		  .setPositiveButton(s.camel(s.YES), new DialogInterface.OnClickListener() {
          				public void onClick(DialogInterface dialog, int which) {
          					LoggingDatabase ldb = LoggingDatabase.getData(getActivity().getApplicationContext());
          					ldb.deleteLog(clog.getName());
          	            	laLogs.remove(clog.getName());
          	            	if(RCLog.isLogging() && RCLog.getCurrentLog().getName().equals(clog)){
          	            		stopLogging();
          	            		RCLog.setCurrentLog(null);
          	            	}
        				}
            		  })
          			  .setNegativeButton(s.camel(s.NO), null)
            		  .show();      	
            }
        });
        
        bEmailLog.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
            	RCLog clog = getSelectedLog();
            	if(clog == null){
            		alert(s.ERR_NO_LOG_SELECTED);
            		return;
            	}
            	File tlogfile = clog.generateSpeedLogFile();
            	if(tlogfile == null){
            		alert(s.ERR_CANNOT_CREATE_LOG);
            		return;
            	}
            	Intent emailIntent = new Intent(Intent.ACTION_SEND);
            	emailIntent.setType("text/csv");
            	emailIntent.putExtra(Intent.EXTRA_SUBJECT, "RCSpeedo log: " + clog.getName() + " for " + clog.getModel());
            	emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + tlogfile.getAbsolutePath()));
            	System.out.println("Extra stream: " + "file://" + tlogfile.getAbsolutePath());
            	startActivity(Intent.createChooser(emailIntent, "Send mail.."));
            }
        });
        
        lAvailableLogs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if(is_initializing) return;
				if(first_select){
					first_select = false;
					return;
				}
				System.out.println("Item selected");
				LoggingDatabase ldb = LoggingDatabase.getData(getActivity().getApplicationContext());
				RCLog log = ldb.getLog((String)lAvailableLogs.getSelectedItem());
				stopLogging();
				RCLog.setCurrentLog(log);
				setSelectedLog(log);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				if(is_initializing) return;
				if(first_select){
					first_select = false;
					return;
				}
				stopLogging();
			}
        });

		return view;
    }
    
    private void setSelectedLog(RCLog log){
		tModelName.setText(log.getModel());
		int nentries = log.getNumberEntries() - 1;
		if(nentries < 0){
			nentries = 0;
		}
		tNumberEntries.setText(Integer.toString(nentries)); //There is always a "Fake" entry, hence the -1
    }
    
    protected void createLogCompleted(String name, String model){
    	stopLogging();
    	if(name.trim().length() == 0){
    		alert(s.ERR_NAME_REQUIRED);
    		return;
    	}
    	if(model.trim().length() == 0){
    		model = s.LOG_DEFAULT_MODEL_NAME;
    	}
    	//insert it into the database
    	LoggingDatabase ldb = LoggingDatabase.getData(getActivity().getApplicationContext());
    	if(ldb.getLog(name) != null){
    		alert(s.ERR_LOG_EXISTS);
    		return;
    	}
    	RCLog log = new RCLog(name, model);
    	ldb.addLogEntry(new EmptyLogEntry(), log); //An empty entry will ensure this log shows up in the list of available logs.
    	//add it to the list of logs
    	laLogs.add(name);
		logMap.put(name, log);
    	//select it (make sure selectLog() is called)
    	lAvailableLogs.setSelection(laLogs.getCount() - 1);
    	RCLog.setCurrentLog(log);
    	setSelectedLog(log);
    }
    
    //These two state variables are to handle issues with adding items to the list - when you do this
    //a bunch of listSelected() events will be thrown which will turn off logging. These help prevent this.
    boolean is_initializing = false;
    boolean first_select = false;
    @Override
    public void onStart(){
    	super.onStart();
    	
    	is_initializing = true;
    	LoggingDatabase ldb = LoggingDatabase.getData(getActivity().getApplicationContext());
    	Collection<RCLog> alllogs = ldb.getAllLogs();
    	Iterator<RCLog> iter = alllogs.iterator();
    	
    	//refresh the logs in that folder
    	laLogs.clear();
    	logMap.clear();
    	int possel = -1;
    	int pos = 0;
    	while(iter.hasNext()){
    		RCLog next = iter.next();
    		laLogs.add(next.getName());
    		logMap.put(next.getName(), next);
    		if(RCLog.isLogging() && RCLog.getCurrentLog().getName().equals(next.getName())){
    			possel = pos;
    			boolean wasLogging = RCLog.isLogging();
    			RCLog.setCurrentLog(next); //It might have more recent data, why not?
    			if(wasLogging){
    				RCLog.startLogging();
    			}else{
    				RCLog.stopLogging();
    			}
    		}
    		pos++;
    	}
    	is_initializing = false;
    	first_select = true;
    	
    	if(RCLog.isLogging()){
    		tLoggingStatus.setText(s.LOGGING_ENABLED);
    		tLoggingStatus.setTextColor(0xFF00FF00);
    		setSelectedLog(RCLog.getCurrentLog());
    		if(possel == -1){
    			System.out.println("Critical Error! Could not find current log in the list of available logs!");
    		}else{
    			lAvailableLogs.setSelection(possel);
    		}
    	}else{
    		stopLogging();
    		if(alllogs.size() > 0){
    			setSelectedLog(alllogs.iterator().next());
    		}
    	}
    }
    
    RCLog getSelectedLog(){
    	if(lAvailableLogs.getSelectedItem() == null){
    		return null;
    	}
    	return logMap.get(lAvailableLogs.getSelectedItem());
    }
    
    void stopLogging(){
    	System.out.println(s.STOP_LISTENING);
    	RCLog.stopLogging();
		tLoggingStatus.setText(s.LOGGING_DISABLED);
		tLoggingStatus.setTextColor(0xFFFF0000);
		tModelName.setText(s.NONE_NAME);
		tNumberEntries.setText("0");
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
    
    String _alert_text; //vulnerable to threading..
    void alert(String msg){
    	_alert_text = msg;
		getActivity().showDialog(DIALOG_ALERT);
    }
    
    void alertCritical(String msg){
    	_alert_text = msg;
		getActivity().showDialog(DIALOG_ALERT_CRITICAL);
    }
    
    final int DIALOG_ALERT = 0;
    final int DIALOG_ALERT_CRITICAL = 1;
    protected Dialog onCreateDialog(int id){
    	Dialog dialog = null;
    	switch(id){
    	case DIALOG_ALERT:
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	builder.setMessage(_alert_text)
        		   .setPositiveButton(s.OK, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int which) { }
    			});
        	dialog = builder.create();
        	break;
    	case DIALOG_ALERT_CRITICAL:
        	builder = new AlertDialog.Builder(getActivity());
        	builder.setMessage(_alert_text)
        		   .setPositiveButton(s.OK, new DialogInterface.OnClickListener() {
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
