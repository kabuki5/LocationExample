package com.kabuki.location.locationexample.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kabuki.location.locationexample.R;
import com.kabuki.location.locationexample.services.MyLocationService;

/**
 * Created by Kabuki on 29/12/2016.
 * Optimus Mobile Engineering
 */

public class ResurrectServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(context.getString(R.string.action_resurrect))){
            context.startService(new Intent(context, MyLocationService.class));
        }
    }
}
