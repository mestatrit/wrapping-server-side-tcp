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
    private Main m;     // A reference to the instance of Main under which this thread is running.
    logger thisLogger;  // A reference to the instance of logger that created this instance of Heartbeat
    private String sender;      // TODO: Redundant? ReadHeartbeat no longer needed?
    private boolean serverAlive = true;     // Indicates whether the Heartbeat currently believes the server to be alive or not.
    private boolean currentBeat = true;     // This is set to true by the logger every time it receives a beat. Heartbeat knows if currentbeat is true, the server must be alive.
    private boolean interactingWithClient = false;  // This variable indicates whether logger.interactWithClient() has been called, and so avoids it being called twice.
    boolean detectBeats = true;     // This variable indicates whether heartbeat should be detecting heartbeats, or should temporarily not look for them.
    boolean finished = false;       // This variable controls a while loop that ensures that Heartbeat's behaviour is repeated until the program is finished.
    
    // TODO: REDUNDANT? private byte heartbeatFlag = 1;
    
    /** 
     * Constructor
     * Creates a new instance of Heartbeat 
     */
    public Heartbeat(Main main, logger newLogger) {
        m = main;
        thisLogger = newLogger;
    }
    
    public void run(){
        // Initiate heartBeat behaviour.
        checkHeartBeat();
    }
    
    
    public void checkHeartBeat(){
       
       System.out.println("HEARTBEAT IS RUNNING");
       
       // Repeat the following behaviour until the program is over (finished == true)
       do{
           
            try{
                // Sleep for timeout period.
                // If no heartbeat has arrived in this time, we must assume the server has died.
                this.sleep(20000);
                
            }catch(Exception e){}
           
           // Only check for heartbeats if detectBeats indicates that this is desired.
           if(this.detectBeats == true){ 
                // If a heartbeat has recently arrived..
                if(this.currentBeat == true){
                    // A heartbeat must have arrived from the logger (i.e. from the server).
                    System.out.println("HEARTBEAT: Woke up. Current beat is TRUE. Server is alive.");
                    thisLogger.setOperatingNormally(true);
                   // thisLogger.setServerAlive(true);    // Indicate to the logger that the server is alive.
                    this.currentBeat = false;   // Set currentBeat to false in order to wait for the next heartbeat.
                }
                else{
                    // No beat arrived during sleeping.
                    System.out.println("HEARTBEAT: No beat arrived, setting serverAlive to false.");
                    thisLogger.setOperatingNormally(false);
                    //thisLogger.setServerAlive(false);   // Indicate to the logger that the server has died and normal operation should cease.
                    //if(interactingWithClient == false){     // Only interact with the client if clientInteraction isn't already running.
                     //   thisLogger.clientInteraction();     // Call a method on the logger to handle operation while the server is dead as well as handle restarting.
                        this.detectBeats = false;   // Stop checking for heartbeats until told to by the logger.
                    //    this.interactingWithClient = true;       // Indicate that clientInteraction has been called.
                    //}
                }
           }
        }while(finished == false);    
    }
    
    
    public boolean getServerAlive(){
        return serverAlive;
    }
    
    
    public void setServerAlive(boolean temp){
        serverAlive = temp;
    }
       
   
    //TODO: Redundant?
    /*private byte[] readHeartbeat(){
        try{
            while(true){
                FilenameFilter filter = new LOGFileFilter();
                File f = new File("loggerBuffer/receivedHearbeat");
                String[] files = f.list(filter);
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
    }*/
    
    public void beat(){
        System.out.println("HEARTBEAT: Logger received heatbeat. Set currentBeat to TRUE.");
        this.currentBeat = true;
    }
    
    public void setInteractingWithClient(boolean newBoolean){
        this.interactingWithClient = newBoolean;
    }
    
    public void setDetectBeats(boolean newBoolean){
        this.detectBeats = newBoolean;
    }
}
