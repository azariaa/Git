package InMind.Server.SignalInfo;

import InMind.Consts;

/**
 * Created by Amos on 17-Feb-15.
 *
 * should be replaced by sphinx
 */
public class SimpleSignalInfoProvider extends ASignalInfoProvider
{
    int bytesSilentAtEnd = 0;
    int bytesTalkAtCurrentSample = 0;
    int bytesTotalTalkLength = 0;
    double totalTimeFromStart = 0; //not in use?


    //final int silentLengthNeeded = 500;  //in milliseconds
    final int considerSilent = 200;//500;//2500;
    final int considerSpeech = 400;//1000;//3500;//3000;
    final int minimalTalk = Consts.sampleRate / 10000; //require at least 0.001 sec of speech


    @Override
    public void startNewStream()
    {
        bytesSilentAtEnd = 0;
        bytesTalkAtCurrentSample = 0;
        bytesTotalTalkLength = 0;
        totalTimeFromStart = 0;
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
            if (Math.abs(sample) < considerSilent)
                bytesSilentAtEnd++;
            else
            {
                bytesSilentAtEnd = 0;
                if (Math.abs(sample) > considerSpeech)
                {
                    bytesTotalTalkLength++;
                    bytesTalkAtCurrentSample++;
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
