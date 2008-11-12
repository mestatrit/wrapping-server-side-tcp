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
    int emptyReadCounter = 0;   // For counting how many consecutive times a read has been performed when nothing has been available to read.
    private enum entity {SSW,NSW};
    Thread NorthSideThread= new Thread();
    Thread SouthSideThread=new Thread();
    /**
     * Constructor
     */
    public logger(Main main){
        m = main;
    }
    
    /**
     * Logger thread
     */
    @Override
    public void run(){
       int length=100;
       byte[][] readlength= new byte[length][length]; //to have the array received from the client
       byte[] temp = new byte[length];
       int count=0;
       boolean finished=false;
       boolean serverAlive = true;
     

       
       
      
      do{ 

           try{
               temp = readPacket();
           
           }  
           catch(Exception e){}
           
        //perform the code below until it is said to stop (program finishes)
           if(emptyReadCounter < 3){
               if(sender=="NSW"){

                       for(int i=0; i<temp.length; i++){
                           readlength[count][i] = temp[i];
                       }
                       sendACK(entity.NSW);   // Supply an acknowledgement
                       count++;
                       emptyReadCounter = 0;
               }else if(sender == "SSW"){

               }
           }
       
       /* Three empty reads occured in a row */
       /* Assume server is disconnected */
       /* Must attempt to re-establish connection */
       
       serverAlive = false;
       
       // loop:
       // read server
       // check if read is ACK
       // if yes, send NSW all of the client data (in conversation)
       
       do{

           temp = readPacket(); 
           
           if(temp[0] == 3){        // 3 is CURRENTLY  the code (/flag) for an ACK 
               // If temp is correct format for an ACK then:
               serverAlive = true;
           }
           else{
               try{
                    this.sleep(3000);
               } catch(java.lang.InterruptedException e){
                   
               }
           }

       }while(serverAlive = false);
       
       /* Then communicate with server to give it stored client data */
       
       
       
     }while(finished=false);
   
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
