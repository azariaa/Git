package InMind.Server.asr;

import InMind.Consts;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Amos Azaria on 16-Dec-14.
 */
public class ASR
{

    DataOutputStream wr = null;
    HttpsURLConnection con = null;

    static public class AsrRes
    {
        public String text;
        public double confidence;
        public String fullJsonRes;
        public List<String> alternatives = new LinkedList<>(); //only alternatives (not including "text")
    }

    public ASR()
    {
    }

    public boolean isConnectionOpen()
    {
        return (wr != null && con != null);
    }

    public void beginTransmission() throws IOException
    {
        String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/15.0.874.121 Safari/535.2",
                url = "https://www.google.com/speech-api/v2/recognize?output=json&lang=en-us&key=AIzaSyChZTv4KdGD56Uuh7uMBXy-YEdaAsSBmpw&client=chromium&maxresults=6&pfilter=2";

        URL obj = new URL(url);
        con = (HttpsURLConnection) obj.openConnection();

        // add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", "audio/l16; rate=" + Consts.sampleRate);
        //con.setRequestProperty("AcceptEncoding", "");

        // Send post request
        con.setDoOutput(true);
        wr = new DataOutputStream(con.getOutputStream());
    }

    public void sendDataAsync(byte[] dataToSend, int length)
    {
        Thread a = new Thread(() -> {
            try
            {
                sendData(dataToSend, length);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        a.start();
    }


    public void sendData(byte[] dataToSend, int length) throws IOException
    {
        if (wr == null)
            beginTransmission();
        wr.write(dataToSend, 0, length);
        wr.flush();
    }

    public interface IAsrGetResponse
    {
        void gotResponse(AsrRes asrRes);
    }

    public void closeAndGetResponseAsync(IAsrGetResponse asrGetResponse)
    {
        Thread a = new Thread(() -> {
            try
            {
                AsrRes asrRes = closeAndGetResponse();
                asrGetResponse.gotResponse(asrRes);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        a.start();
    }

    public AsrRes closeAndGetResponse() throws IOException
    {
        AsrRes res = new AsrRes();
        res.confidence = 0;
        res.text = "";


        wr.flush();
        wr.close();
        wr = null;

        int responseCode = con.getResponseCode();
        //System.out.println("\nSending 'POST' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(
                con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null)
        {
            response.append(inputLine);
        }
        in.close();

        //System.out.println("Google response: " + response.toString());
        res.fullJsonRes = response.toString();
        //remove first empty result (13 chars): {"result":[]}
        List<String> alternatives = new LinkedList<>();
        try
        {
            JSONObject jsonObj = new JSONObject(response.toString().substring(13));
            JSONArray results = jsonObj.getJSONArray("result").getJSONObject(0).getJSONArray("alternative");
            for (int i=0; i<results.length(); i++)
            {
                JSONObject result = results.getJSONObject(i);
                if (i == 0)
                {
                    res.text = result.getString("transcript");
                    if (result.has("confidence"))
                        res.confidence = result.getDouble("confidence");
                }
                else
                {
                    alternatives.add(result.getString("transcript"));
                }
            }
            res.alternatives = alternatives;
        } catch (org.json.JSONException ex)
        {
            System.out.println("Error parsing ASR JSon. json:" + res.fullJsonRes);
        }

        // print result
        //System.out.println(response.toString());
        con = null;

        return res;

    }

    /**
     * get google asr from file (static)
     */
    static public AsrRes getGoogleASRFromFile(Path audioFile)
    {
        try
        {
            ASR asr = new ASR();
            asr.beginTransmission();
            byte[] fileAsByteArr = Files.readAllBytes(audioFile);
            asr.sendData(fileAsByteArr, fileAsByteArr.length);
            AsrRes asrRes = asr.closeAndGetResponse();
            PrintWriter pw = new PrintWriter(audioFile.toString() + ".txt");
            pw.print(asrRes.fullJsonRes);
            pw.flush();
            pw.close();
            return asrRes;

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
