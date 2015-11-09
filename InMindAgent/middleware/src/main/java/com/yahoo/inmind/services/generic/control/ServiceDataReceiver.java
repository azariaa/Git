package com.yahoo.inmind.services.generic.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by oscarr on 8/13/15.
 */
public class ServiceDataReceiver extends BroadcastReceiver {
    private Context mContext;

    public ServiceDataReceiver(Context context){
        mContext = context;
    }

    public ServiceDataReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("", "Inside ServiceDataReceiver.onReceive");
        Log.d("", "Context: " + context);
        Log.d("", "Intent: " + intent.getAction());
    }
}
