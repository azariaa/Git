package com.yahoo.inmind.services.privacy.control;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.yahoo.inmind.comm.generic.model.MBRequest;
import com.yahoo.inmind.commons.control.Constants;
import com.yahoo.inmind.services.generic.control.GenericService;
import com.yahoo.inmind.services.streaming.control.Stream;

public class PrivacyService extends GenericService {
    public PrivacyService() {
        super( null );
        Log.d("PrivacyService", "PrivacyService constructor-End");
    }

    @Override
    public void doAfterBind() {
        super.doAfterBind();
        // here is your code...
        // ...
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = super.onBind( intent );
        // here is your code:
        // ...
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    /**
     *  This method will grant or deny permissions to the caller to access a resource. Also, it will
     *  logs and monitors the flow of messages between InMind components.
     *
     * @param caller    this is the object who calls the MessageBroker's method and is trying to
     *                  access the resource
     * @resource        a resource can be the name of a class or file (e.g., xml); the name of a
     *                  service, sensor, or effector; or a MBRequest identifier (i.e., MSG_XXX_XX...)
     * @param mbMethod  this is the MessageBroker's method that was called by caller (send,
     *                  sendAndReceive, get, post, postSticky)
     * @return
     */
    public boolean checkPermissions( Object caller, Object resource, String mbMethod ){
        String callerStr = extractCaller(caller);
        String resourceStr = extractResource(resource);
        Log.d("", "Caller class: " + callerStr + "  resource: " + resourceStr + "  and method: "
                + mbMethod );

        // check privacy
        // .....

        // monitoring and logging
        // ...

        return true;
    }

    /**
     * This method extracts the caller
     * @param caller
     * @return
     */
    private String extractCaller(Object caller) {
        if( caller == null )
            throw new NullPointerException("Caller must not be null!");
        return caller instanceof Class? ((Class) caller).getCanonicalName()
                : caller.getClass().getCanonicalName();
    }

    /**
     * This method extracts the resource
     * @param resource
     * @return
     */
    private String extractResource(Object resource){
        if( resource == null )
            throw new NullPointerException("Resource must not be null!");
        return resource instanceof String? (String) resource
                : resource instanceof MBRequest? Constants.getID(((MBRequest) resource).getRequestId())
                : resource instanceof Class? ((Class) resource).getCanonicalName()
                : resource.getClass().getCanonicalName();
    }
}
