package InMind.Server;

import InMind.Consts;
import InMind.Server.asr.ASR;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Amos Azaria on 16-Dec-14.
 */
public class InMindLogic
{

    static final int udpFirstPort = 50005;
    static final int udpLastPort = 52005;
    int currentPort = udpFirstPort;
    int nextPort()
    {
        currentPort++;
        if (currentPort > udpLastPort)
            currentPort = udpFirstPort;
        return currentPort;
    }

    Map<String,UserConversation> userConversationMap = new HashMap<String,UserConversation>();

    public void runServer()
    {

        while (true)
        {
            try
            {
                //creates the object OnMessageReceived asked by the TCPServer constructor
                MessageReceiver messageReceiver = new MessageReceiver();
                TCPServer tcpServer = new TCPServer(messageReceiver);
                messageReceiver.tcpServer = tcpServer;
                tcpServer.listenForConnection(); //blocking until accept
                tcpServer.start();

            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }


    public interface SendMessageToUser
    {
        public void sendMessage(String message);
    }


    public class MessageReceiver implements TCPServer.OnMessageReceived
    {

        TCPServer tcpServer = null;
        String userId = null;

        //this method declared in the interface from TCPServer class is implemented here
        //this method is actually a callback method, because it will run every time when it will be called from
        //TCPServer class (at while)
        public void messageReceived(String message)
        {
            try
            {
                System.out.println("InMindLogic" + "Dealing with message:" + message);
                Pattern p = Pattern.compile(Consts.clientMessagePattern);
                Matcher m = p.matcher(message);
                boolean found = m.find();
                if (found)
                {
                    userId = m.group(1);

                    ASR.AsrRes asrRes = null;

                    if (m.group(2).equalsIgnoreCase(Consts.sendingText))
                    {
                        asrRes = new ASR.AsrRes();
                        asrRes.confidence = 1;
                        asrRes.text = m.group(3).trim();//TODO: might want to remove punctuation.
                        dealWithText(userId, asrRes);
                    }

                    if (m.group(2).equalsIgnoreCase(Consts.requestSendAudio))
                    {
                        int portToUse = nextPort();
                        tcpServer.sendMessage(Consts.connectUdp + Consts.commandChar + portToUse);


                        AudioTopDirector audioTopDirector = new AudioTopDirector(portToUse, new AudioTopDirector.IControllingOrders()
                        {
                            @Override
                            public void dealWithAsrRes(ASR.AsrRes asrRes)
                            {
                                dealWithText(userId, asrRes);
                            }

                            @Override
                            public void cancelAllAction()
                            {

                            }

                            @Override
                            public void closeAudioConnection()
                            {
                                tcpServer.sendMessage(Consts.stopUdp + Consts.commandChar);
                            }
                        });

                        audioTopDirector.runServer();
                    }

                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private void dealWithText(String userId, ASR.AsrRes asrRes)
        {
            if (asrRes != null && asrRes.text != null && !asrRes.text.isEmpty() && userId != null)
            {
                UserConversation userConversation = null;
                if (userConversationMap.containsKey(userId))
                    userConversation = userConversationMap.get(userId);
                else
                {
                    userConversation = new UserConversation(userId);
                    userConversationMap.put(userId,userConversation);
                }

                UserConversation.ToDoWithConnection toDoWithConnection = userConversation.dealWithMessage(asrRes, new MessageSender());

                if (toDoWithConnection != UserConversation.ToDoWithConnection.nothing)
                {
                    tcpServer.sendMessage((toDoWithConnection == UserConversation.ToDoWithConnection.renew ? Consts.startNewConnection : Consts.closeConnection) + Consts.commandChar);
                    tcpServer.abandonClient();
                }
            }
            else
            {
                //tcpServer.sendMessage(Consts.sayCommand + Consts.commandChar + "I didn't hear anything.");
                tcpServer.sendMessage(Consts.closeConnection + Consts.commandChar);
                tcpServer.abandonClient();
            }

            //runServer();
        }

        public class MessageSender implements SendMessageToUser
        {
            @Override
            public void sendMessage(String message)
            {
                if (tcpServer != null)
                    tcpServer.sendMessage(message);
            }
        }

    }
}
