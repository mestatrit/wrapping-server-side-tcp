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
        gui.printToScreen("SSW flashing.");
        gui.clt2tcp();
        gui.printToScreen("SSW out");
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
                clientSYN = readPacket();
            }
            //Check to see if SYN
            isSYN = isSYNPacket(clientSYN);
        }
        
        
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = TCP.getSequenceNumber(clientSYN);
        m.setStable_seq(clientInitSeqNum +1);
        
        byte[] data = TCP.convertDataToByteArray(clientInitSeqNum);
        byte[] data2 = new byte[data.length+1];
        data2[0] = initSeqNumFlag;
        for(int i=1;i<data2.length;i++){
            data2[i] = data[i-1];
        }
        //Send logger Client Initial Seq Num
        sendPacket(data, m.getLoggerAddress());
        
        //Send SYN packet to server
        sendPacket(clientSYN,m.getServerAddress());
        
        //While both packets aren't received, wait for them
        while(!servAckRecv || !logAckRecv){
            byte[] received = readPacket();
            if (sender.equals("LOG")){
                logAckRecv = TCP.getACKFlag(received);
            }
            else if(sender.equals("SRV")){
                servAck = received;
                servAckRecv = TCP.getACKFlag(received);
            }
        }
        
        //Send Servers ACK to client
        sendPacket(servAck,m.getClientAddress());
        
        //Set currentState to normal
        SSWcurrentState = States.normal;
    }
    
    /**
     * South Side Wraps normal operation protocol
     */
    private void SSWnormalOpp(){
        while(!m.getRestarting()){
            byte[] receivedPacket = readPacket();
            if (sender.equals("CLT")){
                //Forward packet to logger
                byte[] forwardPacket = TCP.getData(receivedPacket);
                for (int i = forwardPacket.length-1;i>1;i--){
                    forwardPacket[i-1] = forwardPacket[i];
                }
                forwardPacket[0] = fwdCltPacketFlag;
                sendPacket(forwardPacket, m.getLoggerAddress());
                
                //Subtracts delta_seq from ACK number, change packets ack#
                int ackNumber = TCP.getAcknowledgementNumber(receivedPacket) - m.getDelta_seq();
                TCP.setAcknowledgementNumber(ackNumber,receivedPacket);
                
                //Recompute Checksum NEEDS IMPLEMENTING
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send Packet to server (TCP layer)
                sendPacket(receivedPacket,m.getServerAddress());
            }
            else if (sender.equals("LOG")){
                //If ack is for client data packet with seq# from sn->sn+l, and 
                //sn+l+1 > stable_seq, set stable_seq to sn+l+1
                
            }
            else if (sender.equals("SRV")){
                //Add delta_seq to sequence#
                int sequenceNo = TCP.getSequenceNumber(receivedPacket);
                TCP.setSequenceNumber(sequenceNo+m.getDelta_seq(),receivedPacket);
                
                //Change ack# to stable_seqwhile(!m.getRestarting()){
                TCP.setAcknowledgementNumber(m.getStable_seq(),receivedPacket);
                
                //Change advertised window size by adding ack#-stable_seq
                //Convert int to short
                int intWindowSize = TCP.getWindowSize(receivedPacket) + TCP.getAcknowledgementNumber(receivedPacket) - m.getStable_seq();
                //int intWindowSize = getWindowSize(receivedPacket) + getAckNumber(receivedPacket) + m.getStable_seq();
                short windowSize = ByteArray.getShort(TCP.convertDataToByteArray(intWindowSize),0);

                /*byte[] intArr = intToByteArr(intWindowSize);
                byte[] shortArr = new byte[2];
                shortArr[0] = intArr[0];
                shortArr[1] = intArr[1];
                short windowSize = byteArrayToShort(shortArr,0);*/
                //Set window size
                TCP.setWindowSize(windowSize,receivedPacket);
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
     * Checks packet to see if it's an ACK from logger
     * @param received Packet received
     * @return isServAck Boolean true if it is a logger ACK
     */
    private boolean isLogAck(byte[] received){
        boolean isLogAck = false;
        //Check to see if packet is from logger
        if(getSenderAddress(received) == m.getLoggerAddress()){
            //Check to see if its an ACK packet
            if(isAckPacket(received)){
                isLogAck = true;
            }
        }
        return isLogAck;
    }
    
    private byte[] intToByteArr(int num){
        byte[] byteArr = new byte[4];
        byteArr[3] =(byte)( num >> 24 );
        byteArr[2] =(byte)( (num << 8) >> 24 );
        byteArr[1] =(byte)( (num << 16) >> 24 );
        byteArr[0] =(byte)( (num << 24) >> 24 );
        return byteArr;
    }
    
    /**
     * Converts 4 bytes in byte array to an int, starting from offset
     * @param byteArr Byte Array
     * @param offset Place to start in array
     * @return int value of four bytes
     */
    
    private int byteArrayToInt(byte[] byteArr, int offset) {
        int value = byteArr[0];
        for (int i = 1; i < 4; i++) {
            value += byteArr[i+offset] << 8;
        }
        return value;
    }

    private byte[] shortToByteArr(short num){
        byte[] byteArr = new byte[2];
        byteArr[1] =(byte)( num >> 8 );
        byteArr[0] =(byte)( (num << 8) >> 8 );
        return byteArr;
    }
    
    private short byteArrayToShort(byte[] byteArr, int offset){
        short shortVal = byteArr[offset];
        shortVal += byteArr[offset+1] <<8;
        return shortVal;
    }
    
    /**
     * Checks packet to see if it's an ACK from server
     * @param received Packet received
     * @return isServAck Boolean true if it is a server ACK
     */
    private boolean isServAck(byte[] received){
        boolean isServAck = false;
        //Check to see if packet is from logger
        if(getSenderAddress(received) == m.getServerAddress()){
            //Check to see if its an ACK packet
            if(isAckPacket(received)){
                isServAck = true;
            }
        }
        return isServAck;
    }
    
    /**
     * Gets packets sender address
     * @param received Packet received
     * @return Short containing sender address
     */
    private short getSenderAddress(byte[] received){
        //IMPLEMENT
        short address = 0;
        return address;
    }
    
    /**
     * Gets packets ACK number
     * @param received Packet received
     * @return int Packets ACK number
     */
    private int getAckNumber(byte[] received){
        int ackNumber = byteArrayToInt(received,8);
        return ackNumber;
    }
    
    /**
     * Sets packets ACK number
     * @param received Packet received
     * @param ackNumber New ack number to be set
     * @return Object recieved Packet, with new ack number
     */
    private byte[] setAckNumber(byte[] received, int newAckNumber){
        byte[] newAck = intToByteArr(newAckNumber);
        for (int i=0;i<4;i++){
            received[8+i] = newAck[i];
        }
        return received;
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
     * Get Initial Sequence Number from given packet
     * @param received Packet to analyse
     * @return int Initial Sequence Number of packet
     */
    private int getInitSeq(byte[] received){
        //IMPLEMENT
        int initSeq = 0;
        return initSeq;
    }
   
    /**
     * Get Sequence Number from given packet
     * @param received Packet 
     * @return int Sequence Number of packet
     */
    private int getSequenceNumber(byte[] received){
        int seqNumber = byteArrayToInt(received,4);
        return seqNumber;
    }
    
    /**
     * Set Sequence Number for given packet
     * @param received Packet
     * @param seqNumber new sequence number
     * @return Object packet with new sequence number
     */
    private byte[] setSequenceNumber(byte[] received, int seqNumber){
        byte[] newSeq = intToByteArr(seqNumber);
        for (int i=0;i<4;i++){
            received[4+i] = newSeq[i];
        }
        return received;
    }
    
    /**
     * Get Window size from given packet
     * @param received Packet 
     * @return short Sequence Number of packet
     */
    private short getWindowSize(byte[] received){
        short windowSize = byteArrayToShort(received,14);
        return windowSize;
    }
    
    /**
     * Set Window Size for given packet
     * @param received Packet
     * @param windowSize new window size
     * @return Object packet with new window size
     */
    private byte[] setWindowSize(byte[] received, short windowSize){
        byte[] newWindow = shortToByteArr(windowSize);
        for (int i=0;i<2;i++){
            received[14+i] = newWindow[i];
        }
        return received;
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
                if(files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    boolean hadDel = (new File(files[0]).delete());
                    //Find and set sender
                    int length  = files[0].length();
                    String[] info = files[0].split(".");
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
            writeFile(data,"serverBuffer/received.CLT.SRV.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"clientBuffer/received.SRV.CLT.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
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
