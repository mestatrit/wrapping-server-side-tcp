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

public class client extends Thread{
    private Main m;
    
    public client(Main main){
        m = main;
    }

    /**
     * Client thread
     */
    @Override
    public void run(){
        boolean Serverreplied =true;
        do
            for (int i = 0; i<20; i++)
            {
                sendPacket(intToByteArr(i),m.getServerAddress());
                System.out.println("client sending" + i);
            }
        while (Serverreplied = true);
     
    }
        /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        //IMPLEMENT
        try{
            FileInputStream fileinputstream = new FileInputStream("");
            int numberBytes = fileinputstream.available();
            byte[] bytearray = new byte[numberBytes];
            fileinputstream.read(bytearray);
            return bytearray;
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
            //Put in file called toSend.Server in client folder
            writeFile(data,"clientBuffer/toSend.CLT.SRV");
        }
        
    }
    
    private void writeFile(byte[] data, String path){
        try{
            FileOutputStream outStream = new FileOutputStream(path);
            PrintWriter printW = new PrintWriter(outStream);
            printW.print(data);
            printW.flush();
            outStream.close();
        }
        catch(IOException e){

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
