package com.networksockettest.game;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Andreas on 2016-03-18.
 * The listener thread, checking for incoming packets containing connection requests. This is controlled
 * from the CreateServer thread, and terminated there as well.
 */
public class ReceivePacket extends Thread
{
    DatagramSocket dSocket;
    public static final int SOCKETSERVERPORT = 8081;
    private String msg = "msg", error = "No error";
    private Boolean threadRun, connectTrue;

    public ReceivePacket()
    {
        connectTrue = false;
    }

    @Override
    public void run()
    {
        threadRun = true;
        try
        {
            //Create a new DatagramSocket.
            dSocket = new DatagramSocket(SOCKETSERVERPORT, InetAddress.getByName("0.0.0.0"));
            //Activate socket broadcast.
            dSocket.setBroadcast(true);
            //While the server is active, keep looking for connect request packets.
            while(threadRun)
            {
                msg = getClass().getName() + ">>>Ready to receive broadcast packets!";
                //Create a packet buffer for incoming packets.
                byte[] recvBuf = new byte[15000];
                DatagramPacket dPacket = new DatagramPacket(recvBuf, recvBuf.length);
                //Look for incoming packets.
                dSocket.receive(dPacket);
                msg = getClass().getName() + ">>>Discovery packet received from: " + dPacket.getAddress().getHostAddress() + "\n";
                msg += getClass().getName() + ">>>Packet received; data: " + new String(dPacket.getData()).trim() + "\n";
                //Get packet data.
                String message = new String(dPacket.getData()).trim();
                //Check packet data for validity.
                if(message.equals("SERVER_CONNECT_CHECK"))
                {
                    //Packet is valid, set connection status to true.
                    connectTrue = true;
                    //Create data packet to send to requesting unit.
                    byte[] sendData = "SERVER_CONNECT_CONFIRMATION".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dPacket.getAddress(), dPacket.getPort());
                    //Send response packet.
                    dSocket.send(sendPacket);
                    msg += getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress();
                }
                /*if(message.equals("SERVER_CONNECT_REQUEST"))
                {

                }*/
            }
        }catch(IOException e)
        {
            e.printStackTrace();
            error = "Exception: " + e.toString();
        }
    }

    public String getMsg() {return msg;}
    public String getError() {return error;}
    //Check connection state.
    public Boolean connectState() {return connectTrue;}
    //Return connection state back to looking.
    public void confirmConnection() {connectTrue = false;}
    //Stop the broadcast.
    public void stopCatch()
    {
        threadRun = false;
        dSocket.close();
    }
}