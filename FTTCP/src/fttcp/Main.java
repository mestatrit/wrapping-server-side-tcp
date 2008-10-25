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
                servAckRecv = false;
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
     * Checks and returns packets sender address
     * @param received Packet received
     * @return Short containing sender address
     */
    private short getSenderAddress(Object received){
        //IMPLEMENT
        short address = 0;
        return address;
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
     * Read packet
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
