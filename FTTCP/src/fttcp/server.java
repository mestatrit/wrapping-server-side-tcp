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
import org.knopflerfish.util.ByteArray;

public class server extends Thread{
    private Main m;
    
    public server(Main main){
        m = main;
    }

    /**
     * Server thread
     */
    @Override
    public void run(){
        
        while(true){
           /* sendPacket(readPacket(),m.getClientAddress());
            System.out.println("server sending" + readPacket());*/
       
        byte[] serverReadData = readPacket();
        char returnLetter = 'a';
        short returnNumber = 0;
        byte[] byteReturnLetter;
        
        //if there is data which has been read
        if(serverReadData != null) {
        
        //convert data read to type short
        short readData = TCP.convertByteArrayToShort(serverReadData, 0);
        
        //get char representation
        returnLetter = (char)readData;
        
        //set correct ASCII representation
        returnNumber = (short)(readData + 64);
        
        //convert short to byte array
        byteReturnLetter = TCP.convertDataToByteArray(returnNumber);
        
        //send packet to client
        sendPacket(byteReturnLetter, m.getClientAddress());
        System.out.println("Server sending " + (char)returnNumber);
     }
        else {
            try {
                this.sleep(2000);
            }
            catch(java.lang.InterruptedException e){
                  }
        }
    }
    }
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new SRVFileFilter();
                File f = new File("serverBuffer");
                String[] files = f.list(filter);
                if(files.length != 0){
                    FileInputStream fileinputstream = new FileInputStream("serverBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    boolean hadDel = (new File(files[0]).delete());
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
        if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            writeFile(data,"serverBuffer/toSend.SRV.CLT.NSW");
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
