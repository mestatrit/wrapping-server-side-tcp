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

public class logger extends Thread{
    private Main m;
    private String sender;
    private String destination;
    private GUI gui;
    int emptyReadCounter = 0;   // For counting how many consecutive times a read has been performed when nothing has been available to read.
    int initialClientSequenceNumber = 0;
    int length=TCP.DATA_SIZE;// Need to update length!!!
       int newReadLength = 0;
       int[] readLengthArray = new int[1000]; //to have the array received from the client
          boolean finished=false;
    
    private byte heartbeatFlag = 1;
    private byte readLengthFlag = 2;
    private byte initClientSeqNumber = 3;
    private byte fwdClientPktFlag = 4;
    private byte[] temp = new byte[TCP.DATA_SIZE];
    Heartbeat heartbeatThread = new Heartbeat(m, this);
    private boolean serverAlive = true;
   
    byte[][] ClientData = new byte[TCP.DATA_SIZE][1000];
    int clientDataCounter = 0;
    int readLengthCounter=0;
    
    private enum entity {SSW,NSW,SRV};
    Thread WrappersThread= new Thread();
    Thread bufferChecker= new Thread();
    /**
     * Constructor
     */
    public logger(Main main, GUI g){
        m = main;
        gui = g;
    }
    
    /**
     * Logger thread
     */
    @Override
    public void run(){
        gui.printToScreen("Logger reporting in.");
       
       
    

       // Start-up   
       // Create instance of Heartbeat
       
       heartbeatThread.start();
  
  
      do{
            /*need to send data to server when server restores */     
                
           //perform the code below until it is said to stop (program finishes)
           try{
               temp = readPacket();
               
                int receivedInteger = ByteArray.getShort(temp, 1);
           }
           catch(Exception e){}
      
           if(this.serverAlive == true){
               gui.printToScreen("LOG: Confirmed Server is alive");
               
               // BEHAVIOUR UNDER NORMAL OPERATION
               
               if(sender.equals("NSW")){
                   gui.printToScreen("LOG: Data comes from North Side Wrapper");
                   // Only ever recieve read lengths from NSW, but check flag type anyway.
                   if(temp[0] == readLengthFlag){
                       gui.printToScreen("LOG: NSW data was a read length. Storing and ACKing.");
                       // Convert to int everything except flag in order to get readlength.
                       newReadLength = TCP.convertByteArrayToInt(temp, 1);
                       readLengthArray[readLengthCounter] = newReadLength;  // Store read length.            
                       sendACK(entity.NSW);   // Supply an acknowledgement
                       gui.printToScreen("LOG: Have sent ACK to NSW.");
                       readLengthCounter++;   // Increment counter to indicate number of stored readLengths.
                   }
                       
               }else if(sender.equals("SSW")){
                   gui.printToScreen("LOG: Data comes from South Side Wrapper");
                    if(temp[0] == 3){   // If the incoming data is the initial sequence number (during startup)..
                        // Store initial client sequence number
                        initialClientSequenceNumber = TCP.convertByteArrayToInt(temp, 1);
                        gui.printToScreen("LOG: Stored initial Seq number. ACKing SSW.");
                        sendACK(entity.SSW);
                    }
                    else if(temp[0] == 4){  // If the incoming data is forwarded client data..
                       
                        int receivedInt = ByteArray.getShort(temp, 1);
                        System.out.println("LOGGER: Stored " +  receivedInt);
                       
                        // It is data from the client which must be stored.
                        ClientData[0][clientDataCounter] = (byte)(initialClientSequenceNumber + clientDataCounter);
                        // Copy each position in temp, into the new array. 
                         
                        for(int i = 1; i < temp.length; i++){
                            ClientData[i][clientDataCounter] = temp[i];
                        }
                        gui.printToScreen("LOG: Is now sending ACK to SSW.");
                        sendACK(entity.SSW);
                        clientDataCounter++;
                        System.out.println("LOGGER: Client data counter is: " + clientDataCounter);
                    }
               }
               else if(sender.equals("SRV")){
                   System.out.println("LOG: Detected an unknown packet.");
                   if(temp[0] == 1){
                       heartbeatThread.beat();          
                   }
               }
        }        
      }while(!finished);
}
    
    public void clientInteraction(){
        gui.printToScreen("LOG: Confirmed Server is dead");
                gui.printToScreen("LOG: Interacting with client...");
                this.serverAlive = false;
                
                // Server is alive again. Begin resending of data.
                m.setRestarting(true); 
                
                 do{
                   // Check for client data from the NSW
                   try{
                        temp = readPacket();
                        // There must have been something in the buffer, or else try would have failed and gone to catch        
                        if(sender.equals("NSW")){
                            // This should not occur. Ignore any messages from the NSW?
                            System.out.println("Server is down, but still receiving data from NSW. Error!");
                        }
                        else if(sender.equals("SSW")){
                            
                            // New client data to be stored.
                            gui.printToScreen("LOG: Data comes from South Side Wrapper");
                            if(temp[0] == 3){   // If the incoming data is the initial sequence number (during startup)..
                                // Store initial client sequence number
                                initialClientSequenceNumber = TCP.convertByteArrayToInt(temp, 1);
                            }
                            else if(temp[0] == 4){  // If the incoming data is forwarded client data..
                                // It is data from the client which must be stored.
                                ClientData[0][clientDataCounter] = (byte)(initialClientSequenceNumber + clientDataCounter);
                                // Copy each position in temp, into the new array.
                                for(int i = 1; i < temp.length; i++){
                                    ClientData[i][clientDataCounter] = temp[i];
                                }
                                sendACK(entity.SSW);
                                clientDataCounter++;
                            }
                            
                        }
                        else if(sender.equals("SRV")){
                            System.out.println("LOGGER: Packet arrived from server. Server has restarted.");
                            
                            // For each packet of client data..
                            for(int i = 0; i < clientDataCounter; i++){
                                System.out.println("LOG: Server is restarting. Sending an item of data to Server.");
                                // Create a new byte array to send to server.
                                byte[] catchupData = new byte[TCP.DATA_SIZE];
                                // For each position in the byte array, collect it's value from ClientData[][]
                                for(int j = 0; j < TCP.DATA_SIZE; j++){
                                    catchupData[j] = ClientData[j][i];
                                }
                                
                                int receivedInt = ByteArray.getShort(catchupData, 1);
                                System.out.println("LOGGER: Retrieved " +  receivedInt);
                                
                                // Send the packet to the server.
                                sendPacket(catchupData, m.getServerAddress());
                            }
                            
                            System.out.println("LOGGER: Finished sending client data to restarting server.");
                            
                            this.setServerAlive(true);
                            heartbeatThread.setServerAlive(true);
                            heartbeatThread.setInteractingWithClient();
                            if(temp[0] == 1){
                                heartbeatThread.beat();
                            }
                            m.setRestarting(false);
                        }
                   }
                   catch(Exception e){
                        // There was nothing in the buffer or something else went wrong.
                   }
                   
               }while(heartbeatThread.getServerAlive() == false);
        
    }
    
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                FilenameFilter filter = new LOGFileFilter();
                File f = new File("loggerBuffer");
                String[] files = f.list(filter);
                if(files != null && files.length != 0){
                    emptyReadCounter = 0;   // Reset the consecutive read counter
                    FileInputStream fileinputstream = new FileInputStream("loggerBuffer/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    fileinputstream.close();
                    boolean hadDel = (new File("loggerBuffer/"+files[0]).delete());
                    //Find and set sender
                    int length  = files[0].length();
                    String[] info = files[0].split("[.]");
                    if(info.length == 3 || info.length == 4){
                        sender = info[1];
                        destination = info[2];
                    }
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
                        this.sleep(3000);
                        emptyReadCounter++;     // Increment counter - another empty read has occured.
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
            gui.log2tcp();
            writeFile(data,"loggerBuffer/toSend.LOG.SRV.TCP");
        }
        else if(address == m.getClientAddress()){
            //Put in file called received.TCP in client folder
            gui.log2tcp();
            writeFile(data,"loggerBuffer/toSend.LOG.CLT.TCP");
        }
        else if(address == m.getSSWAddress()){
            //Put in file called received.TCP in client folder
            gui.log2tcp();
            writeFile(data,"loggerBuffer/toSend.LOG.SSW.TCP");
        }
        else if(address == m.getNSWAddress()){
            //Put in file called received.TCP in client folder
            gui.log2tcp();
            writeFile(data,"loggerBuffer/toSend.LOG.NSW.TCP");
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
    
    public void sendACK(entity e){
        /* This may need parameters showing WHO the ACK is meant for (i.e. SSW or NSW) */
        byte[] empty = new byte[TCP.DATA_SIZE];
        empty[0] = 0;
        if(e == entity.NSW){
            writeFile(empty, "loggerBuffer/toSend.LOG.NSW.TCP");
            gui.printToScreen("LOG: Sending ACK to NSW");
        }
        else if(e == entity.SSW){
            writeFile(empty, "loggerBuffer/toSend.LOG.SSW.TCP");
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
     
     public void setServerAlive(boolean newStatus){
         this.serverAlive = newStatus;
     }     
}
