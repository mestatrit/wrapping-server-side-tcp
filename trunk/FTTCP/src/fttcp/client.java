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
      /*  boolean Serverreplied = true;
        do
            for (int i = 0; i<20; i++)
            {
                sendPacket(intToByteArr(i),m.getServerAddress());
                System.out.println("client sending" + i);
            }
        while (Serverreplied = true); */
        
        //set initial number to send
        int numberSend = 1;
        
        //client to send numbers up to 10
        while(numberSend <= 10) {
            
            //convert integer to send to byte array
            byte[] byteNumberSend = intToByteArr(numberSend);
            
            //send number to server
            sendPacket(byteNumberSend, m.getServerAddress());
            System.out.println("Client sending " + byteNumberSend);
            
            //while server has not replied, wait
            while(readPacket()==null){
               try {
                   this.sleep(1000);
               }
               catch(java.lang.InterruptedException e){
                        }
          }
            
            //when server replies, increase number to send
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
        if(address == m.getServerAddress()){
            //Put in file called toSend.Server in client folder
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
