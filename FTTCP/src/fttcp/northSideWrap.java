/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author James Bossingham
 *          
 */
import java.io.*;

public class northSideWrap extends Thread{
    //define global class variables
    private Main m;
    private GUI gui;
    private String sender;
    private String destination;
    private String extraInfo;
    private enum States {normal, restarting};
    //set initial state to normal
    private States NSWcurrentState = States.normal;
    private byte readFlag = 2;
    private int bytesWritten = 0;
    private int messagesRead = 0;
    private boolean lastRound = false;
    
    
    /**
     * Constructor
     */
    public northSideWrap(Main main,GUI g){
        m = main;
        gui = g;
    }
    
    /**
     * North Side Wrap thread
     *
     * NSW captures data from the server before it
     * gets to the TCP layer, and from the client
     * before it gets to the server.
     *
     **/
    
    @Override
    public void run(){
       gui.printToScreen("NSW reporting in.");
        //while thread is running
        while(true) {
                      
            //if server in normal operation, use normal method
            if (NSWcurrentState==States.normal)
                NSWnormalOperation();
            //if server recovering, use recovering method
            else if (NSWcurrentState==States.restarting)
                NSWrestartingOperation();
        }        
        
    }
    
    public void NSWnormalOperation() {
        //variables for normal operation
        byte[] readLengthArray = new byte[TCP.PACKET_SIZE];
        byte[] tempReadLengthArray;
        int readLength;
        int tempUnstableReads;
        
        //while the server is not in Restart mode
        while(!m.getRestarting()) {
            gui.printToScreen("NSW: Waiting for another packet");
            //read a packet from the buffer
            byte[] NSWreadData = readPacket(true);             
  
             //if it's a read socket call, ie. comes from the client
             if(NSWreadData!=null && sender.equals("CLT")) {
                
                /* 
                 When data comes from the client,
                 * NSW determines the read length of it
                 * and passes this length on to the logger.
                 * It then increments a variable of unstable
                 * reads - the number of read lengths it has
                 * sent to the logger but not received
                 * confirmation of the logger having stored correctly.
                 *
                 */
               
                 //determine read length
                 readLength = NSWreadData.length;
           
                  //convert readLength to Byte Array
                 tempReadLengthArray = intToByteArr(readLength);
            
                 //set Array[0] to readFlag so the logger knows its a read length when received
                 readLengthArray[0]=readFlag;
            
                    //add readLength byte array to array with readFlag
                 for (int i = 1; i < TCP.PACKET_SIZE; i++){
                        readLengthArray[i]=tempReadLengthArray[i-1];
                 }
            
                 //send readLength to logger
                 gui.printToScreen("NSW: Sending read length to logger");
                 sendPacket(readLengthArray, m.getLoggerAddress());
                 //send packet to server
                 gui.printToScreen("NSW: Sending packet to server");
                 sendPacket(NSWreadData, m.getServerAddress());
                 //increment the number of messages which have been read
                 messagesRead++;
                 
            
                 //increment unstable reads by 1
                 gui.printToScreen("NSW: Incrementing unstable reads now");
                 tempUnstableReads = m.getUnstable_reads();
                 m.setUnstable_reads(tempUnstableReads + 1);
       
               }
        
                //if sender is logger
               else if (NSWreadData!=null && sender.equals("LOG")) {
                
                /*
                 * When NSW receives a message from the logger,      
                 * it decrements the number of unstable reads.    
                 * This acknowledgement of storing a read length 
                 * is the only type of message NSW receives from     
                 * the logger                                     
                 * 
                 *
                 */                                                         
            
                   //decrement unstable reads by 1
                   gui.printToScreen("NSW: Got ack, decrementing unstable reads");
                   tempUnstableReads = m.getUnstable_reads();
                   m.setUnstable_reads(tempUnstableReads - 1);
            
               }
        
                //if write socket call
                else if (NSWreadData!=null && sender.equals("SRV")) {      
                
                /* 
                 * When NSW receives a message from the server,
                 * it waits until it has received acknowledgements 
                 * from the logger for all read lengths it has sent 
                 * it, to ensure recovery occurs correctly.
                 *
                 */
            
                   //if unstable reads exist, don't process, wait for ACKs
                   while (m.getUnstable_reads() > 0) {
                       gui.printToScreen("NSW: Waiting for unstable reads to be 0");
                       //wait for ack to be received
                       byte[] ackData = readPacket(true);
                       //when received, decrease unstable reads
                       tempUnstableReads = m.getUnstable_reads();
                       m.setUnstable_reads(tempUnstableReads - 1);
                       gui.printToScreen("NSW: Received ACK, unstable reads now 0");
              }
            
            //if no unstable reads exist, send packet on to client

                    if(NSWreadData != null) {
                        sendPacket(NSWreadData, m.getClientAddress());            
                        gui.printToScreen("NSW: Sending packet to client");
                    }

                
            
                 }
                 
           }
        // NSW exited While loop so now in restart mode
        NSWcurrentState = States.restarting;
  }

       
    //operation when server is restarting
    public void NSWrestartingOperation() {
        
        gui.printToScreen("NSW entering RESTART mode");

        int bytesWritten = 0;
        int tempMessagesRead = messagesRead;
        
        /* Specification tells NSW to get SSW to fabricate a SYN,
         * but SSW fabricates SYN without needing NSW to tell it to.
         * Code below would perform this function.
         
         //create message telling SSW to fabricate SYN
        byte[] fabSyn = new byte[TCP.PACKET_SIZE];
        //send message to SSW
        writeFile(fabSyn,"serverBuffer/received.NSW.SSW");
         
         */
        
        //while server is recovering
        while(m.getRestarting()) {
            
            //read data packet
            byte[] NSWreadData = readPacket(false);
            
             //if it's a read socket call
            if(sender.equals("LOG")) {
                    
                /* 
                 in the specification, NSW knows when
                 * it has caught back up by comparing read
                 * lengths. As our TCP uses fixed lengths
                 * of messages, we update an int variable
                 * keeping track of the number of messages
                 * rather than their lengths, which are fixed
                 *
                 */

                    //NSW passes on data read from logger to server
                    sendPacket(NSWreadData, m.getServerAddress());
                    gui.printToScreen("NSW: SENDING OLD DATA TO SERVER");
                    //decrease the number of messages needed to catchup
                    tempMessagesRead--;
                    //if there's 1 more message to catch up, set lastRound to true
                    if(tempMessagesRead==1) {
                        lastRound = true;
                    }
                    
             }

              //if it's a write socket call
            else if (sender.equals("SRV")) {
                
                /* 
                 * In recovery mode, NSW discards
                 * data from the server as the client
                 * has already received it before the server
                 * crashes, so should not get it again
                 *
                 */
                
                gui.printToScreen("NSW: DISCARDING OLD DATA");
                // NSW keeps track of bytes written.
                bytesWritten = bytesWritten + NSWreadData.length;
                
                /* Data from server in Restarting mode
                 * is discarded as client has already
                 * received it, so do nothing
                 */
               

             }
            
            // when it's the last messages to catch up with
            while(lastRound){
                NSWreadData = readPacket(false);
                
                 if(sender.equals("LOG")) {
                    //pass the last logged message to the sever
                    sendPacket(NSWreadData, m.getServerAddress());
                    gui.printToScreen("NSW sending last packet to SRV");
                    //no longer in Restarting mode
                    m.setRestarting(false);
                    gui.printToScreen("NSW: RESTARTING NOW FALSE");
                    //update lastRound to allow for future crashes.
                    lastRound = false;
                 }

            }
          
        }
        
        // out of Restarting loop, go back to normal operation
        NSWcurrentState = States.normal;
    }

   /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(boolean useRestarting){
        try{
            while((useRestarting && !m.getRestarting()) || !useRestarting){
                FilenameFilter filter = new NSWFileFilter();
                File f = new File("serverBuffer");
                String[] files = f.list(filter);
                java.util.Arrays.sort(files);
                if(files != null && files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    //Delete file as its now been read
                    fileinputstream.close();
                    boolean hadDel = (new File("serverBuffer/"+files[0]).delete());;
                    //Find and set sender
                    String[] info = files[0].split("[.]");
                    
                        sender = info[1];
                        destination = info[2];
                    if(info.length == 5){
                            extraInfo = info[4];
                    }
                    else{
                            extraInfo = null;
                    }
                    
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
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
        if(address == m.getServerAddress()){
            //Put in file called received.TCP in server folder
            gui.nsw2srv();
            if(extraInfo != null){
                String path = "serverBuffer/received.NSW.SRV." + extraInfo;
                writeFile(data, path);
            }
            else{
                writeFile(data,"serverBuffer/received.NSW.SRV");
            }
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            gui.nsw2tcp();
            writeFile(data,"serverBuffer/toSend.SRV.CLT.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
            gui.nsw2tcp();
            writeFile(data,"serverBuffer/toSend.NSW.LOG.TCP");
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
            System.out.println("NSW Cannot write file to: " + path);
        }
    }
     private byte[] intToByteArr(int num){
        byte[] byteArr = new byte[TCP.PACKET_SIZE];
        byteArr[3] =(byte)( num >> 24 );
        byteArr[2] =(byte)( (num << 8) >> 24 );
        byteArr[1] =(byte)( (num << 16) >> 24 );
        byteArr[0] =(byte)( (num << 24) >> 24 );
        return byteArr;
    }
}
