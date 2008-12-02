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

public class northSideWrap extends Thread{
    private Main m;
    private GUI gui;
    private String sender;
    private String destination;
    private enum States {normal, restarting};
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
     */
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

        byte[] readLengthArray = new byte[TCP.PACKET_SIZE];
        byte[] tempReadLengthArray;
        int readLength;
        int tempUnstableReads;
        

        while(!m.getRestarting()) {
            gui.printToScreen("NSW: Waiting for another packet");
            byte[] NSWreadData = readPacket(true);             
  
             //if read socket call
             if(sender.equals("CLT")) {
                 //determine read length
                 readLength = NSWreadData.length;
           
                  //convert readLength to Byte Array
                 tempReadLengthArray = intToByteArr(readLength);
            
                 //set Array[0] to readFlag
                 readLengthArray[0]=readFlag;
            
                    //add readLength byte array to array with readFlag
                 for (int i = 1; i < TCP.PACKET_SIZE; i++){
                        readLengthArray[i]=tempReadLengthArray[i-1];
                 }
            
                 //send readLength to logger, forward packet to server
                 gui.printToScreen("NSW: Sending read length to logger");
                 sendPacket(readLengthArray, m.getLoggerAddress());
                 gui.printToScreen("NSW: Sending packet to server");
                 sendPacket(NSWreadData, m.getServerAddress());
                 messagesRead++;
                 
            
                 //increment unstable reads by 1
                 gui.printToScreen("NSW: Incrementing unstable reads now");
                 tempUnstableReads = m.getUnstable_reads();
                  m.setUnstable_reads(tempUnstableReads + 1);
       
               }
        
                //if sender is logger
               else if (sender.equals("LOG")) {
            
                   //decrement unstable reads by 1
                   gui.printToScreen("NSW: Got ack, decrementing unstable reads");
                   tempUnstableReads = m.getUnstable_reads();
                   m.setUnstable_reads(tempUnstableReads - 1);
            
               }
        
                //if write socket call
                else if (sender.equals("SRV")) {          
            
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
            
            //if no unstable reads exist, send packet

            sendPacket(NSWreadData, m.getClientAddress());
            gui.printToScreen("NSW: Sending packet to client");
                
            
           }
        
     }
        // NSW in restarting mode
        NSWcurrentState = States.restarting;
  }

       
    //operation when server is restarting
    public void NSWrestartingOperation() {
        
        gui.printToScreen("NSW entering RESTART mode");

        int bytesWritten = 0;
        
        /*//create message telling SSW to fabricate SYN
        byte[] fabSyn = new byte[TCP.PACKET_SIZE];
        //send message to SSW
        sendPacket(fabSyn, m.getServerAddress());*/
        
        //while server is recovering
        while(m.getRestarting()) {
            
            byte[] NSWreadData = readPacket(false);
            
             //if it's a read socket call
            if(sender.equals("LOG")) {
                    // NSW replies data read from logger
                    sendPacket(NSWreadData, m.getServerAddress());
                    gui.printToScreen("NSW: SENDING OLD DATA TO SERVER");
                    messagesRead--;
                    if(messagesRead==0) {
                        lastRound = true;
                    }
                    
                  }

              //if it's a write socket call
            else if (sender.equals("SRV")) {
                gui.printToScreen("NSW: DISCARDING OLD DATA");
                // NSW keeps track of bytes written.
                bytesWritten = bytesWritten + NSWreadData.length;
                

                //if data has previous been written
                //if (TCP.getSequenceNumber(NSWreadData) < m.getServer_seq()) {

                   //discard the data (just return successful write)
             //   }

                //if it's new data
              //  else {
                    //all recovering data been replayed, resume normal operation
                    //May not need to send this - needs testing
                   // sendPacket(NSWreadData, m.getClientAddress());
                  //  m.setRestarting(false);

                }
            
            if(lastRound){
                readPacket(false);
                m.setRestarting(false);
                gui.printToScreen("NSW: RESTARTING NOW FALSE");
                lastRound = false;
            }
          
        }
        
        // go back to normal operation
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
            return null;
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
        if(address == m.getServerAddress()){
            //Put in file called received.TCP in server folder
            gui.nsw2srv();
            writeFile(data,"serverBuffer/received.NSW.SRV");
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
            System.out.println("SSW Cannot write file to: " + path);
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
