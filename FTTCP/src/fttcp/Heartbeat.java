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
 * @author Will Isseyegh
 * @author Sam Corcoran
 */

import java.io.*;
import javax.swing.*;
import java.awt.event.*;

public class Heartbeat extends Thread {
    private Main m;     // A reference to the instance of Main under which this thread is running.
    logger thisLogger;  // A reference to the instance of logger that created this instance of Heartbeat
    private String sender;      // TODO: Redundant? ReadHeartbeat no longer needed?
    private boolean currentBeat = true;     // This is set to true by the logger every time it receives a beat. Heartbeat knows if currentbeat is true, the server must be alive.
    boolean detectBeats = true;     // This variable indicates whether heartbeat should be detecting heartbeats, or should temporarily not look for them.
    boolean finished = false;       // This variable controls a while loop that ensures that Heartbeat's behaviour is repeated until the program is finished.
    int timeoutPeriod = 25000;      // The number of milliseconds currently being used as the timeout period.
    
    /** 
     * Constructor
     * (Creates a new instance of Heartbeat)
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
       
       /******************************************************************************************
        *** Heartbeat is spawned by the logger. It contains a variable called currentBeat that ***
        *** is updated to TRUE each time the logger detects a heartbeat packet from the server.***
        *** Heartbeat thread sleeps for a given timeout period before waking and checking the  ***
        *** state of currentBeat. If, while the thread was sleeping, a heartbeat was recorded  ***
        *** then currentBeat will be TRUE. Heartbeat thread then sets currentBeat to false in  ***
        *** preparation for the next heartbeat, before sleeping again.                         ***
        *** If, when heartbeat thread wakes, currentBeat is FALSE then no heartbeat has arrived***
        *** during the timeout period. We can assume, therefore, that the server has failed and***
        *** so is unable to send heartbeats.                                                   ***
        *** For each value of currentBeat, update logger's 'operatingNormally' variable        ***
        *** accordingly.                                                                       ***
        ******************************************************************************************/
       
       // Repeat the following behaviour until the program is over (finished == true)
       do{
           
            try{
                // Sleep for timeout period.
                // If no heartbeat has arrived in this time, we must assume the server has died.
                this.sleep(timeoutPeriod);
                
            }catch(java.lang.InterruptedException e){System.out.println("Thread error: " + e);}
           
           // Only check for heartbeats if detectBeats indicates that this is desired.
           if(this.detectBeats == true){ 
                // If a heartbeat has recently arrived..
                if(this.currentBeat == true){
                    // A heartbeat must have arrived from the logger (i.e. from the server).
                    System.out.println("HEARTBEAT: Woke up. Current beat is TRUE. Server is alive.");
                    thisLogger.setOperatingNormally(true);
                    this.currentBeat = false;   // Set currentBeat to false in order to wait for the next heartbeat.
                }
                else{
                    // No beat arrived during sleeping.
                    System.out.println("HEARTBEAT: No beat arrived, setting operatingNormally to false.");
                    thisLogger.setOperatingNormally(false);
                  
                        this.detectBeats = false;   // Stop checking for heartbeats until told to by the logger. 
                }
           }
        }while(finished == false);    
    }

    /*called by the logger when the server restarts to indicate
    to the heartbeat class that heartbeat is back*/
    public void beat(){
        System.out.println("HEARTBEAT: Logger received heatbeat. Set currentBeat to TRUE.");
        this.currentBeat = true;
    }

    /*Method to set detectBeats variable from the logger class*/
    public void setDetectBeats(boolean newBoolean){
        this.detectBeats = newBoolean;
    }
}
