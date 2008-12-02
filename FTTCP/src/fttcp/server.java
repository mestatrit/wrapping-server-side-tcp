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
    private GUI gui;
    ServerHeartbeat serverHeartbeatThread = new ServerHeartbeat(m, this);
    
    public server(Main main, GUI g){
        m = main;
        gui =g;
    }

    /**
     * Server thread
     */
    @Override
    public void run(){
        gui.printToScreen("Server reporting in.");
        short numberRecv = 1;
        
        
        // Begin serverHeartbeat here.
               System.out.println("SERVER: Server Heartbeat begins.");
        serverHeartbeatThread.start();
        
        
        while(true){
            
           /* sendPacket(readPacket(),m.getClientAddress());
            System.out.println("server sending" + readPacket());*/

        byte[] serverReadData = readPacket();
        char returnLetter = 'a';
        short returnNumber = 0;
        byte[] byteReturnLetter;
        short readData =0;
        
        //convert data read to type short
        if(serverReadData.length == 1){
            byte[] stuffedRD = new byte[2];
            stuffedRD[0] = 0;
            stuffedRD[1] = serverReadData[0];
            readData = TCP.convertByteArrayToShort(stuffedRD, 0);
        }
        else{
            readData = TCP.convertByteArrayToShort(serverReadData, 0);
        }
        gui.printToServer("Received " + readData);
        gui.printToScreen("SRV: Received " + readData);
        
        //get char representation
        returnLetter = (char)numberRecv; /*readData;*/
        
        //set correct ASCII representation
        returnNumber = (short)(numberRecv + 64);
        
        //convert short to byte array
       // byteReturnLetter = TCP.convertDataToByteArray(returnNumber);
        byteReturnLetter = new byte[TCP.DATA_SIZE];
            
        ByteArray.setShort(returnNumber,byteReturnLetter,0);
        //send packet to client
        gui.printToServer("Sending " + (char)returnNumber);
        gui.printToScreen("SRV: Sending " + (char)returnNumber);
        sendPacket(byteReturnLetter, m.getClientAddress());
        numberRecv++;
       
        System.out.println("SERVER LOOP HAS FINISHED EXECUTING. ABOUT TO REPEAT.");
        
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
                    fileinputstream.close();
                    boolean hadDel = (new File("serverBuffer/"+files[0]).delete());
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
            gui.srv2nsw();
            writeFile(data,"serverBuffer/toSend.SRV.CLT.NSW");
        }
        else if(address == m.getLoggerAddress()){
            //Put in file called received.TCP in client folder
            gui.hsrv2nsw();
            gui.hnsw2tcp();
            writeFile(data,"serverBuffer/toSend.SRV.LOG.TCP");
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
     
    public void sendHeartbeat(){
        
        byte[] heartbeatByte=new byte[TCP.DATA_SIZE];
        heartbeatByte[0] = 1;
        
        sendPacket(heartbeatByte,m.getLoggerAddress());
    } 
     
    public void killServerHeartbeat(){
        serverHeartbeatThread.stop();
        System.out.println("SERVER :killServerHeartbeat() is called");
    }
    
}
