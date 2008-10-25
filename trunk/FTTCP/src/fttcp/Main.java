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

public class Main {

    //GLOBAL VARIABLES
    private int delta_seq;
    private int stable_seq;
    private int server_seq;
    private int unstable_reads;
    private boolean restarting;
    private enum States { intial,normal,restarting}
    private States SSWcurrentState = States.intial;
    private short clientAddress;
    private short serverAddress;
    private short loggerAddress;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
    
    /**
     * South Side Wraps thread
     */
    private void SSWrunnable(){
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
        delta_seq = 0;
        unstable_reads = 0;
        restarting = false;
        int clientInitSeqNum;
        
        //Read Clients SYN packet
        byte[] clientSYN = readPacket();
        
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = getInitSeq(clientSYN);
        stable_seq = clientInitSeqNum +1;
        
        byte[] data = intToByteArr(clientInitSeqNum);
        //Send logger Client Initial Seq Num
        sendPacket(data, loggerAddress);
        
        //Send SYN packet to server
        sendPacket(clientSYN,serverAddress);
        
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
        sendPacket(servAck,clientAddress);
        
        //Set currentState to normal
        SSWcurrentState = States.normal;
    }
    
    /**
     * South Side Wraps normal operation protocol
     */
    private void SSWnormalOpp(){
        while(!restarting){
            byte[] receivedPacket = readPacket();
            short sender = getSenderAddress(receivedPacket);
            if (sender == clientAddress){
                //Forward packet to logger
                sendPacket(receivedPacket, loggerAddress);
                
                //Subtracts delta_seq from ACK number, change packets ack#
                int ackNumber = getAckNumber(receivedPacket) - delta_seq;
                receivedPacket = setAckNumber(receivedPacket, ackNumber);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send Packet to server (TCP layer)
                sendPacket(receivedPacket,serverAddress);
            }
            else if (sender == loggerAddress){
                //If ack is for client data packet with seq# from sn->sn+l, and 
                //sn+l+1 > stable_seq, set stable_seq to sn+l+1
            }
            else if (sender == serverAddress){
                //Add delta_seq to sequence#
                int sequenceNo = getSequenceNumber(receivedPacket);
                receivedPacket = setSequenceNumber(receivedPacket, sequenceNo + delta_seq);
                
                //Change ack# to stable_seq
                receivedPacket = setAckNumber(receivedPacket, stable_seq);
                
                //Change advertised window size by adding ack#-stable_seq
                int intWindowSize = getWindowSize(receivedPacket) + getAckNumber(receivedPacket) + stable_seq;
                 receivedPacket = setWindowSize(receivedPacket,intWindowSize);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send to client (IP)
                sendPacket(receivedPacket,clientAddress);
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
        if(getSenderAddress(received) == loggerAddress){
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


    /**
     * Checks packet to see if it's an ACK from server
     * @param received Packet received
     * @return isServAck Boolean true if it is a server ACK
     */
    private boolean isServAck(byte[] received){
        boolean isServAck = false;
        //Check to see if packet is from logger
        if(getSenderAddress(received) == serverAddress){
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
        //IMPLEMENT
        int ackNumber = 0;
        return ackNumber;
    }
    
    /**
     * Sets packets ACK number
     * @param received Packet received
     * @param ackNumber New ack number to be set
     * @return Object recieved Packet, with new ack number
     */
    private byte[] setAckNumber(byte[] received, int newAckNumer){
        //IMPLEMENT
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
        //IMPLEMENT
        boolean isAckPacket = false;
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
        //IMPLEMENT
        int seqNumber = 0;
        return seqNumber;
    }
    
    /**
     * Set Sequence Number for given packet
     * @param received Packet
     * @param seqNumber new sequence number
     * @return Object packet with new sequence number
     */
    private byte[] setSequenceNumber(byte[] received, int seqNumber){
        //IMPLEMENT
        return received;
    }
    
    /**
     * Get Window size from given packet
     * @param received Packet 
     * @return short Sequence Number of packet
     */
    private short getWindowSize(byte[] received){
        //IMPLEMENT
        short seqNumber = 0;
        return seqNumber;
    }
    
    /**
     * Set Window Size for given packet
     * @param received Packet
     * @param windowSize new window size
     * @return Object packet with new window size
     */
    private byte[] setWindowSize(byte[] received, int windowSize){
        //IMPLEMENT
        return received;
    }
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        //IMPLEMENT
        try{
            FileInputStream fileinputstream = new FileInputStream("");
            int numberBytes = fileinputstream.available();
            byte[] bytearray = new byte[numberBytes];
            fileinputstream.read(bytearray);
            return bytearray;
        }
        catch(java.io.FileNotFoundException e){
            return null;
        } 
    }
    
    /**
     * Send packet to address
     * @param object Packet to be sent
     * @param address Place to send it to
     */
    private void sendPacket(byte[] data, short address){
        //IMPLEMENT
        
    }
}
