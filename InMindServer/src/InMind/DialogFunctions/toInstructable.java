package InMind.DialogFunctions;

import InMind.Consts;
import InMind.Server.asr.ASR;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Amos Azaria on 11-Aug-15.
 */
public class toInstructable
{
    static private final int portToUse = 18892;
    static private final String contextRealtimeAgent = "realtimeAgent";
    static private final String actionParam = "action";
    static private final String actionUserSays = "actionUserSays";
    static private final String actionResendRequested = "actionResendRequested";
    static private final String actionNewRealUser = "actionNewRealUser";
    static private final String userSaysParam = "userSays";
    static private final String userIdParam = "userId";
    static private final String usernameParm = "username";
    static private final String encPwd = "encPwd";
    static private final String emailParm = "email";
    static private final String realPwd = "realPwd";
    static public final String successContains = "successfully";
    static public final String multiAltSentenceConcat = "^";
    static public final String userNotRegistered = "user not registered";

    public static List<String> toInstructable(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText)
    {
        try
        {
            boolean retryingToRegister = fullInfo.get("state").equals("registerUser");
            //split userId to username and password //would actually be more efficient to just loop through all characters...
            String username = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 0).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
            String enctyptionPwd = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 1).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));

            if (!fullInfo.get("state").equals("has"))
            {
                Map<String, String> parameters = new HashMap<>();
                parameters.put(actionParam, actionNewRealUser);
                parameters.put(userIdParam, username); //use username as userId for now
                parameters.put(usernameParm, username);
                parameters.put(encPwd, enctyptionPwd);
                String url = "http://localhost:" + portToUse + "/" + contextRealtimeAgent;
                String response = dialogUtils.callServer(url, parameters, false);
                if (!response.contains(successContains))
                {
                    //check if already set email/password, if not ask for it
                    fullInfo.put("state", "getEmail");
                    return Arrays.asList(FunctionInvoker.sayStr+"I'm afraid that I don't have your email address yet.", FunctionInvoker.sayStr+"Please type in your email address above, and click send.");
                }
                else
                    fullInfo.put("state", "has"); //and move on
            }
            Map<String, String> parameters = new HashMap<>();
            parameters.put(actionParam, actionUserSays);
            parameters.put(userIdParam, username); //use username as userId for now
            parameters.put(usernameParm, username);
            parameters.put(encPwd, enctyptionPwd);
            String bestPossibleSentences = userText.text;
            for (int i = 0; i < userText.alternatives.size() && i < 2; i++) //add upto 2 alternatives
            {
                bestPossibleSentences = bestPossibleSentences + multiAltSentenceConcat + userText.alternatives.get(i);
            }
            parameters.put(userSaysParam, bestPossibleSentences);
            String url = "http://localhost:" + portToUse + "/" + contextRealtimeAgent;
            String response = dialogUtils.callServer(url, parameters, false);
            if (response.trim().equals(userNotRegistered))
            {
                if (retryingToRegister) //avoid unending recursion
                {
                    return Collections.singletonList(FunctionInvoker.sayStr+"Could not register on instructable server.");
                }
                else
                {
                    fullInfo.put("state", "registerUser");
                    return toInstructable(fullInfo, userId, userText);
                }
            }
            String[] res = response.split("\n");
            List<String> commands = new LinkedList<>();
            for (String sentence : res)
            {
                if (sentence.isEmpty())
                    continue;
                if (sentence.startsWith(Consts.execCmdPre) || sentence.startsWith(Consts.demonstrateStr))
                {

                    if (sentence.startsWith(Consts.execCmdPre))
                    {
                        String jsonToExec = sentence.substring(Consts.execCmdPre.length()).trim();
                        String exeCommand = Consts.sugilite + Consts.commandChar;
                        exeCommand += Consts.sugiliteExecJson + Consts.commandChar + jsonToExec;
                        commands.add(exeCommand);
                        if (res.length == 1) //only if this is the only command being executed we say "Executing". This is to enable the user to define what should be said (e.g. say ordering cappuccino)
                            commands.add(FunctionInvoker.sayStr + "Executing."); //cannot add script name, since there may be different scripts combined, and there also may be no script name
                    }
                    else
                    {
                        String scriptName = sentence.substring(Consts.demonstrateStr.length()).trim();
                        String exeCommand = Consts.sugilite + Consts.commandChar;
                        exeCommand += Consts.sugiliteStartRecording + Consts.commandChar + scriptName;
                        commands.add(exeCommand);
                        commands.add(FunctionInvoker.sayStr + "Show me how to " + scriptName + "! Once you are done click on the duck and select end recording.");
                    }
                }
                else
                {
                    commands.add(FunctionInvoker.sayStr + sentence);
                    //return Arrays.asList(res).stream().filter(s -> !s.isEmpty()).map(s -> FunctionInvoker.sayStr + s).collect(Collectors.toList());
                }
            }
            return commands;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return Collections.singletonList(FunctionInvoker.sayStr+"Could not connect to instructable server.");
        }

    }

    public static List<String> saveEmailPassword(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText)
    {
        try
        {
            //split userId to username and password //would actually be more efficient to just loop through all characters...
            String username = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 0).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
            String enctyptionPwd = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 1).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));

            String email = fullInfo.get("email").toString();
            String password = fullInfo.get("password").toString();

            Map<String, String> parameters = new HashMap<>();
            parameters.put(actionParam, actionNewRealUser);
            parameters.put(userIdParam, username); //use username as userId for now
            parameters.put(usernameParm, username);
            parameters.put(encPwd, enctyptionPwd);
            parameters.put(emailParm, email);
            parameters.put(realPwd, password);
            String url = "http://localhost:" + portToUse + "/" + contextRealtimeAgent;
            String response = dialogUtils.callServer(url, parameters, false);
            return Collections.singletonList(FunctionInvoker.sayStr+response);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return Collections.singletonList(FunctionInvoker.sayStr+"Could not connect to instructable server.");
        }
    }

}
