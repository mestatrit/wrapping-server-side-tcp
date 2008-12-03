/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;
import org.knopflerfish.util.ByteArray;

/**
 *
 * @author James Bossingham
 */
import java.io.*;

public class client extends Thread{
    private Main m;
    private GUI gui;
    

    
    public client(Main main, GUI g){
        m = main;
        gui = g;
    }

    /**
     * Client thread:
     *
     * Client sends numbers 1 to 100
     * to server, waiting for server to
     * reply with its response before sending
     * the next message.
     *
     **/
    @Override
    public void run(){
        gui.printToScreen("Client reporting in.");
        
        //set initial number to send
        short numberSend = 1;
        
        //client to send numbers up to 100
        while(numberSend <= 100) {
            //convert integer to send to byte array
            byte[] data = new byte[TCP.DATA_SIZE];
            
            //put into byte array to send
            ByteArray.setShort(numberSend,data,0);
            
            gui.printToClient("Sending " + numberSend);
            //send number to server
            sendPacket(data, m.getServerAddress());
            gui.printToScreen("CLT: Sending " + numberSend);
            
            //while server has not replied, wait
            gui.printToScreen("CLT: Waiting for server to reply");  
            byte[] receivedPacket;
            receivedPacket = readPacket();
           
            //convert received letter to int
            int receivedChar = ByteArray.getShort(receivedPacket, 0);
            gui.printToClient("Received " + (char) receivedChar);
            gui.printToScreen("CLT: Received " + (char) receivedChar);
            //as server has replied, increase number to send next message
            numberSend++;

        }
            
    }
        
    
     /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new CLTFileFilter();
                File f = new File("clientBuffer");
                String[] files = f.list(filter);
                if(files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("clientBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    fileinputstream.close();
                    boolean hadDel = (new File("clientBuffer/"+files[0]).delete());
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
                        this.sleep(1000);
                    }
                    catch(java.lang.InterruptedException e){
                        System.out.println("Thread error: " + e);
                    }
                }
            }
        }
        catch(java.io.FileNotFoundException e){
            System.out.println("Thread error: " + e);
            return null;
        } 
        catch(java.io.IOException e){
            System.out.println("Thread error: " + e);
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
            //Put in file called toSend.Server in client folder
            gui.clt2tcp();
            writeFile(data,"clientBuffer/toSend.CLT.SRV.TCP");
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
            System.out.println("Client Cannot write file to: " + path);
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
