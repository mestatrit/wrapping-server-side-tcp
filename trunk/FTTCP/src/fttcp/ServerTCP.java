/*
 * ServerTCP.java
 *
 * Created on December 1, 2008, 1:35 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;

import java.io.*;

/**
 *
 * @author csuffk
 */
public class ServerTCP extends TCP {
    
    private int sequenceNumberForCLT = 0;
    private int serverStatusForLOG = TCP.ESTABLISHED;
    private int serverStatusForCLT = TCP.LISTEN;
    private int newClientDataPacketsReceieved = 0;
    
    /** Creates a new instance of ServerTCP */
    public ServerTCP(Main main, String e, GUI g) {
        super(main,e,g);
    }
    
    public void run() {
        
        while (true) { 
            // read packet
            byte[] data = readPacket("serverBuffer");
            // asumption: connection established

            //System.out.println("TCP SRV: Data found in buffer sender: "+sender+" receiver: "+destination+" state "+serverStatusForCLT+" seq "+sequenceNumberForCLT);
            if (isLoggerConnection() || (isClientConnection() && serverStatusForCLT == TCP.ESTABLISHED)) {
                
                if (direction.equals("received")) {
                    byte[] segContents = TCP.stripHeader(data);
                    // get sequence number
                    int sequenceNumber = TCP.getSequenceNumber(data);

                    //System.out.println("TCP SRV receieved: Client connection? "+(!isLoggerConnection())+" seq found "+sequenceNumber+" seq comp "+sequenceNumberForCLT);
                    
                    if (isLoggerConnection()) { // LOG (assume perfect channel)
                        sendPacket(segContents);
                        gui.printToScreen("TCP " +entity +": Data receieved from "+sender+", sending to "+destination+" seq "+sequenceNumber);
                    } else if (sequenceNumber > sequenceNumberForCLT || newClientDataPacketsReceieved == 0) { // CLT -- only process data if it has not already been processed
                        newClientDataPacketsReceieved++;
                        sequenceNumberForCLT = sequenceNumber;
                        // send data to app
                        sendPacket(segContents);
                        gui.printToScreen("TCP " +entity +": Data receieved from "+sender+", sending to "+destination);
                        // next data expected is SeqNum + 1
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setACKFlag(true,newSeg);
                        TCP.setAcknowledgementNumber(sequenceNumberForCLT+1,newSeg);
                        TCP.setSequenceNumber(sequenceNumberForCLT,newSeg);
                        TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);
                        // send ACK
                        gui.srv2clt();
                        writeFile(newSeg, "clientBuffer/received.SRV.CLT.TCP"); // bypass SSW
                        //sendPacket(newSeg);
                        gui.printToScreen("TCP " +entity +": Sending ACK");
                    } else if (sequenceNumber == sequenceNumberForCLT) { // closed window
                        
                        if (TCP.getWindowSize(data) == 0) {
                            // keep connection alive (not needed as inifinte loop)
                            gui.printToScreen("TCP " +entity +": Closed window detected.");
                        } else {
                            // duplicate data found - ignore
                            gui.printToScreen("TCP " +entity +": Duplicate packet detected.");
                        }
                    }
                } else if (direction.equals("toSend")) { // if toSend add header

                    System.out.println("TCP SRV toSend: Client connection? "+(!isLoggerConnection())+" seq comp "+sequenceNumberForCLT);
                    
                    if (isLoggerConnection()) { // LOG connection
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setData(data,newSeg);
                        sendPacket(newSeg);
                    } else { // CLT connection
                        // increment sequenceNumber
                        sequenceNumberForCLT++; // increment to generate current Sequence number
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setData(data,newSeg);
                        TCP.setSequenceNumber(sequenceNumberForCLT,newSeg);
                        TCP.setAcknowledgementNumber(sequenceNumberForCLT+1,newSeg);
                        TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);

                        // wait for ACK
                        // do not proceed from this point until ACK has been receieved (unless timeout => null returned)
                        byte[] result = null;

                        do {
                            //checkForHeartbeats();
                            // send data (to other entity)
                            sendPacket(newSeg);
                            gui.printToScreen("TCP " +entity +": Sending data to "+destination+" , waiting for ACK.");
                            //System.out.println("TCP " +entity +": Sending data to "+destination+" , waiting for ACK.");
                            result = readACKPacket("serverBuffer","CLT",sequenceNumberForCLT); 
                        } while (result == null);
                        
                        gui.printToScreen("TCP " +entity +": ACK receieved from "+sender);
                        //System.out.println("TCP " +entity +": ACK receieved from "+sender);
                    }
                }
            } else if (isClientConnection()) {
                // assumption: connection setup
                // if receieved strip header
                // if sendTo add header

                switch (serverStatusForCLT) {
                    case TCP.LISTEN:
                        if (data.length == TCP.PACKET_SIZE) {
                            // check if SYN bit set
                            if (TCP.getSYNFlag(data)) {
                                gui.printToScreen("TCP " +entity +": Received SYN Packet");
                                // create new TCP seg with SYN, ACK
                                int sequenceNumberForCLT = TCP.getSequenceNumber(data);
                                byte[] response = TCP.createTCPSegment();
                                TCP.setSYNFlag(true,response);
                                TCP.setACKFlag(true,response);
                                TCP.setSequenceNumber(sequenceNumberForCLT,response);
                                TCP.setAcknowledgementNumber(sequenceNumberForCLT,response);
                                TCP.setWindowSize((short) TCP.DATA_SIZE,response);

                                // send data to client TCP
                                gui.srv2clt();
                                writeFile(response, "clientBuffer/received.SRV.CLT.TCP"); // bypass SSW
                                gui.printToScreen("TCP " +entity +": sending SYN, ACK packet");

                                // change state
                                serverStatusForCLT = TCP.SYN_RCVD;
                            }
                        }
                    break;
                    case TCP.SYN_RCVD:
                        if (data.length == TCP.PACKET_SIZE) {
                            if (TCP.getACKFlag(data)) {
                                gui.printToScreen("TCP " +entity +": ACK receieved, connection established.");
                                serverStatusForCLT = TCP.ESTABLISHED;
                            }
                        }
                    break;
                }   
            } 
        }    
    }
    
    private boolean isClientConnection() {
        
        if (sender.equals("CLT") || destination.equals("CLT")) {
            return true;
        }
        
        return false;
    }

    private boolean isLoggerConnection() {
        
        if (sender.equals("LOG") || destination.equals("LOG")) {
            return true;
        }
        
        return false;
    }
    
    protected void sendPacket(byte[] data) {
        
        // NSW serverBuffer/toSend.NSW.LOG.TCP -- sending inital  message to TCP (ultimately LOG)
        if (sender.equals("NSW") && destination.equals("LOG")) {
            // TCP (in server) reads first - adds header - loggerBuffer/received.NSW.LOG.TCP (message is in server buffer need to process and put in logger buffer)
            gui.tcp2ssw();
            gui.srv2log();
            writeFile(data,"loggerBuffer/received.NSW.LOG.TCP");
        }
        // TCP (in logger) adds header and marks for NSW in server - serverBuffer/received.LOG.NSW.TCP
        else if (sender.equals("LOG") && destination.equals("NSW")) {
            // TCP (in server) strips header and marks for NSW in server - serverBuffer/received.LOG.NSW
            gui.tcp2nsw();
            writeFile(data,"serverBuffer/received.LOG.NSW");
        }
        // SSW (in server) plays with it sends to logger gets ack then marks for TCP - serverBuffer/received.CLT.SRV.TCP
        else if (sender.equals("CLT") && destination.equals("SRV")) {
            // TCP (in server) strips header and marks for NSW (ultimately server) - serverBuffer/received.CLT.SRV.NSW
            gui.tcp2nsw();
            writeFile(data,"serverBuffer/received.CLT.SRV.NSW");
        }
        // NSW (in server) send data to TCP serverBuffer/toSend.SRV.CLT.TCP
        else if (sender.equals("SRV") && destination.equals("CLT")) {
            // TCP (in server) adds header mark for SSW - serverBuffer/toSend.SRV.CLT.SSW
            gui.tcp2ssw();
            writeFile(data,"serverBuffer/toSend.SRV.CLT.SSW");
        }  
        // handling heartbeat

        // Heartbeat (in server) sends data to logger (but TCP first) - serverBuffer/toSendHeartbeat.SRV.LOG.TCP
        else if (sender.equals("SRV") && destination.equals("LOG")) {
            // TCP (in server) adds header then sends to logger (first to TCP) - loggerBuffer/receivedHeartbeat.SRV.LOG.TCP
            gui.htcp2ssw();
            gui.hsrv2log();
            writeFile(data,"loggerBuffer/receivedHeartbeat.SRV.LOG.TCP");
        }
        else if (sender.equals("LOG") && destination.equals("SRV")) {
            // TCP (in server) adds header mark for SSW - serverBuffer/toSend.SRV.CLT.SSW
            gui.tcp2nsw();
            writeFile(data,"serverBuffer/received.LOG.SRV.NSW");
        }  
    }
    
    
    
}
