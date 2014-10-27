package com.desmond.foursquareapidemo;

import android.app.Dialog;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import java.util.ArrayList;

import br.com.condesales.EasyFoursquareAsync;
import br.com.condesales.criterias.CheckInCriteria;
import br.com.condesales.criterias.TipsCriteria;
import br.com.condesales.criterias.VenuesCriteria;
import br.com.condesales.listeners.AccessTokenRequestListener;
import br.com.condesales.listeners.CheckInListener;
import br.com.condesales.listeners.FoursquareVenuesRequestListener;
import br.com.condesales.listeners.ImageRequestListener;
import br.com.condesales.listeners.TipsRequestListener;
import br.com.condesales.listeners.UserInfoRequestListener;
import br.com.condesales.models.Checkin;
import br.com.condesales.models.Tip;
import br.com.condesales.models.User;
import br.com.condesales.models.Venue;
import br.com.condesales.tasks.users.UserImageRequest;


public class MainActivity extends ActionBarActivity implements
        AccessTokenRequestListener,
        ImageRequestListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private EasyFoursquareAsync mAsync;
    private ImageView mUserImage;
    private ViewSwitcher mViewSwitcher;
    private TextView mUserName;

    private LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserImage = (ImageView) findViewById(R.id.imageView1);
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher1);
        mUserName = (TextView) findViewById(R.id.textView1);

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
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
        }
    }

    @Override
    public void onAccessGrant(String accessToken) {
        // with the access token you can perform any request to foursquare.
        mAsync.getUserInfo(new UserInfoRequestListener() {

            @Override
            public void onError(String errorMsg) {
                // Some error getting user info
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onUserInfoFetched(User user) {
                if (user.getBitmapPhoto() == null) {
                    UserImageRequest request = new UserImageRequest(
                            MainActivity.this, MainActivity.this);
                    request.execute(user.getPhoto());
                }
                else {
                    mUserImage.setImageBitmap(user.getBitmapPhoto());
                }
                mUserName.setText(user.getFirstName() + " " + user.getLastName());
                mViewSwitcher.showNext();
                Toast.makeText(MainActivity.this, "Got it!", Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    @Override
    public void onImageFetched(Bitmap bmp) {
        mUserImage.setImageBitmap(bmp);
    }

    @Override
    public void onError(String errorMsg) {
        // Do something with the error message
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    private void checkin() {

        CheckInCriteria criteria = new CheckInCriteria();
        criteria.setBroadcast(CheckInCriteria.BroadCastType.PUBLIC);
        criteria.setVenueId("4c7063da9c6d6dcb9798d27a");

        mAsync.checkIn(new CheckInListener() {
            @Override
            public void onCheckInDone(Checkin checkin) {
                Toast.makeText(MainActivity.this, checkin.getId(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(MainActivity.this, "error", Toast.LENGTH_LONG).show();
            }
        }, criteria);
    }

    private void requestTipsNearby() {
        Location loc = new Location("");
        loc.setLatitude(40.4363483);
        loc.setLongitude(-3.6815703);

        TipsCriteria criteria = new TipsCriteria();
        criteria.setLocation(loc);
        mAsync.getTipsNearby(new TipsRequestListener() {

            @Override
            public void onError(String errorMsg) {
                Toast.makeText(MainActivity.this, "error", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTipsFetched(ArrayList<Tip> tips) {
                Toast.makeText(MainActivity.this, tips.toString(), Toast.LENGTH_LONG).show();
            }
        }, criteria);
    }

    private void getVenues() {
        Location currentLocation = getLocation();
        VenuesCriteria venuesCriteria = new VenuesCriteria();
        venuesCriteria.setRadius(250);
        venuesCriteria.setLocation(currentLocation);

        mAsync.getVenuesNearby(new FoursquareVenuesRequestListener() {

            @Override
            public void onVenuesFetched(ArrayList<Venue> venues) {
                Log.d(TAG, "Nearby venus count " + venues.size());
            }

            @Override
            public void onError(String errorMsg) {

            }

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

}
