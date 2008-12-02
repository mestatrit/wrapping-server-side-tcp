/*
 * Heartbeat.java
 *
 * Created on November 13, 2008, 3:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;




/**
 *
 * @author csugbe
 */

import java.io.*;
import javax.swing.*;
import java.awt.event.*;

public class Heartbeat extends Thread {
    private Main m;
    private String sender;
    int emptyReadCounter = 0;   
    private boolean serverAlive = true;
    logger thisLogger;
    private boolean currentBeat = true;
    private boolean interactingWithClient = false;
    
    private byte heartbeatFlag = 1;
    
    /** Creates a new instance of Heartbeat */
    public Heartbeat(Main main, logger newLogger) {
        m = main;
        thisLogger = newLogger;
    }
    
    public void run(){
        checkHeartBeat();
    }
    
    
    public void checkHeartBeat(){
       int length=TCP.DATA_SIZE;
       byte[][] readlength= new byte[length][length]; //to have the array received from the client
       byte[] temp = new byte[length];
       int count=0;
       boolean finished=false;
       
       
       System.out.println("HEARTBEAT IS RUNNING");
         do{
           
            try{
                this.sleep(20000);
                
            }catch(Exception e){}
            
            if(this.currentBeat == true){
                // A heartbeat must have arrived from the logger (i.e. from the server).
                System.out.println("HEARTBEAT: Woke up. Current beat is TRUE. Server is alive.");
                thisLogger.setServerAlive(true);
                this.currentBeat = false;
            }
            else{
                // No beat arrived during sleeping.
                System.out.println("HEARTBEAT: No beat arrived, setting serverAlive to false.");
                thisLogger.setServerAlive(false);
                if(interactingWithClient == false){
                    thisLogger.clientInteraction();
                    interactingWithClient = true;
                }
            }
            
            
            
            /*if(serverAlive==true){
               
               thisLogger.setServerAlive(true);
               setServerAlive(true);
               thisLogger.ServerUp();
               
               
            }else if(serverAlive==false){
                
               thisLogger.setServerAlive(false); 
               thisLogger.clientInteraction();
               
            }*/

           
        }while(finished == false);  
    }
    
    
    public boolean getServerAlive(){
        return serverAlive;
    }
    
    
    public void setServerAlive(boolean temp){
        serverAlive = temp;
    }
       
    public void setInteractingWithClient(){
        this.interactingWithClient = false;
    }
    
    private byte[] readHeartbeat(){
        try{
            while(true){
                FilenameFilter filter = new LOGFileFilter();
                File f = new File("loggerBuffer/receivedHearbeat");
                String[] files = f.list(filter);
                emptyReadCounter = 0;   // Reset the consecutive read counter
                FileInputStream fileinputstream = new FileInputStream("loggerBuffer/receivedHeartbeat"+files[0]);
                int numberBytes = fileinputstream.available();
                byte[] bytearray = new byte[numberBytes];
                fileinputstream.read(bytearray);
                boolean hadDel = (new File(files[0]).delete());
                //Find and set sender
                int length  = files[0].length();
                sender = files[0].substring(length-7,length-4);
                return bytearray;
            }          
        }
        catch(java.io.FileNotFoundException e){
            return null;
        } 
        catch(java.io.IOException e){
            return null;
        } 
    }
    
    public void beat(){
        System.out.println("HEARTBEAT: Logger received heatbeat. Set currentBeat to TRUE.");
        this.currentBeat = true;
        thisLogger.setServerAlive(true);
    }
}
