/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author James Bossingham
 */

public class Main {

    //GLOBAL VARIABLES
    private int delta_seq;
    private int stable_seq;
    private int server_seq;
    private int unstable_reads;
    private boolean restarting;
    private short clientAddress = 0;
    private short serverAddress = 1;
    private short loggerAddress = 2;
    private short NSWAddress = 3;
    private short SSWAddress = 4;
    //private Main main;
    private server serv;
    private GUI g;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
         byte[] test = new byte[5];
        for(byte i=0;i<test.length;i++){
            test[i] = i;
        }
        //Start threads
        Main main = new Main();
        main.setGui(new GUI(main));
        main.getGui().start();
        Thread log = new logger(main, main.getGui());
        log.start();
        Thread ssw = new southSideWrap(main, main.getGui());
        ssw.start();
        Thread nsw = new northSideWrap(main, main.getGui());
        nsw.start();
        Thread tcpSrv = new ServerTCP(main, "SRV", main.getGui());
       tcpSrv.start();
       Thread tcpClt = new ClientTCP(main, "CLT", main.getGui());
       tcpClt.start();
       Thread tcpLog = new LoggerTCP(main, "LOG", main.getGui());
       tcpLog.start();
        main.setServer(new server(main, main.getGui()));
        Thread client = new client(main, main.getGui());
        client.start();
    }
    
    public void main(){
               
    }
    
    public void KillServer()
    {
        serv.killServerHeartbeat();
        serv.stop();
    }
    public void RestartServer()
    {
       serv = new server(this, getGui());
       serv.start();
    }
    /**
     * Gets restarting
     * @return boolean restarting
     */
    public boolean getRestarting(){
        return restarting;
    }
    
    public void setServer(server t){
        serv = t;
        serv.start();
    }
    
    public server getServer(){
        return serv;
    }
    
    public void setGui(GUI gui){
        g = gui;
    }
    public GUI getGui(){
       return g;
    }
    /**
     * Gets restarting
     * @return boolean restarting
     */
    public void setRestarting(boolean b){
        restarting = b;
    }
    
    /**
     * Gets delta_seq
     * @return int delta_seq
     */
    public int getDelta_seq(){
        return delta_seq;
    }
    /**
     * Sets delta_seq
     * @param s Int new delta_seq value
     */
    public void setDelta_seq(int s){
        delta_seq = s;
    }
    
    /**
     * Gets stable_seq
     * @return int stable_seq
     */
    public int getStable_seq(){
        return stable_seq;
    }
    /**
     * Sets stable_seq
     * @param s Int new stable_seq value
     */
    public void setStable_seq(int s){
        stable_seq = s;
    }
    /**
     * Gets server_seq
     * @return int server_seq
     */
    public int getServer_seq(){
        return server_seq;
    }
    /**
     * Sets server_seq
     * @param s Int new server_seq value
     */
    public void setServer_seq(int s){
        server_seq = s;
    }
     /**
     * Gets unstable_reads
     * @return int unstable_reads
     */
    public int getUnstable_reads(){
        return unstable_reads;
    }
    /**
     * Sets unstable_reads
     * @param s Int new unstable_reads value
     */
    public void setUnstable_reads(int s){
        unstable_reads = s;
    }
    /**
     * Gets Client Address
     * @return short Client Address
     */
    public short getClientAddress(){
        return clientAddress;
    }
    /**
     * Sets client address
     * @param a new client address
     */
    public void setClientAddress(short a){
        clientAddress = a;
    }
    
    /**
     * Gets server Address
     * @return short server Address
     */
    public short getServerAddress(){
        return serverAddress;
    }
    /**
     * Sets server address
     * @param a new server address
     */
    public void setServerAddress(short a){
        serverAddress = a;
    }
    
     /**
     * Gets logger Adress
     * @return short logger Address
     */
    public short getLoggerAddress(){
        return loggerAddress;
    }
    /**
     * Sets logger address
     * @param a new logger address
     */
    public void setLoggerAddress(short a){
        loggerAddress = a;
    }
     /**
     * Gets logger Adress
     * @return short logger Address
     */
    public short getNSWAddress(){
        return NSWAddress;
    }
    /**
     * Sets logger address
     * @param a new logger address
     */
    public void setNSWAddress(short a){
        NSWAddress = a;
    }
   
     /**
     * Gets logger Adress
     * @return short logger Address
     */
    public short getSSWAddress(){
        return SSWAddress;
    }
    /**
     * Sets logger address
     * @param a new logger address
     */
    public void setSSWAddress(short a){
        SSWAddress = a;
    }
    
}
