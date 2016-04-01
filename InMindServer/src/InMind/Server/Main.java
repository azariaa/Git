package InMind.Server;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class Main
{

    public static void main(String[] args)
    {
        InMindLogic logic = new InMindLogic();
        //SphinxSignalInfoProvider.staticInitialize();
        logic.runServer();
    }
}
