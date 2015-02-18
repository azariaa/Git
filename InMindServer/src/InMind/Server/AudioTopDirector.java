package InMind.Server;

import com.sun.xml.internal.ws.util.ByteArrayBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static InMind.Utils.appendToFile;
import static InMind.Utils.delIfExists;

/**
 * Created by Amos Azaria on 17-Feb-15.
 * This class is in-charge of everything to do with the audio input and obtaining the text from ASR.
 * This class also saves to file.
 */
public class AudioTopDirector
{
    IInteractionManager interactionManager;
    SignalInfoProvider signalInfoProvider;
    IControllingOrders controllingOrders;
    int udpPort;
    ByteArrayBuffer allAudioFromBeginning;

    static final Path folderPath = Paths.get("..\\UserData");
    static final String fileStart = "InputAt";
    Path filePath;

    interface IControllingOrders
    {
        void dealWithAsrRes(ASR.AsrRes asrRes);
        void cancelAllAction();
        void closeAudioConnection();
    }

    AudioTopDirector(int udpPort, IInteractionManager interactionManager, IControllingOrders controllingOrders)
    {
        allAudioFromBeginning = new ByteArrayBuffer();
        this.controllingOrders = controllingOrders;
        this.interactionManager = interactionManager;
        signalInfoProvider = new SignalInfoProvider();
        this.udpPort = udpPort;
    }

    void runServer()
    {
        interactionManager.start();
        filePath = Paths.get(folderPath.toString(), fileStart + (new SimpleDateFormat("ddMMyy-hhmmss.SSS").format(new Date())) + ".raw");
        delIfExists(filePath);

        AudioReceiver streamAudioServer = new AudioReceiver(new AudioReceiver.StreamingAlerts()
        {
            ASR asr = new ASR();
            Path obtainedFile = null;

            @Override
            public void firstAudioArriving()
            {
                try
                {
                    asr.beginTransmission();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            boolean hasValidGoogleCall = false;
            int validGoogleCallId = 0;

            @Override
            public boolean audioArrived(byte[] audioReceived)
            {
                boolean retGetMoreAudio = true;
                try
                {
                    appendToFile(audioReceived, audioReceived.length, filePath);
                    allAudioFromBeginning.write(audioReceived);
                    SignalInfoProvider.SignalInfo signalInfo = signalInfoProvider.obtainSampleInfo(audioReceived);


                    IInteractionManager.ActionToTake actionToTake = interactionManager.updatedAudioInfo(signalInfo.offSetFromFirst,signalInfo.sampleLength,signalInfo.vad, signalInfo.finalPause);

                    if (actionToTake == IInteractionManager.ActionToTake.goToGoogle)
                    {
                        invalidateOldCallIfExistsAndStreamNew();
                        getAsrResAsync();
                    }
                    else if (actionToTake == IInteractionManager.ActionToTake.none)
                    {
                        if (asr.isConnectionOpen())
                            asr.sendDataAsync(audioReceived);
                    }
                    else if (actionToTake == IInteractionManager.ActionToTake.commit)
                    {
                        controllingOrders.closeAudioConnection();
                        retGetMoreAudio = false;
                    } else  if (actionToTake == IInteractionManager.ActionToTake.cancel)
                    {
                        invalidateOldCallIfExistsAndStreamNew();
                    }

                } catch (Exception e)
                {
                    retGetMoreAudio = false;
                    interactionManager.stop();
                    e.printStackTrace();
                }
                return  retGetMoreAudio;
            }

            private void invalidateOldCallIfExistsAndStreamNew()
            {
                validGoogleCallId++;
                if (hasValidGoogleCall)
                {
                    hasValidGoogleCall = false;
                    asr = new ASR();
                    try
                    {
                        asr.beginTransmission();
                        asr.sendDataAsync(allAudioFromBeginning.getRawData());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }


            private void getAsrResAsync() throws IOException
            {
                if (asr.isConnectionOpen())
                {
                    hasValidGoogleCall = true;
                    asr.closeAndGetResponseAsync(new ASR.IAsrGetResponse()
                    {
                        int myGoogleCallId = validGoogleCallId;

                        @Override
                        public void gotResponse(ASR.AsrRes asrRes)
                        {
                            dealWithAsrResponse(asrRes, myGoogleCallId);
                        }
                    });
                }

            }

            private void dealWithAsrResponse(ASR.AsrRes asrRes, int myGoogleCallId)
            {
                if (obtainedFile != null) //write json response text file
                {
                    PrintWriter pw = null;
                    try
                    {
                        pw = new PrintWriter(obtainedFile.toString() + ".txt");
                        pw.print(asrRes.fullJsonRes);
                        pw.flush();
                        pw.close();
                    } catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
                System.out.println(asrRes.text);
                boolean wasNotCanceled = myGoogleCallId == validGoogleCallId;
                if (wasNotCanceled)
                {
                    controllingOrders.dealWithAsrRes(asrRes);
                    interactionManager.stop();
                    controllingOrders.closeAudioConnection();
                }
            }

            @Override
            public void audioEnded()
            {
                try
                {
                    if (asr.isConnectionOpen())
                    {
                        controllingOrders.closeAudioConnection();
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void timedOut()
            {
                try
                {
                    if (!hasValidGoogleCall)
                        getAsrResAsync();

                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }

            }
        }, new InteractionManager());


        streamAudioServer.runServer(udpPort);
    }
}
