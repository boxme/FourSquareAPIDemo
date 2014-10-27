package com.desmond.foursquareapidemo;

import android.app.Dialog;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import br.com.condesales.EasyFoursquareAsync;
import br.com.condesales.criterias.VenuesCriteria;
import br.com.condesales.listeners.FoursquareVenuesRequestListener;
import br.com.condesales.models.Venue;


public class MainActivity extends ActionBarActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private EasyFoursquareAsync mAsync;

    private LocationClient mLocationClient;

    private MapView mMapView;
    private GoogleMap mMap;

    private ListView mVenuesListView;

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVenuesListView = (ListView) findViewById(R.id.list_nearby_venues);

        MapsInitializer.initialize(this);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMap = mMapView.getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);

        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();

        // ask for access
        mAsync = new EasyFoursquareAsync(this);
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        getVenues();
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        }
    }

    private void getVenues() {
        Location currentLocation = getLocation();
        VenuesCriteria venuesCriteria = new VenuesCriteria();
        venuesCriteria.setRadius(250);
        venuesCriteria.setLocation(currentLocation);

        mAsync.getVenuesNearby(new FoursquareVenuesRequestListener() {

            @Override
            public void onVenuesFetched(ArrayList<Venue> venues) {
                setupAdapter(venues);
                setupMap();
            }

            @Override
            public void onError(String errorMsg) {}

        }, venuesCriteria);
    }

    private boolean servicesConnected() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(TAG, "Google play service is available");
            return true;
        }
        else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), "google play not available");
            }
            return false;
        }
    }

    private Location getLocation() {
        Location currentLocation = null;

        if (servicesConnected()) {
            // Get the current location
            currentLocation = mLocationClient.getLastLocation();
        }

        return currentLocation;
    }

    private void setupMap() {
        final Location currentLocation = getLocation();
        final LatLng locationLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        final MarkerOptions options = new MarkerOptions().position(locationLatLng).title("Meet Here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        options.draggable(true);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 18));
        mMap.addMarker(options).showInfoWindow();
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });

        mVenuesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mMap.clear();
                LatLng newLatLng;
                if (position == 0) {
                    Location currentLocation = getLocation();
                    newLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                }
                else {
                    Venue venue = (Venue) (mVenuesListView.getAdapter()).getItem(position);
                    newLatLng = new LatLng(venue.getLocation().getLat(), venue.getLocation().getLng());
                }
                options.position(newLatLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 18));
                mMap.addMarker(options).showInfoWindow();
            }
        });
    }

    private void setupAdapter(List<Venue> venues) {

        if (mVenuesListView.getAdapter() == null) {
            mVenuesListView.setAdapter(new VenuesListAdapter(venues));
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private static class VenuesListAdapter extends BaseAdapter {

        private List<Venue> mNearbyVenues;

        public VenuesListAdapter(List<Venue> nearbyVenues) {
            mNearbyVenues = nearbyVenues;
        }

        @Override
        public int getCount() {
            return mNearbyVenues.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == 0) {
                return null;
            }

            return mNearbyVenues.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.venue_location_layout, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            viewHolder = (ViewHolder) convertView.getTag();

            if (position == 0) {
                viewHolder.mVenueName.setText("Current Location");
                viewHolder.mVenueAddress.setText("");
            }
            else {
                Venue venue = mNearbyVenues.get(position - 1);
                br.com.condesales.models.Location location = venue.getLocation();

                viewHolder.mVenueName.setText(venue.getName());
                viewHolder.mVenueAddress.setText(location.getAddress());
            }

            return convertView;
        }

        private static class ViewHolder {
            TextView mVenueName;
            TextView mVenueAddress;

            public ViewHolder(View view) {
                mVenueName = (TextView) view.findViewById(R.id.venue_name);
                mVenueAddress = (TextView) view.findViewById(R.id.venue_address);
            }
        }
    }
}
