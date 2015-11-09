package com.yahoo.inmind.sensors.generic.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.yahoo.inmind.comm.generic.control.MessageBroker;
import com.yahoo.inmind.services.generic.control.AwareServiceWrapper;

import java.util.ArrayList;

/**
 * Created by oscarr on 8/3/15.
 */
public abstract class SensorObserver extends ContentObserver{
    protected Context mContext;
    protected Uri mUri;
    protected MessageBroker mb;
    protected ArrayList<BroadcastReceiver> receivers;
    protected ArrayList<String> actions;
    protected String name;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public SensorObserver(Handler handler, Context context) {
        super(handler);
        mContext = context;
        mb = MessageBroker.getInstance( mContext );
        receivers = new ArrayList<>();
        actions = new ArrayList<>();
    }

    public void register(BroadcastReceiver receiver, ArrayList<String> actions){
        IntentFilter contextFilter = new IntentFilter();
        mContext.getContentResolver().registerContentObserver( mUri, true, this);
        for (String action : actions) {
            contextFilter.addAction(action);
        }
        mContext.registerReceiver(receiver, contextFilter);
        receivers.add( receiver );
    }

    public void unregister(BroadcastReceiver receiver){
        try {
            mContext.getContentResolver().unregisterContentObserver(this);
            mContext.unregisterReceiver(receiver);
            receivers.remove(receiver);
        }catch (IllegalArgumentException e){
            //do nothing
        }
    }

    public void unregister(){
        for(BroadcastReceiver receiver : receivers ){
            unregister(receiver);
        }
        receivers.clear();
    }

    public void startListening(){
        AwareServiceWrapper.setSetting(mContext, name, true);
        AwareServiceWrapper.startSensor(mContext, name);
    }

    public void stopListening(){
        AwareServiceWrapper.setSetting(mContext, name, false);
        AwareServiceWrapper.stopSensor(mContext, name);
    }

    public ArrayList<String> getActions() {
        return actions;
    }
}
