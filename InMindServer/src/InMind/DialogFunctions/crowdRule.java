package InMind.DialogFunctions;

import InMind.Server.asr.ASR;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;


/**
 * Created by Amos Azaria on 29-Dec-14.
 */
public class crowdRule
{

    static class SayOrJSon
    {
        enum SayOrJSonType
        {
            say, execJson
        }


        public SayOrJSon(SayOrJSonType sayOrJSonType, String content)
        {
            this.sayOrJSonType = sayOrJSonType;
            this.content = content;
        }

        SayOrJSonType sayOrJSonType;
        public String content;
    }

    static final String ip = "45.55.172.104"; //"localhost" //"45.55.172.104";
    static final int crowdPort = 1606;
    static final int listenPort = 1607;
    static final long maxWaitForCrowd = 10 * 60 * 1000;
    static HttpServer server;
    static final Object synchronizeUsers = new Object();
    static Map<String, FunctionInvoker.IMessageSender> connectedUserHandles = new HashMap<>();
    //static Map<String, Object> notifiers = new HashMap<>(); change to connectedUserIdHashSet
    //static Map<String, SayOrJSon> contents = new HashMap<>(); remove

    static
    {
        try
        {
            server = HttpServer.create(new InetSocketAddress(listenPort), 0);
            HttpContext httpContext = server.createContext("/crowdListener", new HttpHandler()
            {
                @Override
                public void handle(HttpExchange httpExchange) throws IOException
                {
                    String userId = null;
                    SayOrJSon sayOrJSon = null;
                    FunctionInvoker.IMessageSender messageSender = null;
                    try
                    {
                        Map<String, Object> parameters = dialogUtils.bodyAsParms(httpExchange);
                        //Map<String, Object> parameters = (Map<String, Object>) httpExchange.getAttribute(ParameterFilter.parametersStr);
                        String response;
                        if (!parameters.containsKey("userId"))
                            response = "Error! no 'userId' found";
                        else if (!parameters.containsKey("messageType"))
                            response = "Error! no 'messageType' found";
                        else if (!parameters.containsKey("content"))
                            response = "Error! no 'content' found";
                        else if (!parameters.get("messageType").equals("say") && !parameters.get("messageType").equals("execRule"))
                            response = "Error! 'messageType' must be 'say' or 'execRule'";
                        else
                        {
                            userId = (String) parameters.get("userId");
                            synchronized (synchronizeUsers)
                            {
                                if (!connectedUserHandles.containsKey(userId))
                                    response = "Error! user not connected!";
                                else
                                {
                                    messageSender = connectedUserHandles.get(userId);
                                    sayOrJSon = new SayOrJSon(parameters.get("messageType").equals("say") ? SayOrJSon.SayOrJSonType.say : SayOrJSon.SayOrJSonType.execJson, (String) parameters.get("content"));
                                    response = "ok";
//                                contents.put(userId, new SayOrJSon(parameters.get("messageType").equals("say") ? SayOrJSon.SayOrJSonType.say : SayOrJSon.SayOrJSonType.execJson, (String) parameters.get("content")));
//                                if (notifiers.containsKey(userId))
//                                {
//                                    synchronized (notifiers.get(userId))
//                                    {
//                                        notifiers.get(userId).notify();
//                                    }
//                                    response = "ok";
//                                }
//                                else
//                                    response = "error! notifier not found";
                                }
                            }
                        }
                        httpExchange.sendResponseHeaders(200, response.length());
                        OutputStream os = httpExchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();

                        if (messageSender != null && sayOrJSon != null)
                            sendMessageToUser(messageSender, sayOrJSon, userId);
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            });
            //httpContext.getFilters().add(new ParameterFilter());
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static List<String> initiateCrowd(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText)
    {
        return initiateEndCrowd(fullInfo, userId, userText, true);
    }

    public static List<String> endCrowd(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText)
    {
        return initiateEndCrowd(fullInfo, userId, userText, false);
    }

    public static List<String> initiateEndCrowd(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText, boolean init)
    {
        List<String> ret = new LinkedList<String>();

        try
        {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("userId", userId);
            parameters.put("messageType", init ? "initiate" : "end");
            String response = dialogUtils.callServer("http://" + ip + ":" + crowdPort + "/", parameters, true);
            if (!init)
            {
                synchronized (synchronizeUsers)
                {
                    if (connectedUserHandles.containsKey(userId))
                        connectedUserHandles.remove(userId);
                }
                return new LinkedList<>();
            }
            else if (response.contains("ok"))
            {
                synchronized (synchronizeUsers)
                {
                    connectedUserHandles.put(userId, (FunctionInvoker.IMessageSender) fullInfo.get(FunctionInvoker.messageFunction));
                }
                return Collections.singletonList(FunctionInvoker.sayStr + "Great! Go ahead!");//response);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        //fullInfo.put(UserConversation.stateName,UserConversation.dialogFinish);
        return Collections.singletonList(FunctionInvoker.sayStr + "Could not initialize crowd rule server.");
    }

    public static List<String> forwardToCrowdRule(Map<String, Object> fullInfo, String userId, ASR.AsrRes userText)
    {
        List<String> ret = new LinkedList<String>();

        try
        {
            synchronized (synchronizeUsers)
            {
                connectedUserHandles.put(userId, (FunctionInvoker.IMessageSender) fullInfo.get(FunctionInvoker.messageFunction));
            }
//            if (contents.containsKey(userId)) //this shouldn't really happen
//            {
//                System.out.println("For some reason userId:" + userId +", had an entry.");
//                contents.remove(userId);
//            }
            Map<String, String> parameters = new HashMap<>();
            parameters.put("userId", userId);
            parameters.put("messageType", "userSays");
            parameters.put("userText", userText.text);
            String callResponse = dialogUtils.callServer("http://" + ip + ":" + crowdPort + "/", parameters, true);
            List<String> responseForUser = Collections.singletonList(FunctionInvoker.sayStr + "Sorry, but there seems to be a problem...");
            if (callResponse.contains("ok"))
            {
                responseForUser = new LinkedList<>();
            }
            return responseForUser;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            return Collections.singletonList(FunctionInvoker.sayStr + "Connection with crowd rule server was lost.");
        }
    }

    private static void sendMessageToUser(FunctionInvoker.IMessageSender messageSender, SayOrJSon sayOrJSon, String userId)
    {
        List<String> sendToUser;
        if (sayOrJSon.sayOrJSonType == SayOrJSon.SayOrJSonType.say)
        {
            sendToUser = Collections.singletonList(FunctionInvoker.sayStr + sayOrJSon.content);//FunctionInvoker.execJson + contents.get(userId);
        }
        else
        {
            sendToUser = new LinkedList<>();
            sendToUser.add(FunctionInvoker.execJson + sayOrJSon.content);
            sendToUser.add(FunctionInvoker.sayStr + "Rule added successfully!");
            sendToUser.add(FunctionInvoker.launch + "https://talkingtothecrowd.org/ifthenelse/checkRules.php?user="+userId);
        }
        messageSender.sendMessageToUser(sendToUser);
        //                if (!notifiers.containsKey(userId))
//                    notifiers.put(userId, new Object());
//                if (!contents.containsKey(userId))
//                {
//                    synchronized (notifiers.get(userId))
//                    {
//                        notifiers.get(userId).wait(maxWaitForCrowd);
//                    }
//                }
//                if (contents.containsKey(userId))
//                {
//                    SayOrJSon sayOrJSon = contents.get(userId);
//                    contents.remove(userId);
//                    if (sayOrJSon.sayOrJSonType == SayOrJSon.SayOrJSonType.say)
//                    {
//                        responseForUser = Collections.singletonList(FunctionInvoker.sayStr + sayOrJSon.content);//FunctionInvoker.execJson + contents.get(userId);
//                    }
//                    else
//                    {
//                        responseForUser = new LinkedList<>();
//                        responseForUser.add(FunctionInvoker.execJson + sayOrJSon.content);
//                        responseForUser.add(FunctionInvoker.sayStr + "Rule added successfully!");
//                    }
//                }
//                else
//                {
//                    responseForUser = Collections.singletonList(FunctionInvoker.sayStr + "Sorry, but I got no answer...");
//                }
    }
}
