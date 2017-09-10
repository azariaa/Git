package com.azariaa.lia.liaClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azariaa.lia.Consts;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.azariaa.lia.simpleUtils.getCurrentTimeStr;

//import com.yahoo.inmind.comm.generic.control.MessageBroker;

/*
 * This class is in-charge of all connections to the server. 
 * It first connects to the server (via TCP), authentication etc.
 * Then receives a port number and connects to it via UDP to stream the audio.
 * 
 * 
 * Created by Amos Azaria on 31-Dec-14.
 */
public class LogicController
{


    TCPClient tcpClient;
    AudioStreamer audioStreamer;

    String tcpIpAddr = "34.193.23.122";
    int tcpIpPort = Consts.serverPort;
    String udpIpAddr;
    int udpIpPort;
    String uniqueId;

    private Handler userNotifierHandler;
    private Handler talkHandler;
    private Handler launchHandler;
    syncNotifiers startStopRecNotifier;
    private boolean needToReconnect;

    private MessageController messageController;
    private Context context = null;


    interface syncNotifiers
    {
        void startStopRec(boolean start);
    }

    public LogicController(Handler userNotifierHandler, Handler talkHandler, Handler launchHandler, syncNotifiers startStopRecNotifier, String uniqueId)
    {
        this.userNotifierHandler = userNotifierHandler;
        this.talkHandler = talkHandler;
        this.launchHandler = launchHandler;
        this.startStopRecNotifier = startStopRecNotifier;
        messageController = new MessageController();
        this.uniqueId = uniqueId;
    }

    public void ConnectToServer(String sendThisText, boolean isCommand)
    {
        //closeConnection();
        sendMessageUsingTcp(uniqueId + Consts.commandChar +  getCurrentTimeStr() + Consts.commandChar + (isCommand ? Consts.sendingCommand : Consts.sendingText) + Consts.commandChar + sendThisText);
    }

    public void ConnectToServer(String usedWakeupPhrase)
    {
        //if is currently streaming, ignore request.
        if (tcpClient != null && audioStreamer != null && audioStreamer.isStreaming())
            return;
        //closeConnection(); //not closing, since sometimes remains open.
        sendMessageUsingTcp(uniqueId + Consts.commandChar +  getCurrentTimeStr() + Consts.commandChar + Consts.requestSendAudio + Consts.commandChar + "usedWakeupPhrase: " + usedWakeupPhrase);
    }

    private void sendMessageUsingTcp(String messageToSend)
    {
        if (tcpClient == null)
        {
            //we create a TCPClient object
            tcpClient = TCPClient.getTCPClientAndConnect(tcpIpAddr, tcpIpPort, new TCPClient.OnMessageReceived()
            {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message)
                {
                    dealWithMessage(message); //TODO: make sure that runs on same thread. (avoid multithread unsafe access).
                    //publishProgress(message);//this method calls the onProgressUpdate
                }
            });
        }
        //new connectTask().execute(messageToSend);
        tcpClient.sendMessage(messageToSend);
    }

    public void closeConnection()
    {
        stopStreaming();
        if (tcpClient != null)
        {
            tcpClient.closeConnection();
            tcpClient = null;
        }
    }

    public void stopStreaming()
    {
        if (audioStreamer != null)
        {
            audioStreamer.stopStreaming();
            audioStreamer = null;
        }
    }

    public void changeInitIpAddr(String newIpAddr)
    {
        closeConnection();
        tcpIpAddr = newIpAddr;
    }

    public void changeInitPort(int newPort)
    {
        tcpIpPort = newPort;
    }

    private void openAudioStream()
    {
        audioStreamer = AudioStreamer.getAudioStreamerAndStart(udpIpAddr, udpIpPort, userNotifierHandler);
    }

    private void dealWithMessage(String message)
    {
        //TODO: all this can be written better with a 'switch' and with an object holding the action and the arguments together.
        Log.d("ServerConnector", "Dealing with message:" + message);
        Pattern p = Pattern.compile(Consts.serverMessagePattern);
        Matcher m = p.matcher(message);
        boolean found = m.find();
        Log.d("ServerConnector", "found:" + found);
        if (found)
        {
            if (m.group(1).equalsIgnoreCase(Consts.closeConnection))
            {
                closeConnection();
            }
            if (m.group(1).equalsIgnoreCase(Consts.startNewConnection))
            {
                closeConnection();
                needToReconnect = true;
            }
            if (m.group(1).equalsIgnoreCase(Consts.stopUdp))
            {
                stopStreaming();
                startStopRecNotifier.startStopRec(false); //say that is stopping the recording. must be called AFTER stopping.
            }
            else if (m.group(1).equalsIgnoreCase(Consts.connectUdp))
            {
                udpIpPort = 0;
                try
                {
                    startStopRecNotifier.startStopRec(true);//say that is starting the recording. must be called before starting.
                    udpIpAddr = tcpIpAddr;
                    Log.d("ServerConnector", "found:" + found);
                    //String protocol = m.group(1);
                    udpIpPort = Integer.parseInt(m.group(2).trim());
                    Log.d("ServerConnector", "Got port:" + udpIpPort);
                }
                catch (Exception e)
                {
                    Log.e("ServerConnector", "Error parsing message from server...");
                }
                if (udpIpPort > 0)
                    openAudioStream();
            }
            else if (m.group(1).equalsIgnoreCase(Consts.sayCommand) || m.group(1).equalsIgnoreCase(Consts.sayQuietlyCommand))
            {
                Log.d("ServerConnector", "saying:" + m.group(2));
                Message msgTalk = new Message();
                msgTalk.arg1 = 3; //say aloud and toast
                if (m.group(1).equalsIgnoreCase(Consts.sayQuietlyCommand))
                    msgTalk.arg1 = 1; //don't say aloud, just text and toast.
                msgTalk.obj = m.group(2).trim();
                talkHandler.sendMessage(msgTalk);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.userSaid))
            {
                Log.d("ServerConnector", "user said:" + m.group(2));
                Message msgTalk = new Message();
                msgTalk.arg1 = 0; //don't say aloud and don't toast
                msgTalk.arg2 = 2; //user said
                msgTalk.obj = m.group(2).trim();
                talkHandler.sendMessage(msgTalk);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.launchCommand))
            {
                Message msgLaunch = new Message();
                msgLaunch.arg1 = 1;
                msgLaunch.obj = m.group(2).trim();
                launchHandler.sendMessage(msgLaunch);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.sugilite))
            {
                Log.d("ServerConnector", "Sugilite message:" + m.group(2));
                Message msgLaunch = new Message();
                msgLaunch.arg1 = 2;
                msgLaunch.obj = m.group(2).trim();
                launchHandler.sendMessage(msgLaunch);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.youTube))
            {
                Log.d("ServerConnector", "Play youtube message:" + m.group(2));
                Message msgLaunch = new Message();
                msgLaunch.arg1 = 3;
                String fullArg = m.group(2).trim();
                String videoOrPlayListId;
                boolean isPlaylist = false;
                if (fullArg.startsWith(Consts.playListPre))
                {
                    videoOrPlayListId = fullArg.substring(Consts.playListPre.length());
                    isPlaylist = true;
                }
                else
                {
                    videoOrPlayListId = fullArg.substring(Consts.videoPre.length());
                }
                msgLaunch.obj = videoOrPlayListId;
                msgLaunch.arg2 = isPlaylist ? 1 : 0;
                launchHandler.sendMessage(msgLaunch);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.timerFunctions))
            {
                Log.d("ServerConnector", "timeFunctions message:" + m.group(2));
                Message msgLaunch = new Message();
                msgLaunch.arg1 = 4;
                msgLaunch.obj = m.group(2).trim();
                launchHandler.sendMessage(msgLaunch);
            }
            else //not basic command, check with middleware
            {
                String command = m.group(1);
                String args = null;
                if (m.groupCount() > 1)
                    args = m.group(2);
                try
                {
                    messageController.dealWithMessage(command, args, talkHandler);
                }
                catch (Exception ex)
                {
                    //Log.e("messageController.dealWithMessage", "command=" + command + " args=" + args + " " + ex.toString());
                    ex.printStackTrace();
                }
            }
        }
    }


    /*
     * returns whether is reconnecting now.
     */
    public boolean reconnectIfNeeded()
    {
        Log.d("LogicControl", "Reconnecting if needed");

        boolean isReconnecting = needToReconnect;
        if (needToReconnect)
        {
            needToReconnect = false;
            ConnectToServer("reconnect");
        }
        return isReconnecting;
    }


}
