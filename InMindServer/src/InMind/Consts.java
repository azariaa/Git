package InMind;

/**
 * Created by Amos Azaria on 24-Dec-14.
 */
public class Consts
{
    public static final int serverPort = 4493;
    static public final int sampleRate = 16000;//44100;//16000;
    static public final int sampleSizeBits = 16; //should not be changed
    static public final int channels = 1; //should not be changed
    static public final boolean isSigned = true; //should not be changed
    static public final boolean bigEndian = false; //should not be changed

    static public final String commandChar = "^";
    static public final String messageSeparatorForPattern = "\\^";
    static public final String serverMessagePattern = "(\\p{Alpha}*)"+Consts.messageSeparatorForPattern+"(.*)";

    //connection commands from server
    static public final String connectUdp = "ConnectUDP";
    static public final String stopUdp = "StopUDP";
    static public final String sayCommand = "Say";
    static public final String launchCommand = "Launch";
    static public final String execJson = "execJson";
    static public final String startNewConnection = "StartNewConnection";
    static public final String closeConnection = "CloseConnection";

    //client requests must start with user Id. e.g., userId^SendingText^text
    static public final String clientMessagePattern = "([-a-zA-Z0-9]+)"+Consts.messageSeparatorForPattern+"(\\p{Alpha}*)"+Consts.messageSeparatorForPattern+"(.*)";
    //client requests
    static public final String requestSendAudio = "RequestSendAudio";
    static public final String sendingText = "SendingText";

    //connections to other servers
    static public final String asrStr = "ASR";
    static public final String asrCancel = "cancel";
}
