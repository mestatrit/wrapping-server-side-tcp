/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author James Bossingham
 */
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
        Object servAck = null;
        //Set variables (May need mutex lock on them)
        delta_seq = 0;
        unstable_reads = 0;
        restarting = false;
        int clientInitSeqNum;
        
        //Read Clients SYN packet
        Object clientSYN = readPacket();
        
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = getInitSeq(clientSYN);
        stable_seq = clientInitSeqNum +1;
        
        //Send logger Client Initial Seq Num
        sendPacket(clientInitSeqNum, loggerAddress);
        
        //Send SYN packet to server
        sendPacket(clientSYN,serverAddress);
        
        //While both packets aren't received, wait for them
        while(!servAckRecv || !logAckRecv){
            Object received = readPacket();
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
            Object receivedPacket = readPacket();
            short sender = getSenderAddress(receivedPacket);
            if (sender == clientAddress){
                //Forward packet to logger
                sendPacket(receivedPacket, loggerAddress);
            }
            else if (sender == loggerAddress){
                
            }
            else if (sender == serverAddress){
                
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
    private boolean isLogAck(Object received){
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
    
    /**
     * Checks packet to see if it's an ACK from server
     * @param received Packet received
     * @return isServAck Boolean true if it is a server ACK
     */
    private boolean isServAck(Object received){
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
    private short getSenderAddress(Object received){
        //IMPLEMENT
        short address = 0;
        return address;
    }
    
    /**
     * Gets packets ACK number
     * @param received Packet received
     * @return int Packets ACK number
     */
    private int getAckNumber(Object received){
        //IMPLEMENT
        int ackNumber = 0;
        return ackNumber;
    }
    
    /**
     * Checks to see if given packet is an ACK packet
     * @param received Object to be checked
     * @return boolean True if packet is an ACK packet
     */
    private boolean isAckPacket(Object received){
        //IMPLEMENT
        boolean isAckPacket = false;
        return isAckPacket;
    }
    
    /**
     * Get Initial Sequence Number from given packet
     * @param received Packet to analyse
     * @return int Initial Sequence Number of packet
     */
    private int getInitSeq(Object received){
        //IMPLEMENT
        int initSeq = 0;
        return initSeq;
    }
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private Object readPacket(){
        //IMPLEMENT
        Object received = new Object();
        return received;
    }
    
    /**
     * Send packet to address
     * @param object Packet to be sent
     * @param address Place to send it to
     */
    private void sendPacket(Object object, short address){
        //IMPLEMENT
        
    }
}
