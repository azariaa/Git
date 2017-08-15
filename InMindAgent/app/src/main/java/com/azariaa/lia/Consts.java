package com.azariaa.lia;

/**
 * Created by Amos Azaria on 24-Dec-14.
 */
public class Consts
{
    public static final int serverPort = 4493;
    static public final int sampleRate = 16000;//44100;//16000;

    static public final String commandChar = "^";
    static public final String messageSeparatorForPattern = "\\^";
    static public final String serverMessagePattern = "(\\p{Alpha}*)"+Consts.messageSeparatorForPattern+"(.*)";

    //connection commands from server
    static public final String connectUdp = "ConnectUDP";
    static public final String stopUdp = "StopUDP";
    static public final String sayCommand = "Say";
    static public final String userSaid = "userSaid";
    static public final String launchCommand = "Launch";
    static public final String news = "news";
    static public final String mWExecJson = "execJson";
    static public final String sugilite = "Sugilite";
    static public final String sugiliteStartRecording = "startrec"; //e.g. Sugilite^startrec^script name
    static public final String sugiliteRun = "run"; //e.g. Sugilite^run^script name
    static public final String sugiliteExecJson = "sugExecJson"; //e.g.Sugilite^execJson^{{"variables":{},"nextBlock":{"actionType":"CLICK","childTexts":["Contacts"]}}
    public static final String youTube = "YouTube"; //e.g. YouTube^video:FD23424+dfs3
    static public final String videoPre = "video:";
    static public final String playListPre = "playList:";
    public static final String timerFunctions = "timerFunctions"; //e.g. timeFunctions^{type:"alarm", time:"2017-05-22 17:34:20", say:"get off the computer"}
//    public static final String timeAlarm = "alarm"; //e.g. timeFunctions^alarm^
//    public static final String timeTimer = "timer"; //e.g. timeFunctions^timer^1:23:00
//    public static final String timeReadAll = "readAll"; //e.g. timerFunctions^readAll
//    public static final String timeCancelAll = "cancelAll"; //e.g. timerFunctions^cancelAll
//    public static final String timeReadTime = "readTime"; //e.g. timerFunctions^readTime
//    public static final String timeReadDate = "readDate"; //e.g. timerFunctions^readDate


    static public final String startNewConnection = "StartNewConnection";
    static public final String closeConnection = "CloseConnection";
    static public final String logChangeInSettings = "logChangeInSettings";

    //client requests must start with user Id. e.g., userId^SendingText^text
    static public final String clientMessagePattern = "[-a-zA-Z0-9]+"+Consts.messageSeparatorForPattern+"(\\p{Alpha}*)"+Consts.messageSeparatorForPattern+"(.*)";
    //client requests
    static public final String requestSendAudio = "RequestSendAudio";
    static public final String sendingText = "SendingText";
    static public final String sendingCommand = "SendingCommand";

    static public final String defaultWakeupWord = "jessica";//"exclamation";//"in mind agent";
    static public final int defaultWakeupSensitivity = 12;
}
