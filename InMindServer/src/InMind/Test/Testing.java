package InMind.Test;

import InMind.DialogFunctions.dialogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Amos Azaria on 21-Oct-15.
 */
public class Testing
{
    static public void main(String args[]) throws Exception
    {
        try
        {
            Map<String, String> retParameters = new HashMap<>();
            //retParameters.put("userId", "my userId");
            //dialogUtils.callServer("https://talkingtothecrowd.org:" + 1606 + "/Ugxe8mCBxzxh21V5Vuh4AaABAQ", retParameters, true);
            //String response = dialogUtils.callServer("http://45.55.172.104:" + 1606 + "/", retParameters, true);
            retParameters.put("Body", "Do you want to meet at 2:00 to discuss about the birthday party for Bob?");
            retParameters.put("Sender", "Alan Black");
            retParameters.put("Subject", "Birthday party");
            String response = dialogUtils.callServer("http://birch.speech.cs.cmu.edu:5000/distract", retParameters, true);
            System.out.println(response);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
