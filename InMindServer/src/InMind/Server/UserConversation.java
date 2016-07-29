package InMind.Server;

import InMind.Consts;
import InMind.DialogFunctions.FunctionInvoker;
import InMind.Server.asr.ASR;
import InMind.Utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Amos Azaria on 23-Dec-14.
 * <p/>
 * Important: Should be unique per user.
 */
public class UserConversation
{
    static final String cvsSplitBy = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    static final String basePath = "../Configurations";
    static final String dialogReturn = "return";
    public static final String dialogFinish = "finish";

    static final String cstExtension = "csv";
    static final String conditionsAndSetChar = ";";
    static final String setEquality = "=";

    public static final String stateName = "state";
    static final String callFunName = "callfun";
    static final String callFunRepeat = "repeat";
    static final String dontRenewConnectionStr = "dontRenew";
    static final String dontCloseConnectionStr = "dontClose";


    String dialogFileBase = "";
    String tmpDialogFileBaseJustExited = ""; //used when returning from file use on the first time, not to enter same file again
    Map<String, Object> fullInfo;
    String userId;

    List<String> previousMessages = new LinkedList<String>();



    ScriptEngineManager mgr;
    ScriptEngine engine;

    Logger logger;

    public UserConversation(String userId)
    {
        this.userId = userId;
        fullInfo = new HashMap<String, Object>();
        clearDialog();
        mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("JavaScript");
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
        boolean firstEnter = false;
        List<String> tmpPrevMessages = new LinkedList<String>(previousMessages);
        previousMessages = new LinkedList<String>();

        if (dialogFileBase.isEmpty())
        {
            dialogFileBase = findDialogFile(userText, tmpDialogFileBaseJustExited);
            tmpDialogFileBaseJustExited = "";
            if (!dialogFileBase.isEmpty())
                firstEnter = true;

            if (firstEnter)
            {
                fullInfo.put(stateName, "start");
                //TODO: may also want to load information from DB. maybe add a "required" field to dialogIndex file
            }
        }

        if (dialogFileBase.isEmpty())
        {
            List<String> forUser = new LinkedList<String>();
            forUser.add(Consts.closeConnection + Consts.commandChar);
            //forUser = getSocialTalkResponse(asrRes.text);
            sendToUser(messageSender, forUser, false);
        } else
        {
            List<String> forUser = executeDialogFile(userText);
            if (forUser != null && forUser.size() > 0)
                sendToUser(messageSender, forUser, true);

            //check if need to call a function (callfun)
            if (fullInfo.containsKey(callFunName) && !fullInfo.get(callFunName).toString().isEmpty())
            {
                String funToCall = fullInfo.get(callFunName).toString();
                fullInfo.remove(callFunName); //remove it so it won't be called again next time.

                List<String> toSend;
                if (funToCall.equals(callFunRepeat))
                {
                    toSend = tmpPrevMessages;
                } else
                {
                    fullInfo.put(FunctionInvoker.messageFunction, new FunctionInvoker.IMessageSender()
                    {
                        @Override
                        public void sendMessageToUser(List<String> s)
                        {
                            sendToUser(messageSender, s, true);
                        }
                    });
                    toSend = FunctionInvoker.toInvoke(dialogFileBase, funToCall, fullInfo, userId, asrRes);
                }
                sendToUser(messageSender, toSend, true);
            }

            if (fullInfo.get(stateName).equals(dialogReturn))
            {
                if (firstEnter)
                    tmpDialogFileBaseJustExited = dialogFileBase;
                clearDialog();
                return dealWithMessage(asrRes, messageSender); //recall this function and send relevant information
            }

            if (fullInfo.get(stateName).equals(dialogFinish))
            {
                clearDialog();
            }
        }

        if (dialogFileBase.isEmpty() || fullInfo.containsKey(dontRenewConnectionStr))
            return ToDoWithConnection.close;
        if (fullInfo.containsKey(dontCloseConnectionStr))
            return ToDoWithConnection.nothing;
        return ToDoWithConnection.renew;
    }

    private void sendToUser(InMindLogic.MessageReceiver.MessageSender messageSender, List<String> forUser, boolean saveMessage)
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
                    if (saveMessage)
                        previousMessages.add(command);
                }
            }
        }
    }

    private List<String> executeDialogFile(String userSentence)
    {
        List<String> commandsForUser = new LinkedList<String>();
        //String toSay = "";


        final int csvConditions = 0;
        final int csvPattern = 1;
        final int csvSay = 2;
        final int csvSet = 3;
        final int csvAdditionalCommand = 4; //optional


        Path filePath = Paths.get(basePath, dialogFileBase + "." + cstExtension);//Is there a more elegant way to add the extension?
        String line = "";
        BufferedReader bufferedReader = null;

        try
        {
            bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));

            //line = bufferedReader.readLine(); //ignore first line
            while ((line = bufferedReader.readLine()) != null)
            {
                if (line.isEmpty())
                    continue;

                // use comma as separator
                String[] row = line.split(cvsSplitBy, -1);

                Pattern p = Pattern.compile(removeExtraQuotes(row[csvPattern]), Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(userSentence);
                if (m.matches())
                {

                    boolean matchesConditions = true;
                    if (!row[csvConditions].isEmpty())
                    {
                        String conditions = removeExtraQuotes(row[csvConditions]);
                        String[] conditionList = conditions.split(conditionsAndSetChar);
                        for (String cond : conditionList)
                        {
                            if (!checkCondition(cond, fullInfo, m))
                            {
                                matchesConditions = false;
                                break;
                            }
                        }
                    }

                    if (matchesConditions)
                    {
                        //retFile= row[csvFile];
                        String orgToSay = row[csvSay].trim();
                        if (!orgToSay.isEmpty())
                        {
                            String toSay = refactorStringToSay(orgToSay, m);
                            commandsForUser.add(commandSay(toSay));
                        }
                        if (row.length >= csvAdditionalCommand + 1 && !row[csvAdditionalCommand].trim().isEmpty())
                        {
                            commandsForUser.add(row[csvAdditionalCommand].trim());
                        }

                        String toSet = removeExtraQuotes(row[csvSet]);
                        String[] setList = toSet.split(conditionsAndSetChar);
                        for (String singleSet : setList)
                        {
                            if (!singleSet.isEmpty())
                            {
                                String[] varVal = singleSet.split(setEquality);
                                if (varVal.length != 2)
                                    throw new Exception("Error in:" + singleSet);
                                Object setTo = evaluateVal(varVal[1], fullInfo, m);
                                fullInfo.put(varVal[0], setTo);
                            }
                        }

                        break;
                    }
                }


            }

        } catch (Exception ex)
        {

            ex.printStackTrace();
            System.out.println("UserConversation: error in dialog file: ");
            ex.printStackTrace();
        }

        return commandsForUser;
    }

    private void clearDialog()
    {
        dialogFileBase = "";
        fullInfo.clear();
        fullInfo.put(stateName, "");
    }

    // finds the relevant dialog file which appears after considerOnlyAfterMe
    private String findDialogFile(String userSentence, String considerOnlyAfterMe)
    {
        final int csvPattern = 0;
        final int csvFile = 1;
        String retFile = "";
        boolean startConsidering = false;
        if (considerOnlyAfterMe == null || considerOnlyAfterMe.isEmpty())
            startConsidering = true;


        Path filePath = Paths.get(basePath, "dialogIndex.csv");//URI filePath = URI.create("file:///C:/Server/git/Configurations/logic.csv");
        String line = "";
        BufferedReader bufferedReader = null;

        try
        {
            bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));

            while ((line = bufferedReader.readLine()) != null)
            {

                if (line.isEmpty())
                    continue;
                // use comma as separator
                String[] row = line.split(cvsSplitBy, -1);
                if (!startConsidering)
                {
                    if (row[csvFile].equals(considerOnlyAfterMe))
                    {
                        startConsidering = true;
                        continue;
                    }
                }

                if (startConsidering)
                {
                    String forPattern = row[csvPattern];
                    if (!forPattern.isEmpty())
                    {
                        Pattern p = Pattern.compile((forPattern), Pattern.CASE_INSENSITIVE);
                        Matcher m = p.matcher(userSentence);
                        if (m.matches())
                        {
                            retFile = row[csvFile];
                            break;
                        }
                    }
                }

            }

        } catch (Exception e)
        {
            System.out.println("UserConversation: error in finding file");
            e.printStackTrace();
        }
        return retFile;
    }


//    // returns responses
//    // may have multiple (say something and launch something.
//    static public List<String> getSocialTalkResponse(String userSentence)
//    {
//        final int csvPattern = 0;
//        final int csvSay = 1;
//        final int csvAdditionalCommand = 2;
//
//        List<String> response = new LinkedList<String>();
//
//
//        Path filePath = Paths.get(basePath, "socialTalk.csv");//URI filePath = URI.create("file:///C:/Server/git/Configurations/logic.csv");
//        String line = "";
//        BufferedReader bufferedReader = null;
//
//        try
//        {
//            bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));
//
//            while ((line = bufferedReader.readLine()) != null)
//            {
//
//                // use comma as separator
//                String[] row = line.split(cvsSplitBy, -1);
//                Pattern p = Pattern.compile((row[csvPattern]), Pattern.CASE_INSENSITIVE);
//                Matcher m = p.matcher(userSentence);
//                if (m.matches())
//                {
//                    String toSay = refactorStringToSay(row[csvSay], m);
//                    if (!row[csvSay].isEmpty())
//                    {
//                        response.add(commandSay(toSay));
//                    }
//                    if (!row[csvAdditionalCommand].isEmpty())
//                        response.add(row[csvAdditionalCommand]);
//                    break;
//                }
//
//            }
//
//        } catch (Exception ex)
//        {
//            ex.printStackTrace();
//            System.out.println("Logic: error in response");
//        }
//        return response;
//    }

    private static String commandSay(String toSay)
    {
        return Consts.sayCommand + Consts.commandChar + toSay;
    }


    private static String refactorStringToSay(String orgSentence, Matcher m)
    {
        return refactorUsingM(removeExtraQuotes(orgSentence), m);
    }

    // removes extra quotes and takes care of %1 and %r1
    private static String refactorUsingM(String orgSentence, Matcher m)
    {
        if (orgSentence.contains("%"))
        {
            for (int i = 1; i <= m.groupCount(); i++)
            {
                if (orgSentence.contains("%" + i))
                { //replace without reflection
                    orgSentence = orgSentence.replace("%" + i, m.group(i));
                }
                if (orgSentence.contains("%r" + i)) //replace with reflection
                {
                    orgSentence = orgSentence.replace("%r" + i, reflect(m.group(i)));
                }
            }
        }
        return orgSentence;
    }

    private static String removeExtraQuotes(String string)
    {
        string = string.replace("\"\"", "\""); //replace double quotes " " with single "
        string = string.replaceAll("^\"|\"$", ""); //remove quotes from beginning or end of sentence.
        return string;
    }

    private static String reflect(String text)
    {

        final int reflector = 0;
        final int reflectee = 1;

        Path filePath = Paths.get(basePath, "reflection.csv"); //URI filePath = URI.create("file:///C:/Server/git/Configurations/reflection.csv");
        String line = "";
        BufferedReader bufferedReader = null;
        String alreadyReplaced = "~";

        try
        {
            bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));

            while ((line = bufferedReader.readLine()) != null)
            {

                // use comma as separator
                String[] row = line.split(cvsSplitBy, -1);
                String wordsToMatch = "(?i)" + row[reflector]; //ignore case
                String toMatch = "^" + wordsToMatch + " | " + wordsToMatch + " | " + wordsToMatch + "$|^" + wordsToMatch + "$";
                String excludeAlreadyReplaced = toMatch + "(?=([^" + alreadyReplaced + "]*" + alreadyReplaced + "[^" + alreadyReplaced + "]*" + alreadyReplaced + ")*[^" + alreadyReplaced + "]*$)";

                text = text.replaceAll(excludeAlreadyReplaced, "~" + row[reflectee] + "~");
            }
        } catch (Exception e)
        {
            System.out.println("Logic: error in reflection");
            e.printStackTrace();
            return text;
        }

        return text.replaceAll(alreadyReplaced, " ").trim();

    }

    /// returns whether the condition is true or false.
    private boolean checkCondition(String cond, Map<String, Object> fullInfo, Matcher m) throws Exception
    {
        final String condEquality = "=";
        final String condEquality2 = "==";
        final String condNotEqual = "!=";
        final String condGreater = ">";
        final String condGreaterEqual = ">=";
        final String condSmaller = "<";
        final String condSmallerEqual = "<=";
        String[] condRelationList = new String[]{condEquality2, condNotEqual, condGreater, condGreaterEqual, condSmaller, condSmallerEqual, condEquality}; //condEquality must be last

        String relation = null;
        //find relation
        for (String relationOption : condRelationList)
        {
            if (cond.contains(relationOption))
            {
                relation = relationOption;
                break;
            }
        }
        if (relation == null) //no relation found
            throw new Exception("no relation found in " + cond);

        String[] varVal = cond.split(relation);
        if (varVal.length != 2)
            throw new Exception("could not parse: " + cond);

        Object val = evaluateVal(varVal[1], fullInfo, m);
        Object storedVar = fullInfo.get(varVal[0].trim());

        if (storedVar == null || storedVar instanceof String)
        {
            boolean matches = storedVar == val || (storedVar != null && storedVar.equals(val));
            if (relation.equals(condEquality) || relation.equals(condEquality2))
                return matches;
            if (relation.equals(condNotEqual))
                return !matches;
        }

        //must be a number.
        if (relation == condEquality)
            relation = condEquality2;
        Boolean isConditionTrue = false;
        try
        {
            isConditionTrue = (Boolean) engine.eval(storedVar.toString() + relation + val.toString());//can throw exception, it's ok
        } catch (Exception ex)
        {
            System.out.println("Error in evaluating expression");
            ex.printStackTrace();
        }
        return isConditionTrue;
    }

    private Object evaluateVal(String valStr, Map<String, Object> fullInfo, Matcher m) throws Exception
    {
        valStr = valStr.trim();

        if (valStr == "null")
            return null;

        valStr = refactorUsingM(valStr, m);

        if (valStr.startsWith("\"") && valStr.endsWith("\"")) //if is surrounded by quotes, this is a string (no referring to other strings allowed).
            return removeExtraQuotes(valStr);

        //dealing with numeric (no quotes)

        Object requiredVal;
        if (fullInfo.containsKey(valStr)) //check if referring to a different variable
            return fullInfo.get(valStr);

        String tmpValStr = valStr.replaceAll("[!?,\\+\\-\\*/]", " ");
        String[] allRefsToVariables = tmpValStr.split("[ ]+");
        //this may be slow if fullInfo is big. Could look for words separated by space,+,- etc. to speedup if fullInfo is very big.
        for (String refToVar : allRefsToVariables)
        {
            if (fullInfo.containsKey(refToVar))
            {
                if (fullInfo.get(refToVar) == null)
                    throw new Exception("tried to reference null: " + refToVar);
                valStr = valStr.replaceAll(refToVar, fullInfo.get(refToVar).toString());
            }
        }

        return engine.eval(valStr);
    }
}
