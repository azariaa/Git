package InMind.DialogFunctions;

import InMind.Consts;
import InMind.Server.asr.ASR;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Created by Amos Azaria on 24-Dec-14.
 */
public class FunctionInvoker
{

    static final String sayStr = Consts.sayCommand + Consts.commandChar;
    static final String execJson = Consts.execJson + Consts.commandChar;
    static final String launch = Consts.launchCommand + Consts.commandChar;
    public static final String messageFunction = "messageFunction";

    public interface IMessageSender
    {
        void sendMessageToUser(List<String> s);
    }

    //returns a list of commands to the client. May return null.
    @SuppressWarnings("unchecked")
    static public List<String> toInvoke(String dialogFileBase, String funName, Map<String, Object> fullInfo, String userId, ASR.AsrRes asrRes)
    {
        List<String> toSend = null;
        try
        {
            //hack to take care of toInstructable which raised ClassNotFoundException when running through jar file
            if (dialogFileBase.equals("toInstructable"))
            {
                if (funName.equals("toInstructable"))
                {
                    toSend = toInstructable.toInstructable(fullInfo, userId, asrRes);
                }
                else if (funName.equals("saveEmailPassword"))
                {
                    toSend = toInstructable.saveEmailPassword(fullInfo, userId, asrRes);
                }
            }
            else
            {
                //removed reflection for now.//TODO: will be removed from future versions
                Package pack = FunctionInvoker.class.getPackage();
                Method method = Class.forName(pack.getName() + "." + dialogFileBase).getMethod(funName, Map.class, String.class, ASR.AsrRes.class);
                if (method != null)
                {
                    System.out.print(method.toString());
                    toSend = (List<String>) method.invoke(null, fullInfo, userId, asrRes);
                }
            }
        } catch (Exception e)//IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e)
        {
            if (funName != null && dialogFileBase != null)
                System.out.println("Error while trying to invoke:" + funName + " from:" + dialogFileBase);
            e.printStackTrace();
        }

        return toSend;
    }
}
