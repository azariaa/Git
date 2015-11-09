package com.yahoo.inmind.services.generic.control;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yahoo.inmind.comm.generic.control.MessageBroker;
import com.yahoo.inmind.commons.control.Constants;
import com.yahoo.inmind.commons.control.Util;
import com.yahoo.inmind.commons.rules.model.DecisionRule;
import com.yahoo.inmind.effectors.alarm.control.AlarmEffector;
import com.yahoo.inmind.effectors.generic.control.EffectorDataReceiver;
import com.yahoo.inmind.effectors.generic.control.EffectorObserver;
import com.yahoo.inmind.effectors.sms.control.SmsEffector;
import com.yahoo.inmind.sensors.accelerometer.control.AccelerometerObserver;
import com.yahoo.inmind.sensors.generic.control.SensorDataReceiver;
import com.yahoo.inmind.sensors.generic.control.SensorObserver;
import com.yahoo.inmind.services.activity.control.ActivityRecognitionService;
import com.yahoo.inmind.services.booking.control.HotelReservationService;
import com.yahoo.inmind.services.calendar.control.CalendarService;
import com.yahoo.inmind.services.location.control.LocationService;
import com.yahoo.inmind.services.news.control.NewsService;
import com.yahoo.inmind.services.privacy.control.PrivacyService;
import com.yahoo.inmind.services.streaming.control.StreamingService;
import com.yahoo.inmind.services.weather.control.WeatherService;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * The service locator pattern is a design pattern used in software development to encapsulate the
 * processes involved in obtaining a service with a strong abstraction layer. This pattern uses a
 * central registry known as the "service locator", which on request returns the information
 * necessary to perform a certain task.
 * @see @link https://msdn.microsoft.com/en-us/library/ff648968.aspx
 *
 * Created by oscarr on 8/10/15.
 */
public class ResourceLocator {

    private ConcurrentHashMap<Class, MiddServiceConnection> mServicesHash;
    private ConcurrentHashMap<Class, SensorObserver>  mSensorsHash;
    private ConcurrentHashMap<Class, EffectorObserver> mEffectorsHash;
    private ConcurrentHashMap<String, DecisionRule> mDecisionRulesHash;
    private SensorDataReceiver sensorDataReceiver;
    private ServiceDataReceiver serviceDataReceiver;
    private EffectorDataReceiver effectorDataReceiver;
    private ArrayList<String> actionsServiceReceiver;
    private ArrayList<String> actionsSensorReceiver;
    private ArrayList<String> actionsEffectorReceiver;
    private static ResourceLocator resourceLocator;
    private static Context mContext;
    /** Tracking list of started activities **/
    private ArrayList<Class> mClassActivitiesList = new ArrayList<>();
    private ArrayList<Activity> mActivitiesList = new ArrayList<>();

    private ResourceLocator(Context context) {
        mServicesHash = new ConcurrentHashMap<>();
        mSensorsHash = new ConcurrentHashMap<>();
        mEffectorsHash = new ConcurrentHashMap<>();
        mDecisionRulesHash = new ConcurrentHashMap<>();
        mContext = context;
        sensorDataReceiver = new SensorDataReceiver(mContext);
        serviceDataReceiver = new ServiceDataReceiver(mContext);
        effectorDataReceiver = new EffectorDataReceiver(mContext);
        actionsServiceReceiver = new ArrayList<>();
        actionsSensorReceiver = new ArrayList<>();
        actionsEffectorReceiver = new ArrayList<>();
    }

    public static ResourceLocator getInstance(Context context) {
        if (resourceLocator == null) {
            resourceLocator = new ResourceLocator( context );
        }
        return resourceLocator;
    }

    @Nullable
    public static ResourceLocator getExistingInstance() {
        return resourceLocator;
    }


    /**
     * This method has been replaced by lookupService
     * @param service
     * @param <T>
     * @return
     */
    @Nullable
    @Deprecated
    public <T extends GenericService> T getService(Class<T> service){
        return lookupService(service);
    }


    public <T extends GenericService> T lookupService( Class<T> service ){
        MiddServiceConnection connection;
        if( mServicesHash != null && (connection = mServicesHash.get(service)) != null) {
            while( connection.getService() == null && !connection.isMaxAttempts( ) ){
                Util.sleep( 100 );
            }
            return (T) connection.getService();
        }
        addService( service );
        return null;
    }

    @Nullable
    public  <T extends SensorObserver> T lookupSensor(Class<T> sensor){
        if( mSensorsHash != null ) {
            return (T) mSensorsHash.get(sensor);
        }
        return Util.createInstance(sensor);
    }

    @Nullable
    public  <T extends EffectorObserver> T lookupEffector(Class<T> effector){
        if( mEffectorsHash != null ) {
            return (T) mEffectorsHash.get(effector);
        }
        return Util.createInstance(effector);
    }

    @Nullable
    public DecisionRule getDecisionRule(String ruleID){
        if( mDecisionRulesHash != null ) {
            return mDecisionRulesHash.get( ruleID );
        }
        return null;
    }

    /**
     * It returns the rule id (if not given by the user, it will be generated automatically).
     * @param decisionRule
     * @return
     */
    public String addDecisionRule(DecisionRule decisionRule){
        mDecisionRulesHash.put(decisionRule.getRuleID(), decisionRule);
        return decisionRule.getRuleID();
    }

    public boolean removeDecisionRule(DecisionRule decisionRule){
        if( decisionRule != null ) {
            Object removed = mDecisionRulesHash.remove(decisionRule.getRuleID());
            decisionRule.destroy();
            return removed != null;
        }
        return false;
    }

    /**
     * We add all the Middleware services here.
     */
    public void addServices(){
        Log.d("ResourceLocator", "addServices");
        mServicesHash.put( PrivacyService.class, new MiddServiceConnection() );
        mServicesHash.put( NewsService.class, new MiddServiceConnection() );
        mServicesHash.put( AwareServiceWrapper.class, new MiddServiceConnection() );
//        mServicesHash.put( ActivityRecognitionService.class, new MiddServiceConnection() );
//        mServicesHash.put( HotelReservationService.class, new MiddServiceConnection() );
//        mServicesHash.put( LocationService.class, new MiddServiceConnection() );
//        mServicesHash.put( WeatherService.class, new MiddServiceConnection());
//        mServicesHash.put( CalendarService.class, new MiddServiceConnection());
//        mServicesHash.put( StreamingService.class, new MiddServiceConnection());

        // add your service here:

    }

    public void addService( Class service ){
        mServicesHash.put( service, new MiddServiceConnection() );
        bindService( service );
    }

    //FIXME
    public void addSensors(){
        mSensorsHash.put( AccelerometerObserver.class, new AccelerometerObserver( new Handler(), mContext ) );
        //... add all the sensors here

        for(SensorObserver observer : mSensorsHash.values() ){
            registerSensor(observer);
            //observer.startListening();
        }
    }

    public void addEffectors(){
        mEffectorsHash.put( AlarmEffector.class, new AlarmEffector( new Handler(), mContext ) );
        mEffectorsHash.put( SmsEffector.class, new SmsEffector( new Handler(), mContext ) );
        //... add all the effectors here

        for(EffectorObserver observer : mEffectorsHash.values() ){
            registerEffector(observer);
        }
    }

    //TODO: finish it
    public void startSensor(String name){
        if( name.equals(Constants.SENSOR_ACCELEROMETER )){
            lookupSensor(AccelerometerObserver.class).startListening();
        }
    }

    //TODO: finish it
    public void stopSensor(String name){
        if( name.equals(Constants.SENSOR_ACCELEROMETER )){
            lookupSensor(AccelerometerObserver.class).stopListening();
        }
    }


    private void registerSensor(SensorObserver sensorObserver){
        sensorObserver.unregister(sensorDataReceiver);
        ArrayList<String> actions = sensorObserver.getActions();
        for(String action : actions ){
            if( !actionsSensorReceiver.contains(action) ){
                actionsSensorReceiver.add(action);
            }
        }
        sensorObserver.register( sensorDataReceiver, actionsSensorReceiver );
    }

    private void registerEffector(EffectorObserver effectorObserver){
        effectorObserver.unregister( effectorDataReceiver );
        ArrayList<String> actions = effectorObserver.getActions();
        for(String action : actions ){
            if( !actionsEffectorReceiver.contains(action) ){
                actionsEffectorReceiver.add(action);
            }
        }
        effectorObserver.register( effectorDataReceiver, actionsEffectorReceiver );
    }

    private void registerService(GenericService service){
        ArrayList<String> actions = service.getActions();
        if( !actions.isEmpty() ) {
            service.unregister(serviceDataReceiver);
            for (String action : actions) {
                if (!actionsServiceReceiver.contains(action)) {
                    actionsServiceReceiver.add(action);
                }
            }
            service.register(serviceDataReceiver, actionsServiceReceiver);
        }
    }

    public void bindServices(){
        for(final Class service : mServicesHash.keySet()) {
            bindService( service );
        }
    }

    public void bindService( Class service ){
        try {
            Intent intentService = new Intent(mContext, service);
//            mContext.startService(intentService);
            MiddServiceConnection connection = mServicesHash.get(service);
            mContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE);
            mServicesHash.put( service, connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void destroy(){
        for( MiddServiceConnection conn : mServicesHash.values() ){
            if( conn != null && conn.mService != null ) {
                conn.mService.onDestroy();
                // Unbind from the service
                if (conn.mServiceBound) {
                    mContext.unbindService(conn);
                    conn.mServiceBound = false;
                }
                conn.mService = null;
            }
        }
        for( SensorObserver sensorObserver : mSensorsHash.values() ){
            sensorObserver.stopListening();
            sensorObserver.unregister();
        }
        for( EffectorObserver effectorObserver : mEffectorsHash.values() ){
            effectorObserver.unregister();
        }
        mDecisionRulesHash = null;
        mServicesHash = null;
        mSensorsHash = null;
        mEffectorsHash = null;
        mContext = null;
        sensorDataReceiver = null;
        serviceDataReceiver = null;
        actionsSensorReceiver = null;
        actionsServiceReceiver = null;
        mClassActivitiesList = null;
        mActivitiesList = null;
        System.gc();
    }

    public void addActivity(Object subscriber) {
        if( subscriber instanceof Activity && ( mActivitiesList.isEmpty() ||
            !mActivitiesList.get( mActivitiesList.size() -1 ).equals( subscriber ) ) ){
            mActivitiesList.add((Activity) subscriber);    
        }
    }

    public void addActivityClass(Class clazz) {
        mClassActivitiesList.add( clazz );
    }

    @Nullable
    public Activity getTopActivity(){
        if( !mActivitiesList.isEmpty() ){
            return mActivitiesList.get( mActivitiesList.size() - 1 );
        }
        return null;
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public static class MiddServiceBinder extends Binder {
        private GenericService mService;

        public void setService(GenericService mService) {
            this.mService = mService;
        }

        public GenericService getService() {
            // Return this instance of Service so clients can call public methods
            return this.mService;
        }
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private static class MiddServiceConnection implements ServiceConnection {
        private MiddServiceBinder mBinder;
        private Boolean mServiceBound;
        private GenericService mService;
        private int numberAttempts = 0;
        static final int MAX_NUM_ATTEMPTS = 10;

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to Service, cast the IBinder and get Service instance
            mBinder = (MiddServiceBinder) service;
            mService = mBinder.getService();
            mService.doAfterBind();
            resourceLocator.registerService( mService );
            mServiceBound = true;
            MessageBroker.getInstance( mContext ).processRequests();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }


        public GenericService getService() {
            if( mService == null ){
                numberAttempts++;
            }else{
                numberAttempts = 0;
            }
            return mService;
        }

        public boolean isMaxAttempts( ) {
            return numberAttempts >= MAX_NUM_ATTEMPTS;
        }
    }
}
