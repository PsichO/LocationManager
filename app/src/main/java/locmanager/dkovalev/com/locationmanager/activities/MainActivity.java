package locmanager.dkovalev.com.locationmanager.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfoLte;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.melnykov.fab.FloatingActionButton;

import locmanager.dkovalev.com.locationmanager.R;
import locmanager.dkovalev.com.locationmanager.assets.BackgroundIntentService;

public class MainActivity extends ActionBarActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private ListView placesListView;

    protected GoogleApiClient googleApiClient;
    protected LocationRequest locationRequest;
    protected Location location;

    private double lat;
    private double lng;


    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private Notification notification;

    private Button startLocationUpdatesButton;

    private BroadcastReceiver receiver;

    private SharedPreferences settingsPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settingsPreference = PreferenceManager.getDefaultSharedPreferences(this);
        setupUI();
        buildGoogleAPIClient();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
                if (resultCode == RESULT_OK) {
                    double resultValueLat = intent.getDoubleExtra("resultValueLat", 0.0);
                    double resultValueLng = intent.getDoubleExtra("resultValueLng", 0.0);
                    createNotification(MainActivity.this, resultValueLat, resultValueLng);
                    startLocationUpdates();
                }
            }
        };

    }

    private int getUpdateTime(int updateTime){
        return updateTime;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BackgroundIntentService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        int updateTime = Integer.valueOf(settingsPreference.getString("update_time", "0"));
        createLocationRequest(updateTime);

        startLocationUpdatesButton.setText(settingsPreference.getString("update_time", "NOPE"));
    }

    private void setupUI() {
        placesListView = (ListView) findViewById(R.id.list_of_places_lv);

        FloatingActionButton fab_addNewPlace = (FloatingActionButton) findViewById(R.id.fab_add_new_place);
        fab_addNewPlace.attachToListView(placesListView);
        fab_addNewPlace.setType(FloatingActionButton.TYPE_NORMAL);
        fab_addNewPlace.setColorNormal(getResources().getColor(R.color.primary));
        fab_addNewPlace.setColorPressed(getResources().getColor(R.color.primary_dark));
        fab_addNewPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Hello", Toast.LENGTH_SHORT).show();
                startAddNewPlaceActivity();
            }
        });

        FloatingActionButton fab_showMap = (FloatingActionButton) findViewById(R.id.fab_show_map);
        fab_showMap.attachToListView(placesListView);
        fab_showMap.setType(FloatingActionButton.TYPE_NORMAL);
        fab_showMap.setColorNormal(getResources().getColor(R.color.primary));
        fab_showMap.setColorPressed(getResources().getColor(R.color.primary_dark));
        fab_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Hello Map", Toast.LENGTH_SHORT).show();
                startMapActivity();
            }
        });

        startLocationUpdatesButton = (Button) findViewById(R.id.button_start_location_updates);
        startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startLocationUpdates();
                //startLocationUpdatesButton.setVisibility(View.GONE);
            }
        });

    }

    private synchronized void buildGoogleAPIClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        /*updateTime = Integer.valueOf(settingsPreference.getString("update_time", "0"));
        createLocationRequest(updateTime);*/
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (location == null) {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (location != null) {

                lat = location.getLatitude();
                lng = location.getLongitude();

                startBackgroundService();
            }
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();

            startBackgroundService();
        }
    }

    private void startBackgroundService() {
        Intent background = new Intent(this, BackgroundIntentService.class);
        background.putExtra("lat", lat);
        background.putExtra("lng", lng);
        startService(background);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void createLocationRequest(int updateTime) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(getUpdateTime(updateTime));
        locationRequest.setFastestInterval(updateTime / 2);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BackgroundIntentService.ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void createNotification(Context context, Double lat, Double lng) {
        Intent intent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Notification")
                .setContentText(String.valueOf(lat + " " + lng))
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(1, notification);
    }

    private void startAddNewPlaceActivity() {
        Intent intent = new Intent(MainActivity.this, AddNewPlaceActivity.class);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        startActivity(intent);
    }

    private void startMapActivity() {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
