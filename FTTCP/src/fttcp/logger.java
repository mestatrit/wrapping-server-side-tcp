
/*
 * The logger applies fault-tolerance by storing all data passed from the client to the server. 
 * This data is forwarded to the logger by the South Side Wrap. If the server crashes then the logger continues 
 * to store subsequent client data. Once the server has restarted, the logger detects this by noticing the
 * heartbeats arriving from the restarted server and responds by sending all stored client
 * data to the server until it has none left, at which point normal operation resumes.
 */

package fttcp;

/**
 * @author James Bossingham
 * @author Will Isseyegh
 * @author Sam Corcoran
 */
import java.io.*;
import org.knopflerfish.util.ByteArray;

public class logger extends Thread{
    
    private Main m;     // The instance of Main that initiated this instance of logger.
    private String sender;  // This is used to determine the sender (SRV, NSW, SSW) of an incoming packet.
    private String destination; 
    private GUI gui;    // The instance of the user interface, stored so that it can be utilised by the logger (for animations).
    Heartbeat heartbeatThread = new Heartbeat(m, this); // Creates an instance of of Heartbeat so that the logger can detect heartbeats from the server.
    boolean finished=false; // Used in the shutdown behaviour of the logger.
    boolean operatingNormally = true;
    private String appendedString = null;
    
    private byte[] temp = new byte[TCP.DATA_SIZE];  // Temp[] holds incoming packet that has been read by readPacket.
    int initialClientSequenceNumber = 0;    // Stores the client's initial sequence number, given by the client at the beginning of a transaction.
    int length=1000;    // Length is used to supply the maximum number of packets of client data (or read lengths) by defining the number of rows in an array.
    byte[][] ClientData = new byte[TCP.DATA_SIZE][1000];    // Two dimensional array to store multiple byte arrays of client data (i.e. multiple packets).
    int newReadLength = 0;  // Stores newest read length, as supplied by the North Side Wrap.
    int[] readLengthArray = new int[length]; // An array to store sequence of readlengths supplied by the NSW.
    int clientDataCounter = 0;  // Counts how many items of clientData have been stored.
    int readLengthCounter=0;    // Counts how many individual readLengths have been stored.
    
    // The following are flag codes, appearing in the first position of an incoming packet (byte array). 
    // Refer to here to determine what each flag is.
    private byte heartbeatFlag = 1;
    private byte readLengthFlag = 2;
    private byte initClientSeqNumber = 3;
    private byte fwdClientPktFlag = 4;
    
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
        // Start-up  
        gui.printToScreen("LOGGER reporting in.");
    
       // Begin running an instance of heartbeatThread to begin detecting heartbeats from server.
       heartbeatThread.start();
  
       
       /* This loop ensures that normal operation and fault operation loops will both (conditionally) be
        * run until the logger thread is killed.
        */
       while(!finished){
           
           while(this.operatingNormally){
               
                /* Normal Operation Behaviour */

               try{
                   temp = readPacket(true);     // Attempt to read a packet.
               }
               catch(Exception e){System.out.println("Thread error: " + e);}

                   if(temp != null && sender.equals("NSW")){    // If the arriving packet was received from the North Side Wrap..

                       // Check first position in packet's byte array, to ensure packet contains a readLength
                       if(temp[0] == readLengthFlag){
                           gui.printToScreen("LOGGER: Received a read length from NSW. Storing and ACKing.");
                           // Convert to int everything except flag in order to get readlength.
                           newReadLength = TCP.convertByteArrayToInt(temp, 1);  // Convert readLength to its storable form.
                           readLengthArray[readLengthCounter] = newReadLength;  // Store read length.            
                           sendACK(entity.NSW);   // Supply an acknowledgement
                           readLengthCounter++;   // Increment counter to indicate number of stored readLengths.
                       }

                   }else if(temp != null && sender.equals("SSW")){  // Else, If the arriving packet was received from the South Side Wrap..

                        if(temp[0] == 3){   // Check packet flag, and if it is an initialClientSequenceNumber flag..
                            // Store initial client sequence number
                            initialClientSequenceNumber = TCP.convertByteArrayToInt(temp, 1);   // Convert sequence number to its storable form.
                            gui.printToScreen("LOGGER: Received and stored initial Seq number. ACKing SSW.");
                            sendACK(entity.SSW);    // Supply an acknowledgement.
                        }
                        else if(temp[0] == 4){  // Check packet flag, and if it is forwarded client data..

                            int receivedInt = ByteArray.getShort(temp, 1);  // Convert client data to a printable form.
                            gui.printToScreen("LOGGER: Received and stored client data: " +  receivedInt);

                            /* Begin storing client data packet's payload*/

                            // Store packet's sequence number.
                            ClientData[0][clientDataCounter] = (byte)(initialClientSequenceNumber + clientDataCounter);

                            // Loop through the packet (temp[]) copying it into the ClientData array.
                            for(int i = 1; i < temp.length; i++){
                                ClientData[i][clientDataCounter] = temp[i];
                            }

                            sendACK(entity.SSW);    // Supply an acknowledgement to the South Side Wrap.
                            clientDataCounter++;    // Indicate that another item of client data has been stored.
                            gui.printToScreen("LOGGER: Now has " + clientDataCounter + " items of client data stored.");
                        }
                   }
                   else if(temp != null && sender.equals("SRV")){
                       if(temp[0] == 1){                           
                           gui.printToScreen("LOGGER: Received a heartbeat from the server. Server is ALIVE.");
                           heartbeatThread.beat();  // Let heartbeatThread know that a heartbeat has just been received.       
                       }
                   }
               
               // END OF SINGLE ITERATION OF LOOP FOR NORMAL OPERATION
           }
           
           System.out.println("LOGGER: Logger has left normal operation loop.");
           
           /**********************************************************************************************
            *** If the program has reached this point then it must have ended 'normal operation'. This ***
            *** can only occur if Heartbeat thread detects that no heartbeat has arrived for the set   ***
            *** time-out period. It therefore has ended logger's normal operation loop, causing the    ***
            *** logger to enter a loop to handle behaviour under faulty-operation and the subsequent   ***
            *** restarting behaviour.                                                                  ***
            **********************************************************************************************/ 
           
           
           while(!this.operatingNormally){  
               
               /* Faulty Operation Behaviour */
               
               // This loop occurs only when the heartbeat has informed the logger that the server is dead.
               // The logger will continue to accept incoming packets. If the packet is from the client (via
               // the SSW) the payload will be stored, and if the packet is from the server then the logger
               // will perform its restarting behaviour.
               
               gui.printToScreen("LOGGER: Server is dead.."); 

               // Check for client data from the NSW
               try{
                    temp = readPacket(false);   // collect a packet from the logger's buffer.
                    
                    // There must have been something in the buffer, or else try would have failed and gone to catch.

                    if(sender.equals("NSW")){   // If packet is marked as having come from North Side Wrap..
                        // This should not be occurring. Print warning.
                        System.out.println("LOGGER: Server is down, but still receiving data from NSW. Error!");
                    }
                    else if(sender.equals("SSW")){  //  If packet is marked as having come from South Side Wrap..

                        // Packet could contain new client data, which needs to be stored even if the server is dead.
                        // Check flags. Flags are contained in temp[0]. See top of class for description of each flag value.

                        if(temp[0] == 3){   // If the incoming data is the initial sequence number (which occurs during startup)..
                            // Store initial client sequence number
                            initialClientSequenceNumber = TCP.convertByteArrayToInt(temp, 1);
                        }
                        else if(temp[0] == 4){  // If the incoming data is forwarded client data..
                            
                            // It is data from the client which must be stored.
                            gui.printToScreen("LOGGER: Server is dead, but logger is still receiving data from SSW. Storing and ACKing.");

                            // Store packet's sequence number.
                            ClientData[0][clientDataCounter] = (byte)(initialClientSequenceNumber + clientDataCounter);

                            // Loop through the packet (temp[]) copying it into the ClientData array.
                            for(int i = 1; i < temp.length; i++){
                                ClientData[i][clientDataCounter] = temp[i];
                            }
                            
                            sendACK(entity.SSW);    // Supply an acknowledgement to the South Side Wrap.
                            clientDataCounter++;    // Indicate that another item of client data has been finished.
                        }

                    }
                    else if(sender.equals("SRV")){      //  If packet is marked as having come from Server..

                        /***********************************************************************************
                         *** If a packet has arrived from the server (which is likely to be a heartbeat  ***
                         *** then the server must be alive again. Logger should therefore perform        ***
                         *** restarting behaviour and send all stored client data to the server to bring ***
                         *** it up to date with all data the client has sent during the transaction.     ***
                         ***********************************************************************************/
                        m.setUnstable_reads(0);     // Reset North Side Wrap's unstable reads counter.

                        gui.printToScreen("LOGGER: Setting RESTARTING to TRUE");
                        
                        // Indicate that the server is restarting. 
                        // This will cause the North Side Wrap to block outgoing data from the server.
                        // This is necessary to avoid the client receiving multiples of server messages (e.g. two A's)
                        m.setRestarting(true);      

                        System.out.println("LOGGER: Packet arrived from server. Server has restarted.");

                        // Loop through client data array, retrieving and sending (to the server buffer) all of the stored data.
                        for(int i = 0; i < clientDataCounter; i++){
                            System.out.println("LOGGER: Server is restarting. Sending an item of data to Server.");
                            // Create a new byte array to send to server.
                            byte[] catchupData = new byte[TCP.DATA_SIZE];
                            
                            // For each position in the new byte array, collect it's value from ClientData[][]
                            for(int j = 1; j < TCP.DATA_SIZE; j++){
                                catchupData[j-1] = ClientData[j][i];
                            }

                            // Print retrieved data.
                            int retrievedInt = ByteArray.getShort(catchupData, 0);
                            System.out.println("LOGGER: Retrieved " +  retrievedInt);
                            
                            // Mark each outgoing packet with a unique extension, so that they do not overwrite each other in the server buffer.
                            // As the contents of each outgoing packet is numerical, we decided to simply use the contents as the unique extension.
                            if(retrievedInt <10){
                                appendedString = "0" + Integer.toString(retrievedInt);
                            }
                            else{
                                appendedString = Integer.toString(retrievedInt);
                            }

                            gui.printToScreen("LOGGER: Sending OLD data to server: " + retrievedInt);

                            // Send this packet to the server buffer.
                            sendPacket(catchupData, m.getServerAddress());
                            appendedString = null;

                        }   // End of FOR loop.

                        System.out.println("LOGGER: Finished sending client data to restarting server.");
                        gui.printToScreen("LOGGER: Finished sending all stored data to server.");

                        // If the packet from the server was a heartbeat, let the heartbeatThread know.
                        if(temp[0] == 1){
                            heartbeatThread.beat();
                        }

                        heartbeatThread.setDetectBeats(true);   // Let heartbeatThread begin to detect heartbeats again.
                        operatingNormally = true;   // End this faulty-operation loop.
                    }
               }
               catch(Exception e){
                   // There was nothing in the buffer, or something else went wrong.
                   System.out.println("Thread error: " + e);
               }
               System.out.println("REACHED END OF LOGGER's FAULTY OPERATION LOOP (for a single iteration)");               
           } // End of faulty operation loop.
       }
    }
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(boolean useOpNorm){
        // See South Side Wrap for full commenting on this method.
        try{
            while((useOpNorm && operatingNormally) || !useOpNorm){
                FilenameFilter filter = new LOGFileFilter();
                File f = new File("loggerBuffer");
                String[] files = f.list(filter);
                if(files != null && files.length != 0){
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
                    }
                    catch(java.lang.InterruptedException e){
                        System.out.println("Thread error: " + e);
                    }
                }
            }
            return null;
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
        // See South Side Wrap for full commenting on this method.
        if(address == m.getServerAddress()){
            //Put in file called received.TCP in server folder
            gui.log2tcp();
            if(appendedString != null){
                String path = "loggerBuffer/toSend.LOG.SRV.TCP." + appendedString;
                writeFile(data,path);
            }
            else{
                writeFile(data,"loggerBuffer/toSend.LOG.SRV.TCP");
            }
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
        // See South Side Wrap for full commenting on this method.
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
            System.out.println("Thread error: " + e);
            System.out.println("SSW Cannot write file to: " + path);
        }
    }
    
    public void sendACK(entity e){
        /* This may need parameters showing WHO the ACK is meant for (i.e. SSW or NSW) */
        byte[] empty = new byte[TCP.DATA_SIZE]; // Create a new byte array of correct length to be sent as an ACK.
        empty[0] = 0;
        // Write file to different locations depending on the address extension on the packet.
        if(e == entity.NSW){
            writeFile(empty, "loggerBuffer/toSend.LOG.NSW.TCP");
        }
        else if(e == entity.SSW){
            writeFile(empty, "loggerBuffer/toSend.LOG.SSW.TCP");
        }        
    } 
     
     /**
      * This method is called by heartbeatThread in order to change the value of operatingNormally.
      * This therefore decides whether the logger is performing the normal operation loop or the faulty
      * operation loop.
      */
     public void setOperatingNormally(boolean newBoolean){
         System.out.println("LOGGER: Updating state of Logger's 'Operating Normally': " + newBoolean);
         operatingNormally = newBoolean;
         
         // If the logger is being told to perform faulty operation loop then the server must have died.
         // Therefore, clear the serverBuffer.
         if(!newBoolean){
             gui.clearServerBuffer();
         }
     }
}
