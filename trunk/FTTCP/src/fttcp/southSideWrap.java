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
    
    //Set intial state to 'intial' (yes its spelt wrong :p)
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
        //repeats this mode check forever 
        while(true){
            //If in initial state perform intial protocol SSWintial()
            if(SSWcurrentState == States.intial){
                SSWinitial();
            }
            //If normal state perform SSWnormalOpp(), which runs normal operation as defined by the paper
            else if(SSWcurrentState == States.normal){
                SSWnormalOpp();
            }
            //If system in restarting mode switch the this method, as defined by paper
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
        
        //set local variables
        boolean servAckRecv = false; 
        boolean logAckRecv = false;
        byte[] servAck = null;
        int clientInitSeqNum;
        byte[] clientSYN = null;
        boolean isSYN = false;
        
        //Set global variables as algorithm dictates
        m.setDelta_seq(0);
        m.setUnstable_reads(0);
        m.setRestarting(false);

        //Wait for initial client packet, and check it is a SYN packet, else discard and wait again
        while(!isSYN){
            //Read Clients packet
            while (!sender.equals("CLT")){
                gui.printToScreen("SSW Waiting to read SYN Packet");
                clientSYN = readPacket(false);
            }
            //Check to see if SYN
            isSYN = isSYNPacket(clientSYN);
        }
        
        gui.printToScreen("SSW read SYN Packet");
        
        //Set stable_seq as clients initial seq num + 1
        clientInitSeqNum = TCP.getSequenceNumber(clientSYN);
        m.setStable_seq(clientInitSeqNum +1);
        
        //Create new tcp segment and data segment
        byte[] packet = new byte[TCP.PACKET_SIZE];
        byte[] data = new byte[TCP.DATA_SIZE];
           
        //Put the clients data in the new data segment, offset by one
        ByteArray.setInt(clientInitSeqNum,data,1);
        
        //Set the first byte to be the initial sequence number flag,
        //This allows Logger to understand what is in the packet it receives
        data[0] = initSeqNumFlag;
        
        //Set the new tcp packets data to be the new data segment.
        TCP.setData(data, packet);
        
        gui.printToScreen("SSW: Sending intial sequence number to logger.");
        //Send logger Client Initial Seq Num
        sendPacket(packet, m.getLoggerAddress());
        
        gui.printToScreen("SSW: Sending packet to server.");
        //Send SYN packet to server
        sendPacket(clientSYN,m.getServerAddress());
        
        //While both packets aren't received, wait for them
        //UNCOMMENT FOR IF REAL TCP IS BEING USED
        while(/*!servAckRecv || */!logAckRecv){
            gui.printToScreen("SSW: Waiting for server and logger ACKs.");
            byte[] received = readPacket(false);
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
        
        //Finished Initialising, so switch to normal mode
        SSWcurrentState = States.normal;
    }
    
    /**
     * South Side Wraps normal operation protocol
     */
    private void SSWnormalOpp(){
        gui.printToScreen("SSW: Normal Operation.");
        
        //Read packet from buffer, passing variable true, which means that the
        //readPacket method checks the state of m.getRestarting(), so if the 
        //system moves to restart mode whilst its looking for a packet it will
        //exit the method, and not lock
        byte[] receivedPacket = readPacket(true);
        
        //Repeat loop until global variable restarting is true, which means
        //server has died and restarted, so SSW needs to switch states.
        while(!m.getRestarting()){
            //Once packet is received check sender (either client(CLT), logger(LOG) or 
            //server (SRV)), and act according to algorithm.
            if (sender.equals("CLT")){
                gui.printToScreen("SSW: Received Client Packet.");
                
                //Create new packet to forward to logger, called fullForwardPacket,
                //Create new data packet called forwardPacket, put orginal data in,
                //shift data by one, and add flag to tell logger that this is a
                //forwarded packet.
                byte[] forwardPacket = TCP.getData(receivedPacket);
                forwardPacket[3] = forwardPacket[2];
                forwardPacket[2] = forwardPacket[1];
                forwardPacket[1] = forwardPacket [0];
                forwardPacket[0] = fwdCltPacketFlag;
                byte[] fullForwardPacket = new byte[receivedPacket.length];
                for(int i=0;i<receivedPacket.length;i++){
                    fullForwardPacket[i] = receivedPacket[i];
                }
                for(int i = 24;i<fullForwardPacket.length;i++){
                    fullForwardPacket[i] = forwardPacket[i-24];
                }
                gui.printToScreen("SSW: Forwarding Packet To Logger.");
                //Send the newly created forward packet to logger
                sendPacket(fullForwardPacket, m.getLoggerAddress());
                
                //Subtracts delta_seq from ACK number, change packets ack#,
                //As stated in algorithm
                int ackNumber = TCP.getAcknowledgementNumber(receivedPacket) - m.getDelta_seq();
                TCP.setAcknowledgementNumber(ackNumber,receivedPacket);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                gui.printToScreen("SSW: Sending Edited Packet To Server.");
                //Send Packet to server (TCP layer)
                sendPacket(receivedPacket,m.getServerAddress());
            }
            
            else if (sender.equals("LOG")){
                gui.printToScreen("SSW: Received Logger Packet.");
                //If ack is for client data packet with seq# from sn->sn+l, and 
                //sn+l+1 > stable_seq, set stable_seq to sn+l+1.
                //As this implementation uses fixed window/data lengths we have
                //simplified this just to set stable seq to be the sequence 
                //number.
                m.setStable_seq(TCP.getSequenceNumber(receivedPacket));
            }
            
            else if (sender.equals("SRV")){
                gui.printToScreen("SSW: Received Packet From Server.");
                
                /**************************************************************
                 *** Basically if packet from server going to client, remap tcp ***
                 *** header variables appropriately (According to algorithm), to***
                 *** make sure that if the server dies and comes back to life,  ***
                 *** then the client doesnt notice that sequence# has changed   ***
                 ****************************************************************/
                
                //Add delta_seq to sequence#, as stated in algorithm
                //REMOVE COMMENTS IF REAL TCP IMPLEMENTED
                int sequenceNo = TCP.getSequenceNumber(receivedPacket);
                TCP.setSequenceNumber(sequenceNo /*+ m.getDelta_seq()*/,receivedPacket);
                
                //Change ack# to stable_seq, as stated in algorithm
                TCP.setAcknowledgementNumber(m.getStable_seq(),receivedPacket);
                
                //Change advertised window size by adding ack#-stable_seq
                //Convert int to short
                //NOT NEEDED AS WE ARE USING FIXED WINDOWS!
                //UNCOMMENT STUFF BETWEEN /**/ IF REAL TCP IMPLEMENTED
                /*int intWindowSize = TCP.getWindowSize(receivedPacket) + TCP.getAcknowledgementNumber(receivedPacket) - m.getStable_seq();
                //int intWindowSize = getWindowSize(receivedPacket) + getAckNumber(receivedPacket) + m.getStable_seq();
                short windowSize = ByteArray.getShort(TCP.convertDataToByteArray(intWindowSize),0);

                //Set window size
                TCP.setWindowSize(windowSize,receivedPacket);*/
                //receivedPacket = setWindowSize(receivedPacket,windowSize);
                
                //Recompute Checksum
                receivedPacket = recomputeChecksum(receivedPacket);
                
                //Send to client
                sendPacket(receivedPacket, m.getClientAddress());
            }
            
            //Read next packet from buffer, and repeat loop
            receivedPacket = readPacket(true);
        }
        
        //Exited loop, so m.getRestarting must be true, so enter restarting state
        SSWcurrentState = States.restarting;
    }
    
    /**
     * South Side Wraps restarting protocol
     */
    private void SSWrestarting(){
        gui.printToScreen("SSW: Now in restarting mode.");
        
        //Closed window packets have window size of zero
        short closedWindow = 0;
        
        /***********************************************************************
         *** In the algorithm it has North Side Wrap telling South Side Wrap ***
         *** to do this, but it seems redundant, so in our implementation the***
         *** south side wrap just does it when it knows server has restarted.***
         *** If however the papers method is to be used, uncomment code below***
         *********************************************************************/
        /*byte[] receivedPacket = readPacket(false);
         if(sender.equals("NSW") && receivedPacket != null){*/

        //Fabricate SYN Packet that has the initial sequence# of stable_seq, 
        // and send this to servers TCP layer.
        byte[] SYNPacket = TCP.createTCPSegment();
        TCP.setSYNFlag(true, SYNPacket);
        TCP.setSequenceNumber(m.getStable_seq(),SYNPacket);
        gui.printToScreen("SSW: Sending fake SYN to server.");
        sendPacket(SYNPacket,m.getServerAddress());
        
        byte[] receivedPacket2 = null;
        while(!sender.equals("SRV")){
            //Capture SRV's responding ACK (and discard)
            receivedPacket2 = readPacket(false);
        }
        
        //Reply with fake corresponding ACK
        m.setDelta_seq(m.getDelta_seq() -TCP.getSequenceNumber(receivedPacket2));
        byte[] ACKPacket = TCP.createTCPSegment();
        TCP.setACKFlag(true,ACKPacket);
        gui.printToScreen("SSW: Sending fake ACK to server.");
        sendPacket(ACKPacket,m.getServerAddress());
        /*}*/
        
        //Loop sending closed windows while system is in restarting mode (this 
        //exits when server is fully recovered)
        while(m.getRestarting()){
            
            //Send Closed window packets to client to keep connection alive
            byte[] closedWindowPacket = TCP.createTCPSegment();
            TCP.setWindowSize(closedWindow, closedWindowPacket);
            gui.printToScreen("SSW: Sending closed window to client.");
            //REMOVE LINE BELOW AND UNCOMMENT FOLLOWING CODE IF REAL TCP USED
            sendClosedPacket(closedWindowPacket, m.getClientAddress());
            /*sendPacket(closedWindowPacket, m.getClientAddress());*/
            

            try{
                //Sleep for 1 second and send window again
                this.sleep(1000);
            }
            catch(java.lang.InterruptedException e){
                System.out.println("Thread error: " + e);
            }
            
        }
        //System no longer in restarting mode, so switch back to normal operation
        SSWcurrentState = States.normal;
    }
    
       

    /**
     * Recomputes a changed Packets checksum
     * @param received Packet to be recomputed
     * @return Object New packet with correct ChecksumgetAcknowledgementNumber
     */
    private byte[] recomputeChecksum(byte[] received){
       //Not needed for our TCP, so just return packet
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
     * Periodically check to see if data to be read, if so, read it, and return.
     * Else sleep a little (To make sure process doesnt hog cpu), and try again
     * @param boolean If true then check that server is not in restarting state, before looping again
     * @return Object Packet read
     */
    private byte[] readPacket(boolean useRestarting){
        try{
            //If true passed to method then check if server is restating, and if it is
            //then exit this method.
            while((useRestarting && !m.getRestarting()) || !useRestarting){
                
                //Get appropriate file filter, this allows you to only see packets destined for you
                FilenameFilter filter = new SSWFileFilter();
                //Check in Server buffer folder
                File f = new File("serverBuffer");
                //Create an array of all files in this buffer that go through file filter
                String[] files = f.list(filter);
                
                //If there are files there..
                if(files != null && files.length != 0){
                    //Read first file into byte array
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    fileinputstream.close();
                    
                    //Delete file, to avoid reading again
                    boolean hadDel = (new File("serverBuffer/"+files[0]).delete());
                    
                    //Find and set sender/destination
                    int length  = files[0].length();
                    String[] info = files[0].split("[.]");
                    if(info.length == 3 || info.length == 4){
                        sender = info[1];
                        destination = info[2];
                    }
                    
                    //Return the bytearray containing the files data
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 1 seconds, then look again for file
                        this.sleep(1000);
                    }
                    catch(java.lang.InterruptedException e){
                        System.out.println("Thread error: " + e);
                    }
                }
            }
            return null;
        }
        catch(java.io.FileNotFoundException e){
            System.out.println("Thread error: " + e);
            return null;
        } 
        catch(java.io.IOException e){
            System.out.println("Thread error: " + e);
            return null;
        } 
    }
    
    /**
     * Send packet to address
     * @param object Packet to be sent
     * @param address Place to send it to
     */
    private void sendPacket(byte[] data, short address){
        //Check Address and send to appropriate place
        if(address == m.getServerAddress()){
            //Animate GUI to show message passing
            gui.ssw2tcp();
            //Write file in appropriate place
            writeFile(data,"serverBuffer/received.CLT.SRV.TCP");
        }
        else if(address == m.getClientAddress()){
            //Animate GUI to show message passing
            gui.srv2clt();
            //Write file in appropriate place
            writeFile(data,"clientBuffer/received.SRV.CLT.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Animate GUI to show message passing
            gui.srv2log();
            //Write file in appropriate place
            writeFile(data,"loggerBuffer/received.SSW.LOG.TCP");
        }
        
    }
    
    /*
     *Method animates GUI but doesnt send any data, for the purpose of demoing
     */
  private void sendClosedPacket(byte[] data, short address){
        if(address == m.getClientAddress()){
            //Animate GUI
            gui.srv2clt();
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
