package com.azariaa.lia.liaClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//import com.yahoo.inmind.services.news.control.util.JsonUtil;

import com.azariaa.lia.Consts;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class AudioStreamer
{
    static AudioStreamer singleton = null;

    AudioRecord recorder;

    public static DatagramSocket socket;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean status = false;

    String ipAddr;
    int portNum;
    private Handler userNotifierHandler;
    boolean connectionActive = false;
    final Object notActiveNotifier = new Object();
    final Object recorderSync = new Object();


    private AudioStreamer(String ipAddr, int portNum, Handler userNotifierHandler)
    {
        this.ipAddr = ipAddr;
        this.portNum = portNum;
        this.userNotifierHandler = userNotifierHandler;
    }

    public static AudioStreamer getAudioStreamerAndStart(String ipAddr, int portNum, Handler userNotifierHandler)
    {
        if (singleton == null)
            singleton = new AudioStreamer(ipAddr, portNum, userNotifierHandler);
        else
            singleton.renew(ipAddr, portNum, userNotifierHandler);
        singleton.startStreaming();
        return singleton;
    }

    private void renew(String ipAddr, int portNum, Handler userNotifierHandler)
    {
        Log.d("AudioStreamer", "C: updating variables.");
        this.userNotifierHandler = userNotifierHandler;
        this.ipAddr = ipAddr;
        this.portNum = portNum;
    }

    public boolean isStreaming()
    {
        return status;
    }

    @Override
    protected void finalize()
    {
        stopStreaming();
    }

    public void stopStreaming()
    {
        status = false;
        if (recorder != null)
        {
            synchronized (recorderSync)
            {
                if (recorder != null)
                {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
            }
        }
        Log.d("AudioStreamer", "Recorder released");
        Message msgNotRecording = new Message();
        msgNotRecording.arg1 = 0;
        userNotifierHandler.sendMessage(msgNotRecording); //set not recording image.
    }

    public void startStreaming()
    {
        Thread streamThread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    status = false;
                    if (connectionActive)
                    {
                        synchronized (notActiveNotifier)
                        {
                            if (connectionActive)
                                notActiveNotifier.wait();
                        }
                    }
                    status = true;
                    connectionActive = true;
                    Log.d("AudioStreamer", "Before Creating socket");
                    socket = new DatagramSocket();
                    Log.d("AudioStreamer", "Socket Created.");
                    int minBufSize = AudioRecord.getMinBufferSize(Consts.sampleRate, channelConfig, audioFormat);
                    //int minBufSize = 3584;//Consts.udpBufferSize;

                    Log.d("AudioStreamer", "minBufSize:" + minBufSize);
                    byte[] buffer = new byte[minBufSize];

                    Log.d("AudioStreamer", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(ipAddr);
                    Log.d("AudioStreamer", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Consts.sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    Log.d("AudioStreamer", "Recorder initialized");

                    recorder.startRecording();
                    Log.d("AudioStreamer", "Recordering");

                    if (!status)
                    {
                        Message msgTalk = new Message();
                        msgTalk.arg1 = 1;
                        msgTalk.obj = "Could not initialize mic. on time!";
                        userNotifierHandler.sendMessage(msgTalk);
                    }
                    else
                    {


                        //Added to remove initial noise which Android (or at least Nexus 5) seems to have at the beginning. //can try using noise removal algorithms instead, look at Audacity.
//					Message msgWait = new Message();
//					msgWait.arg1 = 1;
//					msgWait.obj = "Wait!";
//					toasterHandler.sendMessage(msgWait);
//
//					Thread.sleep(1600);

                        Message msgTalk = new Message();
                        msgTalk.arg1 = 1;
                        msgTalk.arg2 = 1; //important toast
                        msgTalk.obj = "Talk!";
                        userNotifierHandler.sendMessage(msgTalk);
                        //Message msgPlayTone = new Message();
                        //msgPlayTone.arg1 = 2;
                        //toasterHandler.sendMessage(msgPlayTone);

//					Thread.sleep(300);
//					//couldn't find a better way to clear buffer.
//					byte[] tmpbuffer = new byte[minBufSize*1000];
//					recorder.read(tmpbuffer, 0, minBufSize*1000);
//					tmpbuffer=null;
                    }

                    while (status == true)
                    {

                        if (recorder != null)
                        {
                            int bytesRead = 0;
                            synchronized (recorderSync)
                            {
                                if (recorder != null)
                                {
                                    //reading data from MIC into buffer
                                    bytesRead = recorder.read(buffer, 0, buffer.length);
                                }
                            }

                            if (bytesRead > 0)
                            {
                                //putting buffer in the packet
                                packet = new DatagramPacket(buffer, buffer.length, destination, portNum);
                                socket.send(packet);
                                Log.d("AudioStreamer", "Send_Packet: " + minBufSize);
                            }
                        }
                    }

                    socket.close();
                    Log.d("AudioStreamer", "Socket Closed");


                }
                catch (Exception ex)
                {
                    stopStreaming();
                    if (socket != null)
                        socket.close();
                }
                connectionActive = false;
                synchronized (notActiveNotifier)
                {
                    notActiveNotifier.notifyAll();
                }
            }

        });
        streamThread.start();
    }


}
