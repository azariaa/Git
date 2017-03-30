package InMind.Server.SignalInfo;

import InMind.Consts;

/**
 * Created by User on 09-Mar-15.
 */
public abstract class ASignalInfoProvider
{
    public class SignalInfo
    {
        public int vad; //was their noise in this sample
        public double finalPause; //in milliseconds
        public double offSetFromFirst; //in milliseconds //seems to not be in use right now
        public double sampleLength; //in milliseconds
    }

    abstract public void startNewStream();
    abstract public void endStream();
    abstract public SignalInfo obtainSampleInfo(byte[] currentSample, int currLength);//, byte[] fullSample, int fullLength);


    protected double convertToMilliSeconds(int bytesOfSomething)
    {
        return bytesOfSomething*1000.0 / Consts.sampleRate;
    }
}
