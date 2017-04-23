package InMind.Server.SignalInfo;

import InMind.Consts;

import java.util.LinkedList;
import java.util.Optional;
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
    final int minimalTalk = Consts.sampleRate / 1000;//0.01 //10000; //require at least 0.001 sec of speech

    double considerSilent()
    {
        //return Math.min(minimalSoundSumInPast10 * factorOnLowestForSilent + addForSilence, considerSpeech());
        return minimalSoundSumInPast10 * factorOnLowestForSilent + addForSilence;
    }
    double considerSpeech()
    {
        //return minimalSoundSumInPast10 + addForSpeech;
        return minimalSoundSumInPast10 * factorOnLowestForSpeech + addForSpeech;
    }

    final double factorOnLowestForSilent = 1.4;//1.8;//1.4; //consider silence upto this factor
    final double factorOnLowestForSpeech = 1.45; //consider silence upto this factor
    final int lowPassFilterN = 500;
    final int addForSpeech = 40 * lowPassFilterN; //30//40
    final int addForSilence = 15 * lowPassFilterN;
    final int minimalSoundStartingPoint = Integer.MAX_VALUE;
    final int minimalSoundLenN = lowPassFilterN*10;

    int minimalSoundSumInPast10 = minimalSoundStartingPoint;
    Queue<Integer> previousSound;
    Queue<Integer> previousSoundSum10;
    int previousSoundSum = 0;

    @Override
    public void startNewStream()
    {
        bytesSilentAtEnd = 0;
        bytesTalkAtCurrentSample = 0;
        bytesTotalTalkLength = 0;
        totalTimeFromStart = 0;
        previousSoundSum = 0;
        minimalSoundSumInPast10 = minimalSoundStartingPoint;
        previousSound = new LinkedList<>();
        previousSoundSum10 = new LinkedList<>();
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
        signalInfo.vad = 0;
        if (bytesTalkAtCurrentSample > minimalTalk) // && totalTimeFromStart > 1200)
            signalInfo.vad = 1;
        //System.out.println("bytesTalkAtCurrentSample: " + bytesTalkAtCurrentSample + " vad: " + signalInfo.vad + " totalTimeFromStart: " + totalTimeFromStart); //DEBUG!!!
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
                previousSoundSum10.add(previousSoundSum);
            }
            if (previousSoundSum10.size() > minimalSoundLenN)
            {
                previousSoundSum10.poll();
                if (i % 100 == 0)  //TODO: bad performance, build more efficient queue!!! for now only performing this test every 100
                {
                    Optional<Integer> minval = previousSoundSum10.stream().min((a, b) -> a.equals(b) ? 0 : (a < b ? -1 : 1));
                    if (minval.isPresent())
                        minimalSoundSumInPast10 = minval.get();
                }
//                if (previousSoundSum10.poll() <= minimalSoundSumInPast10)
//                {
//                    //Optional<Integer> minval = previousSoundSum10.stream().min((a, b) -> a.equals(b) ? 0:(a<b?-1:1));
//                    //if (minval.isPresent())
//                        //minimalSoundSumInPast10 = minval.get();
//                    minimalSoundSumInPast10 = previousSoundSum; //reset to current sound, will find new minimum
//                }
            }
            // don't start updating and counting sound until we filled-up our lowPassFilterN
            if (previousSound.size() >= lowPassFilterN)
            {
                //update silence level,
                if (previousSoundSum < minimalSoundSumInPast10)
                        //&& bytesSilentAtEnd < maximalSilenceAtEndToUpdateMin) but only if didn't detect already a lot of silence.
                {
                    minimalSoundSumInPast10 = previousSoundSum;
                }

//                if (i % 100 == 0) //DEBUG!!!!
//                {
//                    System.out.println(" debug, minimalSoundSumInPast10: " + minimalSoundSumInPast10 + " .. " + previousSound.size()); //DEBUG!!!
//                    System.out.print(" bytesSilentAtEnd: " + bytesSilentAtEnd + " previousSoundSum:" + previousSoundSum); //DEBUG!!!
//                    Optional<Integer> minval = previousSoundSum10.stream().min((a, b) -> a.equals(b) ? 0:(a<b?-1:1));
//                    if (minval.isPresent())
//                        System.out.print(" minval: " + minval.get()); //DEBUG!!!
//                }

                if (previousSoundSum < considerSilent())
                {
                    bytesSilentAtEnd++;
                    //System.out.print(" bytesSilentAtEnd: " + bytesSilentAtEnd); //DEBUG!!!
                }
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
