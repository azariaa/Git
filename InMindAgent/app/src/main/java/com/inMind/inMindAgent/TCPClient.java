package com.inMind.inMindAgent;

import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import InMind.Consts;

/**
 * Created by Amos Azaria on 01-Nov-14.
 */
public class TCPClient
{

    static TCPClient singleton = null;

    static final int connectionTimeout = 1000; // in milliseconds

    String ipAddr;
    int portNum;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private boolean mConnected = false;
    private boolean tryingToConnect = false;

    final Object lock = new Object();
    final Object waitingForStop = new Object();
    final Object waitingForConnect = new Object();
    PrintWriter out;
    BufferedReader in;

    private TCPClient(String ipAddr, int portNum, OnMessageReceived listener)
    {
        Log.d("TCP Client", "C: creating initial TCPClient.");
        mMessageListener = listener;
        this.ipAddr = ipAddr;
        this.portNum = portNum;
    }

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages
     * received from server
     */
    public static TCPClient getTCPClientAndConnect(String ipAddr, int portNum, OnMessageReceived listener)
    {
        if (singleton == null)
        {
            singleton = new TCPClient(ipAddr, portNum, listener);
        }
        else
        {
            singleton.renew(ipAddr, portNum, listener);
        }
        singleton.connect();
        return singleton;
    }

    private void renew(String ipAddr, int portNum, OnMessageReceived listener)
    {
        Log.d("TCP Client", "C: renewing connection.");
        if (mRun)
        {
            mRun = false;
            synchronized (waitingForStop)
            {
                try
                {
                    Log.d("TCP Client", "C: Waiting for close.");
                    waitingForStop.wait();
                    Log.d("TCP Client", "C: closed.");
                }
                catch (Exception ignored)
                {
                }
            }
        }
        Log.d("TCP Client", "C: updating variables.");
        singleton.mMessageListener = listener;
        singleton.ipAddr = ipAddr;
        singleton.portNum = portNum;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message)
    {
        Log.d("TCP Client", "C: sending: " + message);
        if (!tryingToConnect && !mConnected)
        {
            Log.w("TCP Client", "C:tried sending message but not connected, so connecting first. message: " + message);
            connect();
            if (!tryingToConnect && !mConnected)
            {
                Log.e("TCP Client", "C:Error, tried sending message but could not connected. message: " + message);
                return;
            }
        }
        if (tryingToConnect && !mConnected)
        {
            synchronized (waitingForConnect)
            {
                try
                {
                    Log.d("TCP Client", "C: waiting for connection.");
                    waitingForConnect.wait();
                }
                catch (Exception ignored)
                {
                }
            }
        }
        if (mConnected)
        {
            if (out != null && !out.checkError())
            {
                Log.d("TCP Client", "C: printing message. ");
                out.println(message);
                out.flush();
                Log.d("TCP Client", "C: message flushed. ");
            }
            else
                Log.e("TCP Client", "C: error printing message. ");
        }
        else
        {
            Log.e("TCP Client", "C: error, could not connect. ");
        }
    }

    public void closeConnection()
    {
        mRun = false;
    }

    //connects
    private void connect()
    {
        Thread toRun = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mRun = true;

                // create a socket to make the connection with the server
                Socket socket = new Socket();
                try
                {
                    socket.setSoTimeout(connectionTimeout);
                    InetAddress serverAddr = InetAddress.getByName(ipAddr);
                    Log.d("TCP Client", "C: Connecting...");

                    final int maxTrials = 5;
                    for (int i = 0; i < maxTrials; i++)
                    {
                        try
                        {
                            socket.connect(new InetSocketAddress(serverAddr, portNum),
                                    connectionTimeout);
                            break;
                        }
                        catch (Exception ex)
                        {
                            Log.e("TCP Client", "C: problems connecting! i=" + i + " ex=" + ex.getMessage());
                            if (i+1 == maxTrials)
                                throw ex;
                            socket.close();
                            Thread.sleep((i+1)*25);
                            socket = new Socket();
                            socket.setSoTimeout(connectionTimeout);
                        }
                    }

                }
                catch (Exception ex)
                {
                    try
                    {
                        socket.close();
                    }
                    catch (Exception ignored)
                    {
                    }
                    mRun = false;
                    mMessageListener.messageReceived(Consts.sayCommand + Consts.commandChar + "Could not connect.");
                    Log.e("TCP Client", "C: Could not Connect! ex=" + ex.getMessage());
                    tryingToConnect = false;
                    synchronized (waitingForConnect)
                    {
                        waitingForConnect.notifyAll();
                    }
                    return;
                }

                Log.d("TCP Client", "C: Connected!");

                try
                {

                    // send the message to the server
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream())), true);

                    Log.d("TCP Client", "C: Sent.");

                    // receive the message which the server sends back
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));

                    // in this while the client listens for the messages sent by the
                    // server
                    String serverMessage = null;

                    mConnected = true;
                    tryingToConnect = false;
                    synchronized (waitingForConnect)
                    {
                        waitingForConnect.notifyAll();
                    }
                    while (mRun)
                    {
                        Log.d("TCP Client", "inner loop.");
                        try
                        {
                            serverMessage = in.readLine();
                            if (serverMessage == null)
                            {
                                mRun = false;
                            }
                        }
                        catch (Exception ignored)
                        {
                            Log.d("TCP Client", "C: Didn't receive a message");
                        }

                        if (serverMessage != null && mMessageListener != null)
                        {
                            Log.d("TCP Client", "Got a message." + serverMessage);

                            // call the method messageReceived from MyActivity class
                            mMessageListener.messageReceived(serverMessage);
                            serverMessage = null;
                        }
                    }

                    Log.d("RESPONSE FROM SERVER", "S: Received Message: '"
                            + serverMessage + "'");

                }
                catch (Exception e)
                {
                    Log.e("TCP Client", "S: Error", e);
                }
                finally
                {
                    // the socket must be closed. It is not possible to reconnect to
                    // this socket
                    // after it is closed, which means a new socket instance has to be
                    // created.
                    try
                    {
                        socket.close();
                    }
                    catch (Exception e)
                    {
                        Log.e("TCP", "S: Could not close", e);
                    }
                    mConnected = false;
                    tryingToConnect = false;
                    synchronized (waitingForStop)
                    {
                        waitingForStop.notifyAll();
                    }
                }
            }
        });
        tryingToConnect = true;
        toRun.start();

        // Log.e("TCP", "C: could not connect", e);

    }

    // Declare the interface. The method messageReceived(String message) will
    // must be implemented in the MyActivity
    // class at on asynckTask doInBackground
    public interface OnMessageReceived
    {
        public void messageReceived(String message);
    }

}
