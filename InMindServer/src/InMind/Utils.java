package InMind;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.logging.*;

/**
 * Created by User on 25-Dec-14.
 */
public class Utils
{

    static private final String loggingFile = "./logging/inmindServer.log";

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static Logger createLogger(String className)
    {
        Logger logger = Logger.getLogger(className);
        //Handler consoleHandler = null;
        Handler fileHandler  = null;
        //Creating consoleHandler and fileHandler
        //consoleHandler = new ConsoleHandler();
        try
        {
            fileHandler  = new FileHandler(loggingFile,true);
        } catch (IOException e)
        {
            File file = new File(loggingFile);
            File parent = file.getParentFile();
            if (parent.exists())
            {
                e.printStackTrace();
            }
            else
            {
                parent.mkdir();
                try
                {
                    fileHandler = new FileHandler(loggingFile, true);
                } catch (IOException e2)
                {
                    e2.printStackTrace();
                }
            }

        }
        // Creating SimpleFormatter
        Formatter simpleFormatter = new SimpleFormatter();
        // Setting formatter to the handler
        fileHandler.setFormatter(simpleFormatter);
        //Assigning handlers to logger object
        //logger.addHandler(consoleHandler); //don't need console handler, it is added automatically
        logger.addHandler(fileHandler);

        //Setting levels to handlers and logger
        //consoleHandler.setLevel(Level.ALL);
        fileHandler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        return logger;
    }

    public static void appendToFile(byte soundbytes[], int soundlength, Path filePath)
    {
        FileOutputStream out;
        try
        {
            out = new FileOutputStream(filePath.toFile(), true);
            byte[] toWrite = new byte[soundlength];
            System.arraycopy(soundbytes, 0, toWrite, 0, soundlength);
            out.write(toWrite);
            out.close();
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void delIfExists(Path filePath)
    {
        try
        {
            // Delete if tempFile exists
            File fileTemp = filePath.toFile();
            if (fileTemp.exists())
            {
                fileTemp.delete();
            }
        } catch (Exception e)
        {
            // if any error occurs
            e.printStackTrace();
        }
    }
}
