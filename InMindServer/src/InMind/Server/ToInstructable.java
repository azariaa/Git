package InMind.Server;

import InMind.Consts;
import InMind.Server.asr.ASR;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Amos Azaria on 11-Aug-15.
 */
public class ToInstructable
{
    static private final int portToUse = 18892;
    static private final String contextRealtimeAgent = "realtimeAgent";
    static private final String actionParam = "action";
    static private final String actionUserSays = "actionUserSays";
    static private final String actionResendRequested = "actionResendRequested";
    static private final String actionSetEmailAndPswd = "actionSetEmailAndPswd";
    static private final String userSaysParam = "userSays";
    static private final String userIdParam = "userId";
    static private final String usernameParm = "username";
    static private final String encPwd = "encPwd";
    static private final String emailParm = "email";
    static private final String realPwd = "realPwd";
    //static public final String successContains = "successfully";
    static public final String multiAltSentenceConcat = "^";
    static public final String userNotRegistered = "user not registered";
    static public final String sayWithCom = Consts.sayCommand + Consts.commandChar;

    ToInstructable(String userId)
    {
        this.userId = userId;
    }

    String userId;
    boolean obtainingUsernamePassword = false;
    Optional<String> userEmail = Optional.empty();
    Optional<String> userPassword = Optional.empty();


    public List<String> connectWithInstructable(ASR.AsrRes userText)
    {
        try
        {
            if (obtainingUsernamePassword)
                return dealWithEmailAndPassword(false, userText);

            String username = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 0).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
            String enctyptionPwd = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 1).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));

            Map<String, String> parameters = new HashMap<>();
            parameters.put(actionParam, actionUserSays);
            parameters.put(userIdParam, username); //use username as userId for now
            parameters.put(usernameParm, username);
            parameters.put(encPwd, enctyptionPwd); //only relevant if server has user's password.

            String bestPossibleSentences = userText.text;
            for (int i = 0; i < userText.alternatives.size() && i < 2; i++) //add upto 2 alternatives
            {
                bestPossibleSentences = bestPossibleSentences + multiAltSentenceConcat + userText.alternatives.get(i);
            }
            parameters.put(userSaysParam, bestPossibleSentences);
            String url = "http://localhost:" + portToUse + "/" + contextRealtimeAgent;
            String response = dialogUtils.callServer(url, parameters, false);

            String[] res = response.split("\n");
            List<String> commands = new LinkedList<>();
            for (String sentence : res)
            {
                if (sentence.isEmpty())
                    continue;
                if (sentence.startsWith(Consts.getEmailAndPassword)) //TODO: future versions should obtain a token using oAuth 2
                {
                    return dealWithEmailAndPassword(true, userText /*shouldn't really matter here*/);
                }
                if (sentence.startsWith(Consts.execCmdPre) ||
                        sentence.startsWith(Consts.demonstrateStr) ||
                        sentence.startsWith(Consts.playYouTubeStr) ||
                        sentence.startsWith(Consts.timerFunctions + Consts.instructableDelimiterColon))
                {
                    if (sentence.startsWith(Consts.execCmdPre))
                    {
                        String jsonToExec = sentence.substring(Consts.execCmdPre.length()).trim();
                        String exeCommand = Consts.sugilite + Consts.commandChar;
                        exeCommand += Consts.sugiliteExecJson + Consts.commandChar + jsonToExec;
                        commands.add(exeCommand);
                        if (res.length == 1) //only if this is the only command being executed we say "Executing". This is to enable the user to define what should be said (e.g. say ordering cappuccino)
                            commands.add(sayWithCom + "Executing."); //cannot add script name, since there may be different scripts combined, and there also may be no script name
                    }
                    else if (sentence.startsWith(Consts.demonstrateStr))
                    {
                        String scriptName = sentence.substring(Consts.demonstrateStr.length()).trim();
                        String exeCommand = Consts.sugilite + Consts.commandChar;
                        exeCommand += Consts.sugiliteStartRecording + Consts.commandChar + scriptName;
                        commands.add(exeCommand);
                        commands.add(sayWithCom + "Show me how to " + scriptName + "! Once you are done click on the duck and select end recording.");
                    }
                    else if (sentence.startsWith(Consts.playYouTubeStr))
                    {
                        String videoId = sentence.substring(Consts.playYouTubeStr.length()).trim();
                        String exeCommand = Consts.youTube + Consts.commandChar;
                        exeCommand += videoId;
                        commands.add(exeCommand);
                        commands.add(sayWithCom + "Playing...");
                    }
                    else if (sentence.startsWith(Consts.timerFunctions + Consts.instructableDelimiterColon))
                    {
                        String json = sentence.substring(Consts.timerFunctions.length() + 1).trim();
                        commands.add(Consts.timerFunctions + Consts.commandChar + json);
                    }
                }
                else
                {
                    commands.add(sayWithCom + sentence);
                }
            }
            return commands;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return Collections.singletonList(sayWithCom + "Could not connect to instructable server.");
        }
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    private List<String> dealWithEmailAndPassword(boolean gotNewRequest, ASR.AsrRes userText)
    {
        if (gotNewRequest)
        {
            obtainingUsernamePassword = true;
            return Collections.singletonList(sayWithCom + "This operation requires obtaining your email and password. Please type-in your full email address (or just ignore). Your email and password will be stored on a server. You may wish to provide a new email account and forward your emails to this account.");
        }
        else if (userEmail.isPresent())
        {
            userPassword = Optional.of(userText.text);
            obtainingUsernamePassword = false;
            return saveEmailPassword();
        }

        //check if this looks like an email, if so, ask for password, if not, remove obtainingUsernamePassword state and pass on to instructable server.
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(userText.text);
        if (matcher.find())
        {
            userEmail = Optional.of(userText.text);
            return Collections.singletonList(sayWithCom + "Thank you. Now please type-in your password.");
        }
        else
        {
            obtainingUsernamePassword = false;
            return connectWithInstructable(userText);
        }
    }


    public List<String> saveEmailPassword()
        {
            try
            {
                //split userId to username and password //would actually be more efficient to just loop through all characters...
                String username = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 0).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
                String enctyptionPwd = IntStream.range(0, userId.length()).filter(i -> (i % 2) == 1).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));


                Map<String, String> parameters = new HashMap<>();
                parameters.put(actionParam, actionSetEmailAndPswd);
                parameters.put(userIdParam, username); //use username as userId for now
                parameters.put(usernameParm, username);
                parameters.put(encPwd, enctyptionPwd);
                parameters.put(emailParm, userEmail.get());
                parameters.put(realPwd, userPassword.get());
                String url = "http://localhost:" + portToUse + "/" + contextRealtimeAgent;
                String response = dialogUtils.callServer(url, parameters, false);
                userEmail = Optional.empty(); //delete them for security reasons.
                userPassword = Optional.empty(); //delete them for security reasons.
                return Collections.singletonList(sayWithCom+response);
            } catch (Exception ex)
            {
                ex.printStackTrace();
                return Collections.singletonList(sayWithCom+"Could not connect to instructable server.");
            }
        }


}
