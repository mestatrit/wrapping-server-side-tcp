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
 * @author Will Isseyugh
 * @author Sam Corcoran
 */
public class ServerHeartbeat extends Thread {
    
    Main m;
    server s;

    
    /** Creates a new instance of ServerHeartbeat */
    public ServerHeartbeat(Main main, server newServer) {
        this.m = main;
        this.s = newServer;
    }
    
    /* 
     * This thread sends a heartbeat packet to the logger and sleeps for a short duration before sleeping again.
     * This will occur continuously while the server is operating normally, and will resume when server restarts
     * (which occurs by creating a new instance of server and therefore a new instance of serverHeartbeat).
     */
     public void run(){
        
         while(true){
             System.out.println("SERVERHEARTBEAT: Sending heartbeat and sleeping.");
              s.sendHeartbeat();
             
             try{
                 // Thread will now sleep for a short while before sending another heartbeat.
                this.sleep(500);
                
            }catch(java.lang.InterruptedException e){
                System.out.println("Thread error: " + e);
            }
         }
     }
}
