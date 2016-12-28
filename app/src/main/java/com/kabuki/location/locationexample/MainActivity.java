package com.kabuki.location.locationexample;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kabuki.location.locationexample.services.MyLocationService;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Messenger mMessenger;

    private boolean mBound;

    private ServiceIncomingReceiver mReceiver;

    private GoogleMap mMap;

    private EditText intervalEdt;

    private TextView mInfoTev;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            mMessenger = new Messenger(service);
            bindResultReceiver();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mMessenger = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null)
            mapFragment.getMapAsync(this);
        mInfoTev = (TextView) findViewById(R.id.info_tev);
        intervalEdt = (EditText) findViewById(R.id.edit_interval);
        intervalEdt.setText("0");

        startService(new Intent(this, MyLocationService.class));
    }

    // SEND MESSAGE TO SERVICE
    private void changeLocationRequestPriority(int locationRequestPriority, int interval) {
        if (!mBound)
            return;

        Message message = Message.obtain(null, MyLocationService.MESSAGE_CHANGE_PRIORITY_AND_INTERVAL, locationRequestPriority, interval);
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void bindResultReceiver() {
        if (!mBound)
            return;
        mReceiver = new ServiceIncomingReceiver(null);
        Message message = Message.obtain();
        message.what = MyLocationService.BIND_RESULT_RECEIVER;
        Bundle bundle = new Bundle();
        bundle.putParcelable(MyLocationService.RESULT_RECEIVER, mReceiver);
        message.setData(bundle);
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //   BIND / UNBIND SERVICE
    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, MyLocationService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    //    MAP READY CALLBACK
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public void onButtonClick(View view) {
        int id = view.getId();
        int interval = Integer.parseInt(intervalEdt.getText().toString());
        interval = interval * 1000; //SECONDS
        if (interval < 1000)
            interval = 1000; //minimum is 1 Sec
        switch (id) {
            case R.id.high:
                changeLocationRequestPriority(LocationRequest.PRIORITY_HIGH_ACCURACY, interval);
                break;
            case R.id.balanced:
                changeLocationRequestPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, interval);
                break;
        }
    }


    //    INCOMING RECEIVER
    public class ServiceIncomingReceiver extends ResultReceiver implements Parcelable {

        public ServiceIncomingReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            switch (resultCode) {
                case MyLocationService.MESSAGE_ON_LOCATION_CHANGE:
//               Update map position and marker
                    Location location = resultData.getParcelable("location");
                    String info = resultData.getString("info");
                    if (mMap != null && location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng));
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(latLng, 15)));
                    }
                    mInfoTev.setText(info);
                    break;
            }

        }
    }
}
