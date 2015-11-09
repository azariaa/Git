package com.yahoo.inmind.sensors.accelerometer.model;

import com.yahoo.inmind.comm.accelerometer.model.AccelerometerEvent;
import com.yahoo.inmind.comm.generic.control.MessageBroker;
import com.yahoo.inmind.comm.generic.model.MBRequest;
import com.yahoo.inmind.commons.control.Constants;
import com.yahoo.inmind.commons.rules.model.DecisionRule;
import com.yahoo.inmind.commons.rules.model.PropositionalStatement;
import com.yahoo.inmind.sensors.accelerometer.control.AccelerometerObserver;
import com.yahoo.inmind.services.generic.control.ResourceLocator;

import java.util.ArrayList;

/**
 * Created by oscarr on 10/14/15.
 */
public class AccelerometerProposition extends PropositionalStatement {

    /**
     * We need this constructor for json deserialization
     */
    public AccelerometerProposition(){
        super();
        componentName = Constants.ACCELEROMETER;
    }

    public AccelerometerProposition(String attribute, String operator, String value){
        super( attribute, operator, value );
        componentName = Constants.ACCELEROMETER;
        subscribe();
    }

    @Override
    public void subscribe(){
        super.subscribe();
        startAccelerometer();
    }

    @Override
    public void unsubscribe(){
        stopAccelerometer();
        super.unsubscribe();
    }

    @Override
    public ArrayList validate() {
        return getList(ResourceLocator.getExistingInstance().lookupSensor(AccelerometerObserver.class)
                .getAccEvent());
    }


    private void startAccelerometer(){
        ResourceLocator.getExistingInstance().lookupSensor(AccelerometerObserver.class).startListening();
    }

    private void stopAccelerometer(){
        ResourceLocator.getExistingInstance().lookupSensor(AccelerometerObserver.class).stopListening();
    }

    @Override
    public Object validate(Object objValue) {
        try {
            AccelerometerEvent event = (AccelerometerEvent) objValue;
            double valueDouble, attributeDouble = 0;
            if (attribute.equals(Constants.ACCELEROMETER_X_AXIS)) {
                attributeDouble = event.getAccelerationX();
            } else if (attribute.equals(Constants.ACCELEROMETER_Y_AXIS)) {
                attributeDouble = event.getAccelerationY();
            } else if (attribute.equals(Constants.ACCELEROMETER_Z_AXIS)) {
                attributeDouble = event.getAccelerationZ();
            } else if (attribute.equals(Constants.ACCELEROMETER_VECTOR_SUM)) {
                attributeDouble = Math.sqrt(Math.pow(event.getAccelerationX(), 2) + Math.pow(
                        event.getAccelerationY(), 2) + Math.pow(event.getAccelerationZ(), 2));
            } else if (attribute.equals(Constants.ACCELEROMETER_ACCURACY)) {
                attributeDouble = event.getAccuracy();
            }
            valueDouble = Double.valueOf(value);
            if( validateNumbers(attributeDouble, valueDouble) ){
                return event;
            }
        }catch(Exception e){
            //do nothing
        }
        return null;
    }


    public void onEvent(AccelerometerEvent event){
        if( rules != null ){
            Object result = validate( event );
            for (DecisionRule rule : rules) {
                rule.setPropositionFlag( this, result != null, null );
            }
        }
    }
}
