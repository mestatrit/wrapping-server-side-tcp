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
import javax.swing.*;
import java.awt.event.*;

public class logger extends Thread{
    private Main m;
    private String sender;
    private GUI gui;
    int emptyReadCounter = 0;   // For counting how many consecutive times a read has been performed when nothing has been available to read.
    int initialClientSequenceNumber = 0;
    
    private byte heartbeatFlag = 1;
    private byte readLengthFlag = 2;
    private byte initClientSeqNumber = 3;
    private byte fwdClientPktFlag = 4;
   
    
    private enum entity {SSW,NSW};
    Thread WrappersThread= new Thread();
    Thread bufferChecker= new Thread();
    /**
     * Constructor
     */
    public logger(Main main, GUI g){
        m = main;
        gui = g;
    }
    
    /**
     * Logger thread
     */
    @Override
    public void run(){
        gui.printToScreen("Logger reporting in.");
       int length=100;// Need to update length!!!
       int newReadLength = 0;
       int[] readLengthArray = new int[length]; //to have the array received from the client
       byte[] temp = new byte[length];
       byte[][] ClientData = new byte[TCP.DATA_SIZE][length];

       int clientDataCounter = 0;
       int readLengthCounter=0;
       
       boolean finished=false;


       // Start-up
       
       // Create instance of Heartbeat
       Heartbeat heartbeatThread = new Heartbeat(m);
       heartbeatThread.run();
       
       
       
       
      do{
           //perform the code below until it is said to stop (program finishes)
           try{
               temp = readPacket();
           }
           catch(Exception e){}
           
           if(heartbeatThread.getServerAlive() == true){
               // BEHAVIOUR UNDER NORMAL OPERATION
              
               // TODO: Update sender before it is accessed.
               
               if(sender=="NSW"){
                    
                   // Only ever recieve read lengths from NSW, but check flag type anyway.
                   if(temp[0] == readLengthFlag){
                       // Convert to int everything except flag in order to get readlength.
                       newReadLength = TCP.convertByteArrayToInt(temp, 1);
                       readLengthArray[readLengthCounter] = newReadLength;  // Store read length.            
                       sendACK(entity.NSW);   // Supply an acknowledgement
                       readLengthCounter++;   // Increment counter to indicate number of stored readLengths.
                   }
                       
               }else if(sender == "SSW"){
                   
                    if(temp[0] == 3){   // If the incoming data is the initial sequence number (during startup)..
                        // Store initial client sequence number
                        initialClientSequenceNumber = TCP.convertByteArrayToInt(temp, 1);
                    }
                    else if(temp[0] == 4){  // If the incoming data is forwarded client data..
                        // It is data from the client which must be stored.
                        ClientData[0][clientDataCounter] = (byte)(initialClientSequenceNumber + clientDataCounter);
                        // Copy each position in temp, into the new array.
                        for(int i = 1; i < temp.length; i++){
                            ClientData[i][clientDataCounter] = temp[i];
                        }
                        sendACK(entity.SSW);
                        clientDataCounter++;
                    }
               }
        }
        else if(heartbeatThread.getServerAlive() == false){
               // Server has failed. 
               
               do{
                   // Check for client data from the NSW
                   try{
                        temp = readPacket();
                        // There must have been something in the buffer, or else try would have failed and gone to catch
                        
                   }
                   catch(Exception e){
                        // There was nothing in the buffer or something else went wrong.
                   }
                   

                   
               }while(heartbeatThread.getServerAlive() == false);
        }
           

       
      }while(!finished);
   
    }
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new LOGFileFilter();
                File f = new File("loggerBuffer");
                String[] files = f.list(filter);
                if(files.length != 0){
                    emptyReadCounter = 0;   // Reset the consecutive read counter
                    FileInputStream fileinputstream = new FileInputStream("loggerBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
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
                        emptyReadCounter++;     // Increment counter - another empty read has occured.
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
            writeFile(data,"loggerBuffer/toSend.LOG.SRV.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"loggerBuffer/toSend.LOG.CLT.TCP");
        }
        else if(address == m.getSSWAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"loggerBuffer/toSend.LOG.NSW.TCP");
        }
        else if(address == m.getNSWAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"loggerBuffer/toSend.LOG.SSW.TCP");
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
    
    public void sendACK(entity e){
        /* This may need parameters showing WHO the ACK is meant for (i.e. SSW or NSW) */
        byte[] empty = new byte[1];
        empty[0] = 0;
        if(e == entity.NSW){
            writeFile(empty, "loggerBuffer/sendAck.NSW.LOG.TCP");
        }
        else if(e == entity.SSW){
            writeFile(empty, "loggerBuffer/sendAck.SSW.LOG.TCP");
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
