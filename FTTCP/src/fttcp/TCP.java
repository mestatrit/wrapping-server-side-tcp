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

public class TCP extends Thread{
    private Main m;
    private String entity;
    private String direction;
    
    /**
     * Constructor
     */
    public TCP(Main main, String e){
        m = main;
        entity =e;
        
    }
    
    /**
     * TCP thread
     */
    @Override
    public void run(){
        // when buffer is reabable i.e. sendToTCP then read and act on data
        // use entity and direction to indicate what action to take
    }
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new TCPFileFilter();
                File f = new File(entity);
                String[] files = f.list(filter);
                if(files.length != 0){
                    // find out direction the TCP Layer is intercepting buffer data
                    // insteam - received (receive header)
                    // outstream - sendTo (add header)
                    if (files[0].startsWith("received")) 
                        direction = "received";
                    else
                        direction = "sendTo";
                    
                    FileInputStream fileinputstream = new FileInputStream(entity+"/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
                        this.sleep(3000);
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
            writeFile(data,"serverBuffer/received.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"clientBuffer/received.TCP");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in logger folder
            writeFile(data,"loggerBuffer/received.TCP");
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
     private byte[] intToByteArr(int num){
        byte[] byteArr = new byte[4];
        byteArr[3] =(byte)( num >> 24 );
        byteArr[2] =(byte)( (num << 8) >> 24 );
        byteArr[1] =(byte)( (num << 16) >> 24 );
        byteArr[0] =(byte)( (num << 24) >> 24 );
        return byteArr;
    }
}
