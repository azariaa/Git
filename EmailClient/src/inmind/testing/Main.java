package inmind.testing;

import inmind.piazza.PiazzaApi;

/**
 * Created by Amos Azaria on 15-Jul-15.
 */
public class Main
{
    static final String username = "inmindenc@gmail.com";
    static final String password = "CNEdnimni";

    public static void main(String[] args) throws Exception
    {
        //EmailOperations emailOperations = new EmailOperations(username,password,username);
        //emailOperations.printLastEmails(2);
        //emailOperations.sendEmail("Testing Subject", "This is me,\n\n have a nice day!", "amos.azaria@gmail.com");

        PiazzaApi piazzaApi = new PiazzaApi(username,password);
        //piazzaApi.answerQuestion("icqi85vqvc375u","Automatic answer");
        //piazzaApi.followup("icqi85vqvc375u","Automatic followup");
        piazzaApi.askQuestion("ic53erv8juk488", "Automatic question asked", "auto generated", "other");
    }
}
