/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author James Bossingham
 */
import java.io.*;
import org.knopflerfish.util.ByteArray;

public class southSideWrap extends Thread{
    
    //Variable declaration
    private enum States { intial,normal,restarting};
    private States SSWcurrentState = States.intial;
    private Main m;
    private byte initSeqNumFlag = 3;
    private byte fwdCltPacketFlag = 4;
    private String sender = "x";
    private String destination = "x";
    private GUI gui;
    
    public southSideWrap(Main main, GUI g){
        m = main;
        gui = g;
    }
    
    /**
     * South Side Wraps thread
     */
    @Override
    public void run(){
        gui.printToScreen("SSW Reporting in.");
        //repeats this forever (or until connection ended for good)
        while(true){
            //If in initial state perform intial protocol SSWintial()
            if(SSWcurrentState == States.intial){
                SSWinitial();
            }
            else if(SSWcurrentState == States.normal){
                SSWnormalOpp();
            }
            else if(SSWcurrentState == States.restarting){
                SSWrestarting();
            }
        }
    }
    
    /**
     * South Side Wraps intial connection protocol
     */
    private void SSWinitial(){
        gui.printToScreen("SSW initialising");
        boolean servAckRecv = false; 
        boolean logAckRecv = false;
        byte[] servAck = null;
        //Set variables (May need mutex lock on them)
        m.setDelta_seq(0);
        m.setUnstable_reads(0);
        m.setRestarting(false);
        int clientInitSeqNum;
        byte[] clientSYN = null;
        boolean isSYN = false;
        while(!isSYN){
            //Read Clients packet
            while (!sender.equals("CLT")){
                gui.printToScreen("SSW Waiting to read SYN Packet");
                clientSYN = readPacket();
            }
            //Check to see if SYN
            isSYN = isSYNPacket(clientSYN);
        }
        
        gui.printToScreen("SSW read SYN Packet");
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = TCP.getSequenceNumber(clientSYN);
        m.setStable_seq(clientInitSeqNum +1);
        byte[] packet = new byte[TCP.PACKET_SIZE];
        byte[] data = new byte[TCP.DATA_SIZE];
            
        ByteArray.setInt(clientInitSeqNum,data,1);
        data[0] = initSeqNumFlag;
        TCP.setData(data, packet);
        
        gui.printToScreen("SSW: Sending intial sequence number to logger.");
        //Send logger Client Initial Seq Num
        sendPacket(packet, m.getLoggerAddress());
        gui.printToScreen("SSW: Sending packet to server.");
        //Send SYN packet to server
        sendPacket(clientSYN,m.getServerAddress());
        
        //While both packets aren't received, wait for them
        //UNCOMMENT STUFF WHEN SERV ACKS PACKETS
        while(/*!servAckRecv || */!logAckRecv){
            gui.printToScreen("SSW: Waiting for server and logger ACKs.");
            byte[] received = readPacket();
            if (sender.equals("LOG")){
                byte[] receivedData = TCP.getData(received);
                if(receivedData[0] == 0){
                    gui.printToScreen("SSW: Received LOG ACK.");
                    logAckRecv = true;
                }
            }
            /*else if(sender.equals("SRV")){
                servAck = received;
                gui.printToScreen("SSW: Received SRV ACK.");
                servAckRecv = TCP.getACKFlag(received);
            }*/
        }
        gui.printToScreen("SSW: Sending servers ACK to client.");
        //Send Servers ACK to client
        /*sendPacket(servAck,m.getClientAddress());*/
        
        //Set currentState to normal
        SSWcurrentState = States.normal;
    }
    
    /**
     * South Side Wraps normal operation protocol
     */
    private void SSWnormalOpp(){
        gui.printToScreen("SSW: Normal Operation.");
        while(!m.getRestarting()){
            byte[] receivedPacket = readPacket();
            if (sender.equals("CLT")){
                gui.printToScreen("SSW: Received Client Packet.");
                //Forward packet to logger
                byte[] forwardPacket = TCP.getData(receivedPacket);
                for (int i = forwardPacket.length-1;i>1;i--){
                    forwardPacket[i-1] = forwardPacket[i];
                }
                forwardPacket[0] = fwdCltPacketFlag;
                byte[] fullForwardPacket = new byte[receivedPacket.length];
                for(int i=0;i<receivedPacket.length;i++){
                    fullForwardPacket[i] = receivedPacket[i];
                }
                for(int i = 24;i<fullForwardPacket.length;i++){
                    fullForwardPacket[i] = forwardPacket[i-24];
                }
                gui.printToScreen("SSW: Forwarding Packet To Logger.");
                sendPacket(fullForwardPacket, m.getLoggerAddress());
                
                //Subtracts delta_seq from ACK number, change packets ack#
                int ackNumber = TCP.getAcknowledgementNumber(receivedPacket) - m.getDelta_seq();
                TCP.setAcknowledgementNumber(ackNumber,receivedPacket);
                
                //Recompute Checksum NEEDS IMPLEMENTING
                receivedPacket = recomputeChecksum(receivedPacket);
                
                gui.printToScreen("SSW: Sending Edited Packet To Server.");
                //Send Packet to server (TCP layer)
                sendPacket(receivedPacket,m.getServerAddress());
            }
            else if (sender.equals("LOG")){
                gui.printToScreen("SSW: Received Logger Packet.");
                //If ack is for client data packet with seq# from sn->sn+l, and 
                //sn+l+1 > stable_seq, set stable_seq to sn+l+1
                
            }
            else if (sender.equals("SRV")){
                gui.printToScreen("SSW: Received Packet From Server.");
                //Add delta_seq to sequence#
                int sequenceNo = TCP.getSequenceNumber(receivedPacket);
                TCP.setSequenceNumber(sequenceNo+m.getDelta_seq(),receivedPacket);
                
                //Change ack# to stable_seqwhile(!m.getRestarting()){
                TCP.setAcknowledgementNumber(m.getStable_seq(),receivedPacket);
                
                //Change advertised window size by adding ack#-stable_seq
                //Convert int to short
                //UNCOMMENT WHEN WINDOW SIZE/ACK NUMBER IMPLEMENTED
                /*int intWindowSize = TCP.getWindowSize(receivedPacket) + TCP.getAcknowledgementNumber(receivedPacket) - m.getStable_seq();
                //int intWindowSize = getWindowSize(receivedPacket) + getAckNumber(receivedPacket) + m.getStable_seq();
                short windowSize = ByteArray.getShort(TCP.convertDataToByteArray(intWindowSize),0);

                //Set window size
                TCP.setWindowSize(windowSize,receivedPacket);*/
                //receivedPacket = setWindowSize(receivedPacket,windowSize);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send to client (IP)
                sendPacket(receivedPacket, m.getClientAddress());
            }
        }
        SSWcurrentState = States.restarting;
    }
    
    /**
     * South Side Wraps restarting protocol
     */
    private void SSWrestarting(){
        short closedWindow = 0;
        while(m.getRestarting()){
            //Send Closed window packets to client to keep connection alive
            byte[] closedWindowPacket = TCP.createTCPSegment();
            TCP.setWindowSize(closedWindow, closedWindowPacket);
            sendPacket(closedWindowPacket, m.getClientAddress());
        }
        while(!sender.equals("NSW")){
            byte[] receivedPacket = readPacket();
        }
        //Fabricate SYN Packet that has the initial sequence# of stable_seq, send this to servers TCP layer
        byte[] SYNPacket = TCP.createTCPSegment();
        TCP.setSYNFlag(true, SYNPacket);
        TCP.setSequenceNumber(m.getStable_seq(),SYNPacket);
        sendPacket(SYNPacket,m.getServerAddress());
        while(!sender.equals("SRV")){
            //Capture SRV's responding ACK
            byte[] receivedPacket = readPacket();
        }
        //Reply with fake corresponding ACK
        byte[] ACKPacket = TCP.createTCPSegment();
        TCP.setACKFlag(true,ACKPacket);
        sendPacket(ACKPacket,m.getServerAddress());
  
        SSWcurrentState = States.normal;
    }
    
       

    /**
     * Recomputes a changed Packets checksum
     * @param received Packet to be recomputed
     * @return Object New packet with correct Checksum
     */
    private byte[] recomputeChecksum(byte[] received){
        //IMPLEMENT
        return received;
    }
    
    /**
     * Checks to see if given packet is an SYN packet
     * @param received Object to be checked
     * @return boolean True if packet is an SYN packet
     */
    private boolean isSYNPacket(byte[] received){
        boolean isSYNPacket = TCP.getSYNFlag(received);
        return isSYNPacket;
    }
    
    /**
     * Checks to see if given packet is an ACK packet
     * @param received Object to be checked
     * @return boolean True if packet is an ACK packet
     */
    private boolean isAckPacket(byte[] received){
        boolean isAckPacket = TCP.getACKFlag(received);
        return isAckPacket;
    }
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new SSWFileFilter();
                File f = new File("serverBuffer");
                String[] files = f.list(filter);
                if(files != null && files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    fileinputstream.close();
                    boolean hadDel = (new File("serverBuffer/"+files[0]).delete());
                    //Find and set sender
                    int length  = files[0].length();
                    String[] info = files[0].split("[.]");
                    if(info.length == 3 || info.length == 4){
                        sender = info[1];
                        destination = info[2];
                    }
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
                        this.sleep(3000);
                    }
                    catch(java.lang.InterruptedException e){
                        
                    }
                }
            }
        }
        catch(java.io.FileNotFoundException e){
            return null;
        } 
        catch(java.io.IOException e){
            return null;
        } 
    }
    
    /**
     * Send packet to address
     * @param object Packet to be sent
     * @param address Place to send it to
     */
    private void sendPacket(byte[] data, short address){
        //NEED TO CHECK TO SEE IF FILE EXISTS, IF SO WAIT FOR IT TO GO THEN MAKE FILE
        if(address == m.getServerAddress()){
            //Put in file called received.TCP in server folder
            gui.ssw2tcp();
            writeFile(data,"serverBuffer/received.CLT.SRV.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            gui.srv2clt();
            writeFile(data,"clientBuffer/received.SRV.CLT.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
            gui.srv2log();
            writeFile(data,"loggerBuffer/received.SSW.LOG.TCP");
        }
        
    }
    
/**
     * Writes data array to given path
     * @param data byte[] to be written
     * @param path location to save file
     */
    private void writeFile(byte[] data, String path){
        try{
            FileOutputStream outStream = new FileOutputStream(path);
            PrintWriter printW = new PrintWriter(outStream);
            for (int i=0;i<data.length;i++){
                printW.write((int)data[i]);
            }
            printW.flush();
            outStream.close();
        }
        catch(IOException e){
            System.out.println("SSW Cannot write file to: " + path);
        }
    }

}
