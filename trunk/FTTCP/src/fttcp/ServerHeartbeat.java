/*
 * ServerHeartbeat.java
 *
 * Created on December 1, 2008, 5:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author csugbe
 */
public class ServerHeartbeat extends Thread {
    
    Main m;
    server s;

    
    /** Creates a new instance of ServerHeartbeat */
    public ServerHeartbeat(Main main, server newServer) {
        this.m = main;
        this.s = newServer;
    }
    
    /*A new thread sending the heartbeat from the server
     This is stopped when the server goes down by killing the thread
     
     */
     public void run(){
        
         while(true){
             System.out.println("SERVERHEARTBEAT: will sent heartbaet and sleep");
              s.sendHeartbeat();
             
             try{
                 //amount of time that thread sleeps before sending next heartbeat.
                this.sleep(500);
                
            }catch(Exception e){}
             
         }
         
     }
     

}
