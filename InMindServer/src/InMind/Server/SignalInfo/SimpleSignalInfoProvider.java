package InMind.Server.SignalInfo;

import InMind.Consts;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Amos on 17-Feb-15.
 *
 * should be replaced by sphinx. Sphinx didn't work that well.
 * Updated and works nicely!
 * Applies a low-pass filter (moving average) of length lowPassFilterN.
 * Tracks the minimal sound thus far and uses it as a guide to determine speech vs silence.
 */
public class SimpleSignalInfoProvider extends ASignalInfoProvider
{
    int bytesSilentAtEnd = 0;
    int bytesTalkAtCurrentSample = 0;
    int bytesTotalTalkLength = 0;
    double totalTimeFromStart = 0; //not in use?


    //final int silentLengthNeeded = 500;  //in milliseconds
    //final int considerSilent = 100;//200;//500;//2500;
    //final int considerSpeech = 200;//400;//1000;//3500;//3000;
    final int minimalTalk = Consts.sampleRate / 10000; //require at least 0.001 sec of speech

    double considerSilent()
    {
        //return Math.min(minimalSoundSumUntilNow * factorOnLowest + addForSilence, considerSpeech());
        return minimalSoundSumUntilNow * factorOnLowest + addForSilence;
    }
    double considerSpeech()
    {
        //return minimalSoundSumUntilNow + addForSpeech;
        return considerSilent() + addForSpeech;
    }

    final double factorOnLowest = 1.8;//1.4; //consider silence upto this factor
    final int lowPassFilterN = 500;
    final int addForSpeech = 30 * lowPassFilterN; //40
    final int addForSilence = 15 * lowPassFilterN;
    final int minimalSoundStartingPoint = 1000*lowPassFilterN;
    final int maximalSilenceAtEndToUpdateMin = 37;

    int minimalSoundSumUntilNow = minimalSoundStartingPoint;
    Queue<Integer> previousSound;
    int previousSoundSum = 0;

    @Override
    public void startNewStream()
    {
        bytesSilentAtEnd = 0;
        bytesTalkAtCurrentSample = 0;
        bytesTotalTalkLength = 0;
        totalTimeFromStart = 0;
        previousSoundSum = 0;
        minimalSoundSumUntilNow = minimalSoundStartingPoint;
        previousSound = new LinkedList<>();
    }

    @Override
    public void endStream()
    {

    }

    @Override
    public SignalInfo obtainSampleInfo(byte[] currentSample, int currLength)
    {
        SignalInfo signalInfo = new SignalInfo();
        updateTalkAndSilent(currentSample, currLength);

        signalInfo.finalPause = convertToMilliSeconds(bytesSilentAtEnd);
        signalInfo.vad = (bytesTalkAtCurrentSample > minimalTalk) ? 1 : 0;
        signalInfo.offSetFromFirst = totalTimeFromStart;
        signalInfo.sampleLength = currLength * 1000.0 / Consts.sampleRate;
        totalTimeFromStart += signalInfo.sampleLength;

        //System.out.println("vad:" + signalInfo.vad + ", finalPause:" + signalInfo.finalPause + ", sampleLength: " + signalInfo.sampleLength + ", offSetFromFirst:" + signalInfo.offSetFromFirst);

        return signalInfo;
    }

    protected void updateTalkAndSilent(byte[] currentSample, int currLength)
    {
        bytesTalkAtCurrentSample = 0;
        for (int i = 0; 2 * i < currLength; i++)
        {
            short sample = (short) (currentSample[2 * i + 1] << 8 | currentSample[2 * i]); //little endian 16bit

            previousSound.add(Math.abs(sample));
            previousSoundSum += Math.abs(sample);
            if (previousSound.size() > lowPassFilterN)
            {
                previousSoundSum -= previousSound.poll();
            }
            // don't start updating and counting sound until we filled-up our lowPassFilterN
            if (previousSound.size() >= lowPassFilterN)
            {
                //update silence level,
                if (previousSoundSum < minimalSoundSumUntilNow)
                        //&& bytesSilentAtEnd < maximalSilenceAtEndToUpdateMin) but only if didn't detect already a lot of silence.
                {
                    minimalSoundSumUntilNow = previousSoundSum;
                }

                if (Math.abs(previousSoundSum) < considerSilent())
                    bytesSilentAtEnd++;
                else
                {
                    bytesSilentAtEnd = 0;
                    if (Math.abs(previousSoundSum) > considerSpeech())
                    {
                        bytesTotalTalkLength++;
                        bytesTalkAtCurrentSample++;
                    }
                }
            }
        }

    }

//    public boolean isSilentButDidTalk(byte[] asByte)
//    {
//        try
//        {
//            updateTalkAndSilent(asByte);
//            double silentLength = bytesSilentAtEnd / (double) Consts.sampleRate;
//            if (silentLength * 1000 > silentLengthNeeded && bytesTotalTalkLength > minimalTalk)
//                return true;
//
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return false;
//
//    }
}
