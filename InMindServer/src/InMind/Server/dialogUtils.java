package InMind.Server;

import com.sun.deploy.net.URLEncoder;
import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by Amos Azaria on 08-Oct-15.
 */
public class dialogUtils
{
    static private final String USER_AGENT = "Mozilla/5.0";

    public static String callServer(String url, Map<String, String> parameters, boolean isJsonFormat) throws Exception
    {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

//        try
//        {
            HttpPost request = new HttpPost(url);
            if (isJsonFormat)
                request.addHeader("Content-Type","application/json");//request.addHeader("Content-Type","text/plain");//"content-type", "application/x-www-form-urlencoded");
            else
                request.addHeader("Content-Type","application/x-www-form-urlencoded");


            //HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //using post
            //con.setRequestMethod("POST");
            //con.setRequestProperty("User-Agent", USER_AGENT);
            //con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            //if (isJsonFormat)
            //con.setRequestProperty("Content-Type", "application/json");

            boolean firstParam = true;
            StringBuilder parms = new StringBuilder();
            JSONObject jsonObject = null;
            if (isJsonFormat)
            {
                jsonObject = new JSONObject();
                //parms.append(" {");
            }
            for (String parm : parameters.keySet())
            {
                if (!firstParam) //if not first
                {
//                    if (isJsonFormat)
//                        parms.append(",");
//                    else
                    if (!isJsonFormat)
                        parms.append("&");
                }
                if (isJsonFormat)
                {
                    jsonObject.accumulate(parm, parameters.get(parm));
                    //if valid json, don't add the quotes
                    //parms.append("\"" + parm + "\":\"" + parameters.get(parm) + "\""); //building a json, should actually do it right...
                }
                else
                {
                    parms.append(parm + "=" + URLEncoder.encode(parameters.get(parm), "UTF-8"));
                }
                firstParam = false;
            }
            if (isJsonFormat)
                parms.append(" ").append(jsonObject.toString());//for some odd reason the first byte doesn't arrive, so I add extra " " //parms.append("}");

            // Send post request
            //DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            //wr.writeBytes(parms.toString());
            //wr.flush();
            //wr.close();

            request.setEntity(new StringEntity(parms.toString()));//, ContentType.APPLICATION_JSON));
            HttpResponse httpResponse = httpClient.execute(request);

            //int responseCode = con.getResponseCode();
            if (httpResponse.getStatusLine().getStatusCode() != 200)
            {
                System.out.println("S: error. (response code is: " + httpResponse.getStatusLine().getStatusCode() + ")");
            }

        HttpEntity entity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

            //String response = new BasicResponseHandler().handleResponse(httpResponse);//httpResponse.getEntity().toString();
            return responseString;
//                    BufferedReader in = new BufferedReader(
//                new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//
//
//            while ((inputLine = in.readLine()) != null)
//            {
//                response.append(inputLine + "\n");
//            }
//            in.close();

            // handle response here...
//        } catch (Exception ex)
//        {
//            //ex.printStackTrace();
//        }

        //return response.toString();
        //return "";
    }

//    static String convertStreamToString(java.io.InputStream is)
//    {
//        try (java.util.Scanner s = new java.util.Scanner(is))
//        {
//            return s.hasNext() ? s.next() : "";
//        }
//    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }


    private static Map<String, Object> jsonToMap(JSONObject json) throws JSONException
    {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if (json != JSONObject.NULL)
        {
            retMap = toMap(json);
        }
        return retMap;
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException
    {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext())
        {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray)
            {
                value = toList((JSONArray) value);
            }

            else if (value instanceof JSONObject)
            {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException
    {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++)
        {
            Object value = array.get(i);
            if (value instanceof JSONArray)
            {
                value = toList((JSONArray) value);
            }

            else if (value instanceof JSONObject)
            {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static Map<String, Object> bodyAsParms(HttpExchange httpExchange)
    {
        InputStream is = httpExchange.getRequestBody();
        String parms = dialogUtils.getStringFromInputStream(is);//.convertStreamToString(is);
        JSONObject jsonParms = new JSONObject(parms);
        return dialogUtils.jsonToMap(jsonParms);
    }
}
