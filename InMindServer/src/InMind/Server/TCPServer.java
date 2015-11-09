package InMind.Server;

import InMind.Consts;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The class extends the Thread class so we can receive and send messages at the same time
 */
public class TCPServer extends Thread
{

    private boolean running = false;
    private PrintWriter mOut;
    private OnMessageReceived messageListener;

    static final String clientConnected = "Client Connected";
    static final int readTimeout = 1000;

    static final Object serverLock = new Object();
    static ServerSocket serverSocket = null; //TODO: protect from multithread access

    Socket client = null;


    /**
     * Constructor of the class
     *
     * @param messageListener listens for the messages
     */
    public TCPServer(OnMessageReceived messageListener)
    {
        this.messageListener = messageListener;
    }

    /**
     * Method to send the messages from server to client
     *
     * @param message the message sent by the server
     */
    public void sendMessage(String message)
    {
        if (mOut != null && !mOut.checkError())
        {
            mOut.println(message);
            mOut.flush();
        }
    }

    @Override
    public void run()
    {
        super.run();

        running = true;


        try
        {

            //sends the message to the client
            mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);

            //read the message received from client
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            messageListener.messageReceived(clientConnected);
            //in this while we wait to receive messages from client (it's an infinite loop)
            //this while it's like a listener for messages
            while (running)
            {
                String message = null;
                try
                {
                    message = in.readLine();
                    if (message == null) //EOS (End Of Stream)
                        running = false;
                }
                catch (Exception ignored) //probably timeout exception, which is ok.
                {
                }

                if (running && message != null && messageListener != null)
                {
                    //call the method messageReceived from ServerBoard class
                    messageListener.messageReceived(message);
                }
            }

        } catch (Exception e)
        {
            System.out.println("S: Error");
            e.printStackTrace();
        } finally
        {
            try
            {
                client.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            System.out.println("S: Done.");
        }
    }

    /*
    should be called first. Listens for connection.
     */
    public void listenForConnection() throws IOException
    {
        System.out.println("S: Connecting...");


        //create a server socket. A server socket waits for requests to come in over the network.
        if (serverSocket == null)
        {
            synchronized (serverLock)
            {
                if (serverSocket == null)
                {
                    serverSocket = new ServerSocket(Consts.serverPort);
                    serverSocket.setSoTimeout(readTimeout);
                }
            }
        }

        while (client == null)
        {
            synchronized (serverLock)
            {
                //create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
                try
                {
                    client = serverSocket.accept();
                } catch (Exception ignored)
                {
                }
            }
        }
        System.out.println("S: Accepted...");
    }

    //Declare the interface.
    public interface OnMessageReceived
    {
        void messageReceived(String message);
    }

    public void abandonClient()
    {
        running = false;
    }

    public void stopServer()
    {
        running = false;
        serverSocket = null;
    }

}
