/*
 * ClientTCP.java
 *
 * Created on December 1, 2008, 1:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author csuffk
 */
public class ClientTCP extends TCP {
    
    private int sequenceNumberForSRV = 0;
    private int clientStatusForLOG = TCP.ESTABLISHED;
    private int clientStatusForSRV = TCP.CLOSED;
    private byte[] initialDataFromClient = null;
    
    /** Creates a new instance of ClientTCP */
    public ClientTCP(Main main, String e, GUI g) {
        super(main,e,g);
    }
    
    public void run() {
        
        while (true) {
            // read packet
            //System.out.println("TCP Client: Check buffer");
            byte[] data = readPacket("clientBuffer");
            System.out.println("TCP CLT: Data found in buffer sender: "+sender+" receiver: "+destination+" state "+clientStatusForSRV+" seq "+sequenceNumberForSRV);
            // asumption: connection established

            if (isLoggerConnection() || (isServerConnection() && clientStatusForSRV == TCP.ESTABLISHED)) {
                //System.out.println("Logger connection or established server connection");
                if (direction.equals("received")) {
                    byte[] segContents = TCP.stripHeader(data);
                    // get sequence number
                    int sequenceNumber = TCP.getSequenceNumber(data);

                    System.out.println("TCP CLT receieved: Server connection? "+(!isLoggerConnection())+" seq found "+sequenceNumber+" seq comp "+sequenceNumberForSRV);
                    
                    if (isLoggerConnection()) { // LOG (assume perfect channel)
                        sendPacket(segContents);
                        gui.printToScreen("TCP " +entity +": Sending data to "+destination);
                    } else if (sequenceNumber > sequenceNumberForSRV) { // SRV -- only process data if it has not already been processed
                        sequenceNumberForSRV = sequenceNumber;
                        // send data to app
                        sendPacket(segContents);
                        
                        // next data expected is SeqNum + 1
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setACKFlag(true,newSeg);
                        TCP.setAcknowledgementNumber(sequenceNumberForSRV+1,newSeg);
                        TCP.setSequenceNumber(sequenceNumberForSRV,newSeg);
                        TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);
                        
                        // send ACK
                        gui.clt2srv();
                        gui.ssw2tcp();
                        writeFile(newSeg, "serverBuffer/received.CLT.SRV.TCP"); // bypass SSW
                        
                        gui.printToScreen("TCP " +entity +": Data receieved, sending ACK to"+destination);
                    }
                } else if (direction.equals("toSend")) { // if sendTo add header

                    System.out.println("TCP CLT toSend: Cient connection? "+(!isLoggerConnection())+" seq comp "+sequenceNumberForSRV);
                    

                    if (isLoggerConnection()) { // LOG connection
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setData(data,newSeg);
                        sendPacket(newSeg);
                    } else { // SRV connection
                        //System.out.println("TCP Client toSend SRV connection");
                        // increment sequenceNumber
                        
                        if (initialDataFromClient == null) {
                            sequenceNumberForSRV++; // increment to generate current Sequence number
                        } else {
                            initialDataFromClient = null;
                        }
                        
                        byte[] newSeg = TCP.createTCPSegment();
                        TCP.setData(data,newSeg);
                        TCP.setSequenceNumber(sequenceNumberForSRV,newSeg);
                        TCP.setAcknowledgementNumber(sequenceNumberForSRV+1,newSeg);
                        TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);

                        // wait for ACK
                        // do not proceed from this point until ACK has been receieved (unless timeout => null returned)
                        byte[] result = null;

                        do {
                            // send data (to other entity)
                            //System.out.println("TCP " +entity +": Sending data to "+destination+" , waiting for ACK. seq"+sequenceNumberForSRV);
                            gui.printToScreen("TCP " +entity +": Sending data to "+destination+" , waiting for ACK. seq"+sequenceNumberForSRV);
                            sendPacket(newSeg);
                            
                            result = readACKPacket("clientBuffer","SRV",sequenceNumberForSRV+1); 
                            //System.out.println("TCP Client: end of while for aiting for ACK "+result);

                        } while (result == null);
                        
                        gui.printToScreen("TCP " +entity +": ACK receieved from "+sender);
                        //System.out.println("TCP " +entity +": ACK receieved from "+sender);
                    }
                }
            } else if (isServerConnection()) {
                // assumption: connection setup
                // if receieved strip header
                // if sendTo add header

                switch (clientStatusForSRV) {
                    case TCP.CLOSED:
                         if (data.length == TCP.DATA_SIZE) {
                             sequenceNumberForSRV++; // increment sequence number to initial i.e. 1
                             // set SYN flag
                             byte[] newSeg = TCP.createTCPSegment();
                             TCP.setSYNFlag(true,newSeg);
                             TCP.setSequenceNumber(sequenceNumberForSRV,newSeg);
                             TCP.setAcknowledgementNumber(sequenceNumberForSRV+1,newSeg);
                             TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);

                             gui.printToScreen("TCP " +entity +": Connection closed, sending SYN packet.");
                             // store data from client to send when connection established
                             initialDataFromClient = data; 
                             sendPacket(newSeg); // go through SSW
                             clientStatusForSRV = TCP.SYN_SENT;
                         }
                     break;
                     case TCP.SYN_SENT:
                         if (data.length == TCP.PACKET_SIZE) {
                             if (TCP.getSYNFlag(data) && TCP.getACKFlag(data)) {
                                 // create new TCP seg ACK
                                 byte[] response = TCP.createTCPSegment();
                                 TCP.setACKFlag(true,response);
                                 TCP.setSequenceNumber(sequenceNumberForSRV,response);
                                 TCP.setAcknowledgementNumber(sequenceNumberForSRV+1,response);
                                 TCP.setWindowSize((short) TCP.DATA_SIZE,response);

                                 clientStatusForSRV = TCP.ESTABLISHED;
                                 
                                 gui.clt2srv();
                                 gui.ssw2tcp();
                                 writeFile(response, "serverBuffer/received.CLT.SRV.TCP"); // bypass SSW
                                 gui.printToScreen("TCP " +entity +": Connection established, sending ACK to "+sender);


                                 // put data to send in buffer to be processed and sent to server
                                 writeFile(initialDataFromClient, "clientBuffer/toSend.CLT.SRV.TCP");
                                 gui.printToScreen("TCP " +entity +": Need to send initial data");

                             }
                         }
                     break;
                }   
            }   
        }
            
    }
    
    private boolean isServerConnection() {
        
        if (sender.equals("LOG") || destination.equals("LOG")) {
            return false;
        }
        
        return true;
    }

    private boolean isLoggerConnection() {
        
        if (sender.equals("LOG") || destination.equals("LOG")) {
            return true;
        }
        
        return false;
    }
    
    protected void sendPacket(byte[] data) {
        
        // Client - send message to TCP ultimately Server - clientBuffer/toSend.CLT.SRV.TCP
        if (sender.equals("CLT") && destination.equals("SRV")) {
            // TCP (in client) adds header and marks for SSW - serverBuffer/received.CLT.SRV.SSW
            gui.clt2srv();
            writeFile(data,"serverBuffer/received.CLT.SRV.SSW");
        } 
        // SSW (in server) sends stuff to logger gets ack does stuff mark for client TCP - clientBuffer/received.SRV.CLT.TCP
        else if (sender.equals("SRV") && destination.equals("CLT")) {
            // TCP (in client) strips header and marks for client - clientBuffer/received.SRV.CLT
            gui.tcp2clt();
            writeFile(data,"clientBuffer/received.SRV.CLT");
        } 
    }
    
}
