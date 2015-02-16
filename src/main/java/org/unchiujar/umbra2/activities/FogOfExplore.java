/*******************************************************************************
 * This file is part of Umbra.
 *
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *
 *    http://www.gnu.org/licenses/gpl-3.0.html
 *
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 *        Yen-Liang, Shen - Simplified Chinese and Traditional Chinese translations
 ******************************************************************************/


package org.unchiujar.umbra2.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unchiujar.umbra2.R;
import org.unchiujar.umbra2.backend.ExploredProvider;
import org.unchiujar.umbra2.overlays.CustomUrlProvider;
import org.unchiujar.umbra2.overlays.ExploredTileProvider;
import org.unchiujar.umbra2.services.LocationService;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import hugo.weaving.DebugLog;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.view.MenuItem.*;
import static android.view.View.*;
import static org.unchiujar.umbra2.R.string.bug_me_not;
import static org.unchiujar.umbra2.R.string.connectivity_warning;
import static org.unchiujar.umbra2.R.string.continue_no_gps;
import static org.unchiujar.umbra2.R.string.fullscreen;
import static org.unchiujar.umbra2.R.string.gps_dialog;
import static org.unchiujar.umbra2.R.string.importing_locations;
import static org.unchiujar.umbra2.R.string.prefs_bug_me_not;
import static org.unchiujar.umbra2.R.string.prefs_number_of_launches;
import static org.unchiujar.umbra2.R.string.rate;
import static org.unchiujar.umbra2.R.string.rate_umbra;
import static org.unchiujar.umbra2.R.string.rate_umbra_message;
import static org.unchiujar.umbra2.R.string.remind_later;
import static org.unchiujar.umbra2.R.string.share_app_text;
import static org.unchiujar.umbra2.R.string.start_gps_btn;
import static org.unchiujar.umbra2.activities.Preferences.DRIVE_MODE;
import static org.unchiujar.umbra2.activities.Preferences.FULLSCREEN;
import static org.unchiujar.umbra2.io.GpxImporter.importGPXFile;
import static org.unchiujar.umbra2.overlays.ExploredTileProvider.TILE_SIZE;

/**
 * Main activity for Umbra application.
 *
 * @author Vasile Jureschi
 * @see LocationService
 */
public class FogOfExplore extends ActionBarActivity {
    public static final int FRONT = Integer.MAX_VALUE;
    public static final int BACK = 0;
    public static final int MIDDLE = Integer.MAX_VALUE / 2;
    public static final String TILE_SOURCE = "org.unchiujar.umbra.settings.tile_source";
    /**
     * Initial map zoom.
     */
    private static final int INITIAL_ZOOM = 17;
    /**
     * Constant used for saving the accuracy value between screen rotations.
     */
    private static final String BUNDLE_ACCURACY = "org.unchiujar.umbra.accuracy";
    /**
     * Constant used for saving the latitude value between screen rotations.
     */
    private static final String BUNDLE_LATITUDE = "org.unchiujar.umbra.latitude";
    /**
     * Constant used for saving the longitude value between screen rotations.
     */
    private static final String BUNDLE_LONGITUDE = "org.unchiujar.umbra.longitude";
    /**
     * Constant used for saving the zoom level between screen rotations.
     */
    private static final String BUNDLE_ZOOM = "org.unchiujar.umbra.zoom";
    /**
     * Intent named used for starting the location service
     *
     * @see LocationService
     */
    private static final String SERVICE_INTENT_NAME = "org.com.unchiujar.umbra2.LocationService";
    private static final Logger LOGGER = LoggerFactory.getLogger(FogOfExplore.class);
    private static final int RATE_ME_MINIMUM_LAUNCHES = 4;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * Dialog displayed while loading the explored points at application start.
     */
    private ProgressDialog mLoadProgress;
    /**
     * Location service intent.
     *
     * @see LocationService
     */
    private Intent mLocationServiceIntent;
    /**
     * Source for obtaining explored area information.
     */
    private ExploredProvider mRecorder;
    /**
     * Current device latitude. Updated on every location change.
     */
    private double mCurrentLat;
    /**
     * Current device longitude. Updated on every location change.
     */
    private double mCurrentLong;
    /**
     * Current location accuracy . Updated on every location change.
     */
    private double mCurrentAccuracy;
    /**
     * Flag signaling if the user is walking or driving. It is passed to the location service in
     * order to change location update frequency.
     *
     * @see LocationService
     */
    private boolean mDrive;
    /**
     * Drive or walk preference listener. A listener is necessary for this option as the location
     * service needs to be notified of the change in order to change location update frequency. The
     * preference is sent when the activity comes into view and rebinds to the location service.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            LOGGER.debug("Settings changed {} key {}", sharedPreferences, key);
            mDrive = mSettings.getBoolean(DRIVE_MODE, false);
        }
    };
    /**
     * Messenger for communicating with service.
     */
    private Messenger mService = null;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            LOGGER.debug("Location service attached.");
            // register client
            sendMessage(LocationService.MSG_REGISTER_CLIENT);
            // register interface
            sendMessage(LocationService.MSG_REGISTER_INTERFACE);

            // send walk or drive mode
            sendMessage(mDrive ? LocationService.MSG_DRIVE
                    : LocationService.MSG_WALK);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service has been
            // unexpectedly disconnected / process crashed.
            mService = null;
            LOGGER.debug("Disconnected from location service");
        }
    };
    /**
     * Flag indicating whether we have called bind on the service.
     */
    private boolean mIsBound;
    private SharedPreferences mSettings;
    private GoogleMap map;
    private GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            //if we are only zooming in then do nothing, the topOverlay will be scaled automatically
            updateExplored();
            wiggleLayers();
        }
    };
    private boolean overlaySwitch = false;
    private TileOverlayOptions mapOverlay;
    private ExploredTileProvider provider;

    // ==================== LIFECYCLE METHODS ====================
    private TileOverlay topOverlay;
    private TileOverlay bottomOverlay;
    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = getDefaultSharedPreferences(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLoadProgress = ProgressDialog.show(this, "", "Loading. Please wait...", true);

        mSettings.registerOnSharedPreferenceChangeListener(mPrefListener);
        setContentView(R.layout.main);


        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);

        // Get a handle to the Map Fragment
        map = mapFragment.getMap();
        map.setMyLocationEnabled(true);
        map.setOnCameraChangeListener(cameraListener);
        map.setMapType(GoogleMap.MAP_TYPE_NONE);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);

        LOGGER.debug("onCreate completed: Activity created");
        mLocationServiceIntent = new Intent(SERVICE_INTENT_NAME);
        startService(mLocationServiceIntent);

        // zoom and move to the current location
        Location currentLocation = map.getMyLocation();
        if (currentLocation != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), INITIAL_ZOOM));
        }

        mRecorder = ((UmbraApplication) getApplication()).getCache();
        // check we still have access to GPS info
        checkConnectivity();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.icon);
        setSupportActionBar(toolbar);

        askForStars();

        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                boolean updateCamera = getDefaultSharedPreferences(FogOfExplore.this).getBoolean("org.unchiujar.umbra.settings.animate", false);
                if (updateCamera) {
                    // get current camera info, and update it with the current lat, lng
                    CameraPosition cameraPosition = map.getCameraPosition();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(
                            new LatLng(location.getLatitude(), location.getLongitude()), cameraPosition.zoom, cameraPosition.tilt, cameraPosition.bearing
                    ));
                    map.animateCamera(cameraUpdate);
                }

            }
        });
    }

    private void askForStars() {
        final SharedPreferences prefs = getDefaultSharedPreferences(this);
        // check if we have the do not bother me flag
        boolean bugMeNot = prefs.getBoolean(getString(prefs_bug_me_not), false);
        if (bugMeNot) {
            return;
        }
        //check if the user has started the application at least 5 times
        int launches = prefs.getInt(getString(prefs_number_of_launches), 0);

        if (launches < RATE_ME_MINIMUM_LAUNCHES) {
            prefs.edit().putInt(getString(prefs_number_of_launches), launches + 1).apply();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(rate_umbra))
                .setMessage(getString(rate_umbra_message))
                .setNegativeButton(getString(bug_me_not), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(getString(prefs_bug_me_not), true).apply();
                    }
                })
                .setNeutralButton(getString(remind_later), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putInt(getString(prefs_number_of_launches), 0).apply();
                    }
                })
                .setPositiveButton(getString(rate), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(getString(prefs_bug_me_not), true).apply();
                        Uri uri = Uri.parse("market://details?id=" + getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        LOGGER.debug("Created rate intent {}", goToMarket);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="
                                    + getPackageName())));
                        }
                    }
                });
        builder.create().show();
    }

    /**
     * Loads a gpx data from a file path sent through an intent.
     */
    private void loadFileFromIntent() {
        //sanity checks
        Intent intent = getIntent();
        LOGGER.debug("Intent for loading data from file is {}", intent);
        // there was no intent so just return
        if (intent == null) {
            return;
        }
        Uri data = intent.getData();
        LOGGER.debug("File URI is {}", data);

        //there was no URI data so  just return
        if (data == null) {
            return;
        }

        //if we have all the data we need then load the file
        final String filePath = data.getEncodedPath();

        final ProgressDialog progress = new ProgressDialog(this);

        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setMessage(getString(importing_locations));
        progress.show();

        Runnable importer = new Runnable() {

            @Override
            public void run() {
                ExploredProvider cache = ((UmbraApplication) getApplication())
                        .getCache();

                try {
                    cache.insert(importGPXFile(new FileInputStream(
                            new File(filePath))));
                } catch (ParserConfigurationException | SAXException e) {
                    LOGGER.error("Error parsing file", e);
                } catch (IOException e) {
                    LOGGER.error("Error reading file", e);
                }

                progress.dismiss();
            }
        };
        new Thread(importer).start();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore accuracy and coordinates from saved state
        mCurrentAccuracy = savedInstanceState.getDouble(BUNDLE_ACCURACY);
        mCurrentLat = savedInstanceState.getDouble(BUNDLE_LATITUDE);
        mCurrentLong = savedInstanceState.getDouble(BUNDLE_LONGITUDE);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .zoom(savedInstanceState.getFloat(BUNDLE_ZOOM))
                .target(new LatLng(mCurrentLat, mCurrentLong))
                .build();

        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // save accuracy and coordinates
        outState.putDouble(BUNDLE_ACCURACY, mCurrentAccuracy);
        outState.putDouble(BUNDLE_LATITUDE, mCurrentLat);
        outState.putDouble(BUNDLE_LONGITUDE, mCurrentLong);

        outState.putFloat(BUNDLE_ZOOM, map.getCameraPosition().zoom);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOGGER.debug("onStart completed: Activity started");
    }

    @Override
    protected void onResume() {
        super.onResume();

        configureToolbar();

        loadFileFromIntent();
        map.setOnCameraChangeListener(cameraListener);
        mLoadProgress.cancel();
        LOGGER.debug("onResume completed.");
        // bind to location service
        doBindService();

        map.clear();

        String tilesUrl = getDefaultSharedPreferences(this)
                .getString(TILE_SOURCE, "http://otile2.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png");
        CustomUrlProvider provider = new CustomUrlProvider(TILE_SIZE, TILE_SIZE, tilesUrl);
        mapOverlay = new TileOverlayOptions().tileProvider(provider).zIndex(MIDDLE);
        map.addTileOverlay(mapOverlay);

        // Create new TileOverlayOptions instance.
        TileOverlayOptions opts = new TileOverlayOptions();
        // Set the tile provider to your custom implementation.
        this.provider = new ExploredTileProvider(this);
        opts.fadeIn(false).tileProvider(this.provider);
        // set the layer in front
        opts.zIndex(FRONT);


        // Add the tile overlay to the map.
        topOverlay = map.addTileOverlay(opts);


        // Create new TileOverlayOptions instance.
        TileOverlayOptions backOpts = new TileOverlayOptions();

        backOpts.fadeIn(false).tileProvider(this.provider);
        // set the layer in the back
        backOpts.zIndex(BACK);
        bottomOverlay = map.addTileOverlay(backOpts);

        redrawOverlay();
    }

    private void configureToolbar() {
        boolean fullScreen = getDefaultSharedPreferences(this).getBoolean(FULLSCREEN, false);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(fullScreen? GONE: VISIBLE);

        ImageButton fullscreenButton = (ImageButton) findViewById(R.id.fullscreen);
        fullscreenButton.setVisibility(fullScreen ? VISIBLE:GONE);

        int action = fullScreen? SHOW_AS_ACTION_NEVER: SHOW_AS_ACTION_IF_ROOM;
        configureToolbarOnFullscreenChange(action);
    }

    private void configureToolbarOnFullscreenChange(int action) {
        if (menu != null) {
            menu.findItem(R.id.settings).setShowAsAction(action);
            menu.findItem(R.id.help).setShowAsAction(action);
            menu.findItem(R.id.exit).setShowAsAction(action);
            menu.findItem(R.id.share_app).setShowAsAction(action);
        }
    }

    public void disableFullscreen(View v){
        // hide self
        v.setVisibility(GONE);
        // update fullscreen flag
        SharedPreferences.Editor edit = getDefaultSharedPreferences(this).edit();
        edit.putBoolean(FULLSCREEN, false).apply();
        // make toolbar visible
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(VISIBLE);

        configureToolbarOnFullscreenChange(SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unbind from service as the activity does
        // not display location info (is hidden or stopped)
        doUnbindService();
        LOGGER.debug("onPause completed.");
    }

    @Override
    @DebugLog
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        this.menu = menu;
        configureToolbar();
        return result;
    }

    //    // ================= END LIFECYCLE METHODS ====================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.help:
                LOGGER.debug("Showing help...");
                Intent helpIntent = new Intent(this, Help.class);
                startActivity(helpIntent);
                return true;
            case R.id.exit:
                LOGGER.debug("Exit requested...");
                doUnbindService();
                // cleanup
                stopService(mLocationServiceIntent);
                mRecorder.destroy();
                finish();
                return true;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, Preferences.class);
                startActivity(settingsIntent);
                return true;
            case R.id.share_app:

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(share_app_text));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the current location and calls an overlay redraw.
     */
    private void redrawOverlay() {
        updateExplored();

        if (!overlaySwitch) {
            bottomOverlay.setZIndex(FRONT);
            topOverlay.setZIndex(BACK);
            topOverlay.clearTileCache();
        } else {
            topOverlay.setZIndex(FRONT);
            bottomOverlay.setZIndex(BACK);
            bottomOverlay.clearTileCache();
        }
        overlaySwitch = !overlaySwitch;
    }

    /**
     * Rearrange layers, fixes, Google maps redraw fuckup.
     */
    private void wiggleLayers() {
        if (overlaySwitch) {
            bottomOverlay.setZIndex(FRONT);
            mapOverlay.zIndex(MIDDLE);
            topOverlay.setZIndex(BACK);
        } else {
            topOverlay.setZIndex(FRONT);
            mapOverlay.zIndex(MIDDLE);
            bottomOverlay.setZIndex(BACK);
        }
    }

    private void updateExplored() {
        // get the coordinates of the visible area
        LatLng farLeft = map.getProjection().getVisibleRegion().farLeft;
        LatLng nearRight = map.getProjection().getVisibleRegion().nearRight;

        // TODO - optimization get points for rectangle only if a zoom out
        // or a pan action occurred - ie new points come into view

        // update the overlay with the currently visible explored area
//        OverlayFactory.getInstance(this).setExplored(mRecorder.selectVisited(upperLeft, bottomRight));

//        provider.setExplored(mRecorder.selectVisited(upperLeft, bottomRight), (int) map.getCameraPosition().zoom);
        provider.setExplored(mRecorder.selectAll(), (int) map.getCameraPosition().zoom);

    }

    /**
     * Checks GPS and network connectivity. Displays a dialog asking the user to start the GPS if
     * not started and also displays a toast warning it no network connectivity is available.
     */
    private void checkConnectivity() {

        boolean isGPS = ((LocationManager) getSystemService(LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGPS) {
            createGPSDialog().show();
        }
        displayConnectivityWarning();
    }

    /**
     * Displays a toast warning if no network is available.
     */
    private void displayConnectivityWarning() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = false;
        for (NetworkInfo info : connectivityManager.getAllNetworkInfo()) {
            if (info.getState() == NetworkInfo.State.CONNECTED
                    || info.getState() == NetworkInfo.State.CONNECTING) {
                connected = true;
                break;
            }
        }

        if (!connected) {
            Toast.makeText(getApplicationContext(),
                    connectivity_warning, Toast.LENGTH_LONG).show();

        }
    }

    /**
     * Creates the GPS dialog displayed if the GPS is not started.
     *
     * @return the GPS Dialog
     */
    private Dialog createGPSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(gps_dialog).setCancelable(false);

        final AlertDialog alert = builder.create();

        alert.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(start_gps_btn),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }
        );

        alert.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(continue_no_gps),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        alert.dismiss();
                    }
                }
        );
        return alert;
    }

    /**
     * Binds to the location service. Called when the activity becomes visible.
     */
    private void doBindService() {
        bindService(mLocationServiceIntent, mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        LOGGER.debug("Binding to location service");
    }

    /**
     * Unbinds from the location service. Called when the activity is stopped or closed.
     */
    private void doUnbindService() {
        if (mIsBound) {
            // test if we have a valid service registration
            if (mService != null) {
                sendMessage(LocationService.MSG_UNREGISTER_INTERFACE);
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            LOGGER.debug("Unbinding map from location service.");
        }
    }

    private void sendMessage(int message) {
        // TODO check message
        try {
            Message msg = Message.obtain(null, message);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            // NO-OP
            // Nothing special to do if the service
            // has crashed.
        }

    }

    /**
     * Handler of incoming messages from service.
     */
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_LOCATION_CHANGED:
                    if (msg.obj != null) {
                        LOGGER.debug(msg.obj.toString());

                        mCurrentLat = ((Location) msg.obj).getLatitude();
                        mCurrentLong = ((Location) msg.obj).getLongitude();
                        mCurrentAccuracy = ((Location) msg.obj).getAccuracy();
                        // redraw overlay
                        redrawOverlay();

                    } else {
                        LOGGER.debug("Null object received");
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
