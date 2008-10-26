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

public class southSideWrap extends Thread{
    
    //Variable declaration
    private enum States { intial,normal,restarting};
    private States SSWcurrentState = States.intial;
    private Main m;
    
    public southSideWrap(Main main){
        m = main;
    }
    
    /**
     * South Side Wraps thread
     */
    @Override
    public void run(){
        byte[] j = readPacket();
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
        m.getRestarting(false);
        int clientInitSeqNum;
        
        //Read Clients SYN packet
        byte[] clientSYN = readPacket();
        
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = getInitSeq(clientSYN);
        m.setStable_seq(clientInitSeqNum +1);
        
        byte[] data = intToByteArr(clientInitSeqNum);
        //Send logger Client Initial Seq Num
        sendPacket(data, m.getLoggerAddress());
        
        //Send SYN packet to server
        sendPacket(clientSYN,m.getServerAddress());
        
        //While both packets aren't received, wait for them
        while(!servAckRecv || !logAckRecv){
            byte[] received = readPacket();
            if (isLogAck(received)){
                logAckRecv = true;
            }
            else if(isServAck(received)){
                servAck = received;
                servAckRecv = true;
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
            short sender = getSenderAddress(receivedPacket);
            if (sender == m.getClientAddress()){
                //Forward packet to logger
                sendPacket(receivedPacket, m.getLoggerAddress());
                
                //Subtracts delta_seq from ACK number, change packets ack#
                int ackNumber = getAckNumber(receivedPacket) - m.getDelta_seq();
                receivedPacket = setAckNumber(receivedPacket, ackNumber);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send Packet to server (TCP layer)
                sendPacket(receivedPacket,m.getServerAddress());
            }
            else if (sender == m.getLoggerAddress()){
                //If ack is for client data packet with seq# from sn->sn+l, and 
                //sn+l+1 > stable_seq, set stable_seq to sn+l+1
            }
            else if (sender == m.getServerAddress()){
                //Add delta_seq to sequence#
                int sequenceNo = getSequenceNumber(receivedPacket);
                receivedPacket = setSequenceNumber(receivedPacket, sequenceNo + m.getDelta_seq());
                
                //Change ack# to stable_seq
                receivedPacket = setAckNumber(receivedPacket, m.getStable_seq());
                
                //Change advertised window size by adding ack#-stable_seq
                //Convert int to short
                int intWindowSize = getWindowSize(receivedPacket) + getAckNumber(receivedPacket) + m.getStable_seq();
                byte[] intArr = intToByteArr(intWindowSize);
                byte[] shortArr = new byte[2];
                shortArr[0] = intArr[0];
                shortArr[1] = intArr[1];
                short windowSize = byteArrayToShort(shortArr,0);
                //Set window size
                receivedPacket = setWindowSize(receivedPacket,windowSize);
                
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
     * Checks to see if given packet is an ACK packet
     * @param received Object to be checked
     * @return boolean True if packet is an ACK packet
     */
    private boolean isAckPacket(byte[] received){
        boolean isAckPacket;
        int flags = received[13];
        String str = Integer.toBinaryString(flags);
        if(str.charAt(3)=='1'){
            isAckPacket = true;
        }
        else isAckPacket = false;
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
        //IMPLEMENT
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
                    //return bytearray;
                    System.out.println(files[0]);
                }
                else{
                    try{
                        System.out.println("Going to sleep");
                        this.sleep(3000);
                        System.out.println("Waking up");
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
            writeFile(data,"serverBuffer/received.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"clientBuffer/received.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
            writeFile(data,"loggerBuffer/received.TCP");
        }
        
    }
    
    private void writeFile(byte[] data, String path){
        try{
            FileOutputStream outStream = new FileOutputStream(path);
            PrintWriter printW = new PrintWriter(outStream);
            printW.print(data);
            printW.flush();
            outStream.close();
        }
        catch(IOException e){

        }
    }

}
