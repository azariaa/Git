package InMind.Server.interactionManager;

import InMind.Server.SignalInfo.SimpleSignalInfoProvider;

/**
 * Created by Amos Azaria on 28-Oct-15.
 */
public class InteractionManager extends AInteractionManager
{
    public InteractionManager(IIMRequiredAction imRequiredAction)
    {
        super(imRequiredAction);
    }

    final double dMaxUttDuration = 15000;//30000;
    final double dMinUttDuration = 3000;//1700;//1200;//500;//1000;
    //final double dActionThreshold = 300;
    //final double dListenThreshold = 1000;
    final double dStopSpeakingThreshold = 320; //want to usually call Google only once, so increased required silence at end, was: 250;//150;
    final double dMinVadDuration = 200;
    final double dMinAdditionalVad = 400; //will usually go to Google only once and not cancel// was: 100;
    final int maxGotoGoogle = 2;

    enum InternalState {init, waitForASR, complete};

    double dCurrentDuration = 0; //in milliseconds
    double dTotalVad = 0; //in milliseconds
    double dVadWhenInvokeASR = 0;
    InternalState internalState = InternalState.init;
    int wentToGoogle = 0;

    @Override
    public void updatedAudioInfo(SimpleSignalInfoProvider.SignalInfo signalInfo)
    {
        dCurrentDuration += signalInfo.sampleLength;
        dTotalVad += signalInfo.sampleLength*signalInfo.vad;

        if (dCurrentDuration>=dMaxUttDuration) //if utterance is too long, we must end
        {
            goGetASRIfInitState();
        }
        else if(dCurrentDuration>dMinUttDuration && dTotalVad > dMinVadDuration && signalInfo.finalPause > dStopSpeakingThreshold) //enough speech and pause to initiate ASR call
        {
            goGetASRIfInitState();
        }
        else if(dTotalVad >= dVadWhenInvokeASR + dMinAdditionalVad) //if the user talked dMinAdditionalVad or more than when sent the ASR call, cancel call
        {
            if (internalState == InternalState.waitForASR && wentToGoogle <= maxGotoGoogle) //unless already complete (or there is no call, but that shouldn't happen)
            {
                internalState = InternalState.init;
                imRequiredAction.takeAction(ActionToTake.cancel);
            }
        }
    }

    private void goGetASRIfInitState()
    {
        if (internalState == InternalState.init)
        {
            internalState = InternalState.waitForASR;
            dVadWhenInvokeASR = dTotalVad;
            imRequiredAction.takeAction(ActionToTake.goToGoogle);
            wentToGoogle++;
        }
    }

    @Override
    public void start()
    {
        initialize();
    }

    private void initialize()
    {
        dCurrentDuration = 0; //in milliseconds
        dTotalVad = 0; //in milliseconds
        dVadWhenInvokeASR = 0;
        internalState = InternalState.init;
        wentToGoogle = 0;
    }

    @Override
    public void stop()
    {
        initialize();
    }

    @Override
    public void finalResponseObtained()
    {
        internalState = InternalState.complete;
        imRequiredAction.takeAction(ActionToTake.moveOn);
        imRequiredAction.takeAction(ActionToTake.commit);
    }

    @Override
    public void userStoppedStreaming()
    {
        //if user stopped streaming, so there is no need to consider finalPause
        if (dCurrentDuration > dMinUttDuration && dTotalVad > dMinVadDuration)
            goGetASRIfInitState();
        initialize();
    }
}
