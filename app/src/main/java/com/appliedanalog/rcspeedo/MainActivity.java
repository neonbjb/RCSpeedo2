package com.appliedanalog.rcspeedo;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.appliedanalog.rcspeedo.controllers.Strings;
import com.appliedanalog.rcspeedo.controllers.data.UnitManager;
import com.appliedanalog.rcspeedo.fragments.LogManagerFragment;
import com.appliedanalog.rcspeedo.fragments.MainFragment;
import com.appliedanalog.rcspeedo.fragments.SettingsFragment;

/**
 * Main activity of RCSpeedo.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainFragment.OnFragmentInteractionListener {

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Clearing speeds now", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                //@todo - Clear speeds.
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize Strings localizer
        Strings.init(getResources());

        // Initialize the unit manager
        UnitManager.getInstance().init(this);

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
        } else if (id == R.id.log_manager_nav_button) {
            loadNewFragment(logFragment, true);
        } else if (id == R.id.settings_nav_button) {
            loadNewFragment(settingsFragment, true);
        } else if (id == R.id.help_nav_button) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
