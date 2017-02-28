package InMind.Server;

import InMind.Server.asr.ASR;
import InMind.Utils;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Amos Azaria on 23-Dec-14.
 * <p/>
 * Important: Should be unique per user.
 */
public class UserConversation
{
    static final String conditionsAndSetChar = ";";
    static final String dontRenewConnectionStr = "dontRenew";
    static final String dontCloseConnectionStr = "dontClose";


    String userId;
    ToInstructable toInstructable;

    Logger logger;

    public UserConversation(String userId)
    {
        this.userId = userId;
        this.toInstructable = new ToInstructable(userId);
        logger = Utils.createLogger(UserConversation.class.getName());
    }


    public enum ToDoWithConnection {nothing, close, renew};
    /*
    returns whether to renew a connection.
     */
    public ToDoWithConnection dealWithMessage(ASR.AsrRes asrRes, InMindLogic.MessageReceiver.MessageSender messageSender)
    {
        String userText = asrRes.text;
        logger.info("userid:" + userId + ", userText" +(asrRes.wasSentAsText ? "(as text)" : "(from speech)")+":" + userText);


        List<String> toSend = toInstructable.connectWithInstructable(asrRes);
        sendToUser(messageSender, toSend);
//        if (dialogFileBase.isEmpty() || fullInfo.containsKey(dontRenewConnectionStr))
//            return ToDoWithConnection.close;
//        if (fullInfo.containsKey(dontCloseConnectionStr))
//            return ToDoWithConnection.nothing;
        return ToDoWithConnection.close;
    }

    private void sendToUser(InMindLogic.MessageReceiver.MessageSender messageSender, List<String> forUser) //, boolean saveMessage)
    {
        if (forUser != null)
        {
            for (String command : forUser)
            {

                if (!command.isEmpty())
                {
                    messageSender.sendMessage(command);//"Say^You Said:" + asrRes.text);
                    System.out.println("sent message to user:" + command);
                    logger.info("userid:" + userId + ", agentText:" + command);
                }
            }
        }
    }
}
