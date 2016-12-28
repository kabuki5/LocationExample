package com.kabuki.location.locationexample.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.kabuki.location.locationexample.MainActivity;

/**
 * Created by Kabuki on 28/12/2016.
 * Optimus Mobile Engineering
 */

public class MyLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final int MESSAGE_CHANGE_PRIORITY_AND_INTERVAL = 100;
    public static final int MESSAGE_ON_LOCATION_CHANGE = 101;
    public static final int BIND_RESULT_RECEIVER = 102;

    public static final String RESULT_RECEIVER = "receiver";

    private Messenger mMessenger = new Messenger(new IncomingHandler());

    private GoogleApiClient mGoogleApiClient;

    private MainActivity.ServiceIncomingReceiver mResultReceiver;
    private LocationRequest mLocationRequest;

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

//    GOOGLE API CLIENT CALLBACKS

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        sendResult(lastLocation);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
//        mLocationRequest.setSmallestDisplacement()
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //    SEND NEW lOCATION TO ACTIVITY
    @Override
    public void onLocationChanged(Location location) {
        sendResult(location);
    }

    private void sendResult(Location location) {
        if (mResultReceiver != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("location", location);
            bundle.putString("info",getInfo());
            mResultReceiver.send(MESSAGE_ON_LOCATION_CHANGE, bundle);
        }
    }


    // INCOMING HANDLER CLASS
    private final class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_CHANGE_PRIORITY_AND_INTERVAL:
                    int priority = msg.arg1;
                    int interval = msg.arg2;
                    swapLocationRequestPriorityAndInterval(priority, interval);
                    break;
                case BIND_RESULT_RECEIVER:
                    Bundle bundle = msg.getData();
                    mResultReceiver = bundle.getParcelable(RESULT_RECEIVER);
                    break;
            }


        }
    }


    private void swapLocationRequestPriorityAndInterval(int priority, int interval) {
        if (!mGoogleApiClient.isConnected())
            return;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        mLocationRequest.setPriority(priority);
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(interval);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private String getInfo(){
        if(mLocationRequest==null)
            return "";

        int interval = (int) mLocationRequest.getFastestInterval();
        int priority = mLocationRequest.getPriority();
        String p ="";
        if(priority == LocationRequest.PRIORITY_HIGH_ACCURACY)
            p = "HIGH ACCURACY";
        else if(priority == LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            p= "BALANCED POWER ACCURACY";

        return "Priority: " +p+"\nInterval: "+String.valueOf(interval/1000)+" seconds";
    }


}
