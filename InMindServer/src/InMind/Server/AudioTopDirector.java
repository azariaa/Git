package InMind.Server;

import InMind.Server.SignalInfo.ASignalInfoProvider;
import InMind.Server.SignalInfo.SimpleSignalInfoProvider;
import InMind.Server.asr.ASR;
import InMind.Server.interactionManager.AInteractionManager;
import InMind.Server.interactionManager.InteractionManager;
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
    AInteractionManager interactionManager;
    ASignalInfoProvider signalInfoProvider;
    IControllingOrders controllingOrders;
    int udpPort;
    ByteArrayBuffer allAudioFromBeginning;

    static final Path folderPath = Paths.get("../UserData");
    static final String fileStart = "InputAt";
    Path filePath;
    ASR asr;
    AudioReceiver audioReceiver;

    boolean sentCloseAudioConnection;

    interface IControllingOrders
    {
        void dealWithAsrRes(ASR.AsrRes asrRes);

        void cancelAllAction();

        void closeAudioConnection();
    }

    AudioTopDirector(int udpPort, IControllingOrders controllingOrders)
    {
        allAudioFromBeginning = new ByteArrayBuffer();
        this.controllingOrders = controllingOrders;
        this.interactionManager = new InteractionManager(new IMCommandExecutor());
        signalInfoProvider = new SimpleSignalInfoProvider();//new SphinxSignalInfoProvider();
        this.udpPort = udpPort;
        sentCloseAudioConnection = false;
    }


    private class IMCommandExecutor implements AInteractionManager.IIMRequiredAction
    {
        boolean hasValidGoogleCall = false;
        int validGoogleCallId = 0;
        ASR.AsrRes latestValidRes = null;

        @Override
        public void takeAction(AInteractionManager.ActionToTake actionToTake)
        {
            try
            {
                System.out.println("received command from IM: " + actionToTake.toString());

                switch (actionToTake)
                {
                    case none:
                        break;
                    case commit:
                        //ignoring, since I decide on my own when to commit.
//                        audioReceiver.stopListening();
//                        signalInfoProvider.endStream();
                        //controllingOrders.closeAudioConnection();
                        break;
                    case goToGoogle:
                        invalidateOldCallIfExistsAndStreamNew();
                        getAsrResAsync();
                        break;
                    case cancel:
                        invalidateOldCallIfExistsAndStreamNew();
                        break;
                    case moveOn:
                        if (latestValidRes != null)
                            moveOnWithResponse();
                        else
                            System.out.println("Error! received 'moveOn', but no validRes is present!");
                        break;
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }

        }

        private void dealWithAsrResponse(ASR.AsrRes asrRes, int myGoogleCallId)
        {
            if (filePath != null) //write json response text file
            {
                PrintWriter pw = null;
                try
                {
                    pw = new PrintWriter(filePath.toString() + ".txt");
                    pw.print(asrRes.fullJsonRes);
                    pw.flush();
                    pw.close();
                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            System.out.println("Google Asr response: " + asrRes.text);
            boolean wasNotCanceled = myGoogleCallId == validGoogleCallId;
            if (wasNotCanceled)
            {
                latestValidRes = asrRes;
                interactionManager.finalResponseObtained();//null);//expects an event but not clear what it should contain.
                //moveOnWithResponse();
            }
        }

        private void moveOnWithResponse()
        {
            interactionManager.stop();
            //we first want the server to stop listening so we won't get stuck in waiting (until timeout).
            audioReceiver.stopListening();
            signalInfoProvider.endStream();
            //we need to send the user a message (over tcp) to close audio connection (udp) before closing tcp connection.
            if (!sentCloseAudioConnection)
            {
                //due to no synchronization method, in theory, closeAudioConnection might be sent twice, but who cares?
                sentCloseAudioConnection = true;
                controllingOrders.closeAudioConnection();
            }
            //this will send a response and close tcp connection
            controllingOrders.dealWithAsrRes(latestValidRes);
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
                    if (asr.isConnectionOpen())
                        asr.sendDataAsync(allAudioFromBeginning.getRawData(), allAudioFromBeginning.size());
                } catch (Exception e)
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
    }

    private IMCommandExecutor imCommandExecutor = new IMCommandExecutor();

    void runServer()
    {
        interactionManager.start();
        asr = new ASR();
        filePath = Paths.get(folderPath.toString(), fileStart + (new SimpleDateFormat("ddMMyy-HHmmss.SSS").format(new Date())) + ".raw");
        delIfExists(filePath);

        audioReceiver = new AudioReceiver(new AudioReceiver.StreamingAlerts()
        {
            @Override
            public void firstAudioArriving()
            {
                try
                {
                    signalInfoProvider.startNewStream();
                    asr.beginTransmission();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }


            @Override
            public void audioArrived(byte[] audioReceived, int length)
            {
                try
                {
                    appendToFile(audioReceived, length, filePath);
                    allAudioFromBeginning.write(audioReceived, 0, length);
                    if (asr.isConnectionOpen())
                        asr.sendDataAsync(audioReceived, length);
                    SimpleSignalInfoProvider.SignalInfo signalInfo = signalInfoProvider.obtainSampleInfo(audioReceived, length);
                    interactionManager.updatedAudioInfo(signalInfo);
//                    IMEvent imEventVad = new IMEvent(IMEvent.IMEventType.vad);
//                    imEventVad.feature.put(IMEvent.featureVad, (signalInfo.vad == 1) ? "true":"false");
//                    imEventVad.feature.put(IMEvent.featureFinalPause, ((Double)signalInfo.finalPause).toString());
//                    imEventVad.feature.put(IMEvent.featureDurationFromBeginning, ((Double) signalInfo.offSetFromFirst).toString());
//                    imEventVad.feature.put(IMEvent.featureDurationOfSample, ((Double) signalInfo.sampleLength).toString());
//                    interactionManager.updatedAudioInfo(imEventVad);//signalInfo.offSetFromFirst, signalInfo.sampleLength, signalInfo.vad, signalInfo.finalPause);
//
//                    IMEvent imEventAsr = new IMEvent(IMEvent.IMEventType.asr);
//                    imEventAsr.feature.put(IMEvent.featureVad, (signalInfo.vad == 1) ? "true":"false");
//                    imEventAsr.feature.put(IMEvent.featureFinalPause, ((Double)signalInfo.finalPause).toString());
//                    imEventAsr.feature.put(IMEvent.featureDurationFromBeginning, ((Double) signalInfo.offSetFromFirst).toString());
//                    imEventAsr.feature.put(IMEvent.featureDurationOfSample, ((Double) signalInfo.sampleLength).toString());
//                    interactionManager.updatedAudioInfo(imEventAsr);

                } catch (Exception e)
                {
                    interactionManager.stop();
                    e.printStackTrace();
                }
            }


            @Override
            public void stoppedListeningForAudio()
            {
                try
                {
                    if (!sentCloseAudioConnection)
                    {
                        sentCloseAudioConnection = true;
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
                //if (!hasValidGoogleCall)
                //getAsrResAsync();
                interactionManager.userStoppedStreaming();
            }
        });


        audioReceiver.runServer(udpPort);
    }
}
