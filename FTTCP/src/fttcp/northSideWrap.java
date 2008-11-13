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
    private String sender;
    private enum States {normal, restarting};
    private States NSWcurrentState = States.normal;
    private byte readFlag = 5;
    private int bytesWritten = 0;
    
    /**
     * Constructor
     */
    public northSideWrap(Main main){
        m = main;
    }
    
    /**
     * North Side Wrap thread
     */
    @Override
    public void run(){
        
        //while thread is running
        while(true) {
            
            //set operation mode
            if (m.getRestarting()==true)
                NSWcurrentState=States.restarting;
            else NSWcurrentState=States.normal;
            
            //if server in normal operation, use normal method
            if (NSWcurrentState==States.normal)
                NSWnormalOperation();
            //if server recovering, use recovering method
            else if (NSWcurrentState==States.restarting)
                NSWrestartingOperation();
        }        
        
    }
    
    public void NSWnormalOperation() {
        byte[] NSWreadData = readPacket();
        int readLength;
        byte[] readLengthArray = new byte[5];
        byte[] tempReadLengthArray;
        
        //if read socket call
        if(sender=="CLT") {
            //determine read length
            readLength = NSWreadData.length;
           
            //convert readLength to Byte Array
            tempReadLengthArray = intToByteArr(readLength);
            
            //set Array[0] to readFlag
            readLengthArray[0]=readFlag;
            
            //add readLength byte array to array with readFlag
            for (int i = 1; i < 5; i++){
                readLengthArray[i]=tempReadLengthArray[i-1];
            }
            
            //send readLength to logger
            sendPacket(readLengthArray, m.getLoggerAddress());
            
            //increment unstable reads by 1
            m.setUnstable_reads(m.getUnstable_reads() + 1);
       
        }
        
        //if sender is logger
        else if (sender=="LOG") {
            
            //decrement unstable reads by 1
            m.setUnstable_reads(m.getUnstable_reads() - 1);
            
        }
        
        //if write socket call
        else if (sender=="SVR") {          
            
            //if unstable reads exist, don't process, try again in 3 seconds
            while (m.getUnstable_reads() > 0) {
                
                try{
                        this.sleep(3000);
                    }
                    catch(java.lang.InterruptedException e){
                        System.out.println("North Side Wrap thread interrupted");
                    }
            }
            
            //if no unstable reads exist, send packet
            sendPacket(NSWreadData, m.getClientAddress());
                
            
        }
        
    }
       
    //operation when server is restarting
    public void NSWrestartingOperation() {
        
        //if it's a read socket call
        if(sender=="CLT") {
            // NSW replies data read from logger
        }
        
        else if (sender == "SVR") {
            // NSW keeps track of bytes written.
        }
    }

   /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new NSWFileFilter();
                File f = new File("serverBuffer");
                String[] files = f.list(filter);
                if(files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    //Delete file as its now been read
                    boolean hadDel = (new File(files[0]).delete());
                    //Find and set sender
                    int length  = files[0].length();
                    sender = files[0].substring(length-7,length-4);
                    
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
        if(address == m.getServerAddress()){
            //Put in file called received.TCP in server folder
            writeFile(data,"serverBuffer/received.NSW.SRV");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"serverBuffer/toSend.NSW.CLT.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
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
        byte[] byteArr = new byte[4];
        byteArr[3] =(byte)( num >> 24 );
        byteArr[2] =(byte)( (num << 8) >> 24 );
        byteArr[1] =(byte)( (num << 16) >> 24 );
        byteArr[0] =(byte)( (num << 24) >> 24 );
        return byteArr;
    }
}