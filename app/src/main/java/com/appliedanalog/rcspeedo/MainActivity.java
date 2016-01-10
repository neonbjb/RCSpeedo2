/*
 * Copyright Applied Analog (c) 2015/2016
 *
 * This code is free for use in any non-commercial software. It carries
 * no restrictions in such software.
 */

package com.appliedanalog.rcspeedo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.appliedanalog.rcspeedo.controllers.DopplerController;
import com.appliedanalog.rcspeedo.controllers.LoggingDbController;
import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.WeatherController;
import com.appliedanalog.rcspeedo.controllers.data.DetectedSpeed;
import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import com.appliedanalog.rcspeedo.controllers.data.logs.GroupInfoEntry;
import com.appliedanalog.rcspeedo.controllers.data.logs.ModelLog;
import com.appliedanalog.rcspeedo.controllers.data.logs.SpeedLogEntry;
import com.appliedanalog.rcspeedo.fragments.LogManagerFragment;
import com.appliedanalog.rcspeedo.fragments.MainFragment;
import com.appliedanalog.rcspeedo.fragments.SettingsFragment;

import java.util.Collection;

/**
 * Main activity of RCSpeedo.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Fragments which can be swapped in and out on request
    MainFragment mainFragment;
    LogManagerFragment logFragment;
    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabSaveLog);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DopplerController.getInstance().getDetectedSpeeds().size() <= 0) {
                    Toast toast = Toast.makeText(MainActivity.this, Strings.getInstance().ERR_NO_SPEEDS, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    showSaveLogDialog();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        // Set the default checked option to the first one.
        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Strings localizer
        Strings.init(getResources());

        // Initialize the unit manager
        UnitManager.getInstance().init(this);

        // Initialize the WeatherController
        WeatherController.getInstance().init((LocationManager)getSystemService(Context.LOCATION_SERVICE),
                                                PreferenceManager.getDefaultSharedPreferences(this));

        // Initialize the background fragments.
        settingsFragment = new SettingsFragment();
        logFragment = new LogManagerFragment();

        // Populate the fragment container.
        if (findViewById(R.id.fragment_container) != null) {
            // If we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
        }

        // Keep the screen on when the DopplerController is active.
        DopplerController.getInstance().addSpeedListener(new DopplerController.DopplerListener() {
            @Override
            public void dopplerActiveStateChanged(final boolean aIsActive) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(aIsActive) {
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } else {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }
                });
            }

            @Override
            public void dopplerError(String aError) { }
            @Override
            public void newSpeedDetected(DetectedSpeed aSpeedInMps) { }
            @Override
            public void highestSpeedChanged(DetectedSpeed aNewHighestSpeedMps) { }
            @Override
            public void speedInvalidated(DetectedSpeed aSpeed) { }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        DopplerController.getInstance().powerdown();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadNewFragment(Fragment aFragment, boolean aBackStackAppropriate){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, aFragment);
        if(aBackStackAppropriate){
            transaction.addToBackStack(null);
        }

        // Commit the transaction
        transaction.commit();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.main_screen_nav_button) {
            loadNewFragment(mainFragment, false);
        } else if (id == R.id.clear_speeds) {
            DopplerController.getInstance().clearSpeeds();
        } else if (id == R.id.log_manager_nav_button) {
            loadNewFragment(logFragment, true);
        } else if (id == R.id.settings_nav_button) {
            loadNewFragment(settingsFragment, true);
        } else if (id == R.id.help_nav_button) {
            // Simply bring up the manual on the website.
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.rcspeedo.com/guide/manual/"));
            startActivity(browserIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Show a dialog to let the user to pick the desired log and start logging to it.
     */
    private void showSaveLogDialog(){
        final LoggingDbController ldb = LoggingDbController.getInstance(getApplicationContext());
        Collection<ModelLog> models = ldb.getAllModels();
        if(models.size() <= 0){
            Toast toast = Toast.makeText(MainActivity.this, Strings.getInstance().ERR_NO_LOGS, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        final Dialog createDlg = new Dialog(this);
        createDlg.setContentView(R.layout.picklogdlg);
        createDlg.setTitle(Strings.getInstance().PICK_LOG_TITLE);

        final Spinner lAvailableLogs = (Spinner)createDlg.findViewById(R.id.lAvailableLogs);
        final Button bPickLog = (Button)createDlg.findViewById(R.id.bPickLog);
        final Button bGoLogging = (Button)createDlg.findViewById(R.id.bGoLogging);
        final Button bCancelLogging = (Button)createDlg.findViewById(R.id.bCancelLogging);
        final EditText tExtraInfo = (EditText)createDlg.findViewById(R.id.tExtraInfo);

        //Set up spinner adapter
        ArrayAdapter<String> laLogs = new ArrayAdapter<String>(this, R.layout.logs);
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

        bGoLogging.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createDlg.hide();
                loadNewFragment(logFragment, true);
            }
        });

        bPickLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String logname = lAvailableLogs.getSelectedItem().toString();
                ModelLog log = ldb.getModelLog(logname);
                if(log == null) {
                    Toast toast = Toast.makeText(MainActivity.this, Strings.getInstance().ERR_NO_LOGS, Toast.LENGTH_SHORT);
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
