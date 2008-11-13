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
    private boolean serverAlive = false;
    
    private byte heartbeatFlag = 1;
    
    /** Creates a new instance of Heartbeat */
    public Heartbeat(Main main) {
    m = main;
    }
    
    
    public void run(){
        checkHeartBeat();
    }
    
    
    public void checkHeartBeat(){
       int length=100;
       byte[][] readlength= new byte[length][length]; //to have the array received from the client
       byte[] temp = new byte[length];
       int count=0;
       boolean finished=false;
       

           do{
               try{
                   temp = readHeartbeat(); 
                   if (temp[0] == heartbeatFlag){
                       setServerAlive(true);
                   }
                   else{
                        setServerAlive(false);
                   }
                   this.sleep(2000);
               }
               catch(Exception e){
                    setServerAlive(false);
                    try{
                        this.sleep(2000);
                    }
                    catch(Exception f){}
               }

           
        }while(finished = false);  
    }
    
    
    public boolean getServerAlive(){
        return serverAlive;
    }
    
    
    public void setServerAlive(boolean temp){
        serverAlive = temp;
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
}
