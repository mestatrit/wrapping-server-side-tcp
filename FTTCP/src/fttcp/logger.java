/*
 * The logger applies fault-tolerance by storing all data passed from the client to the server. If the server crashes then
 * the logger continues to store subsequent client data. Once the server has restarted, the logger sends all stored client
 * data on to the server until it has none left, at which point normal operation resumes.
 */

package fttcp;

/**
 *
 * @author James Bossingham
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
    
    private boolean serverAlive = true; // Indicates whether the logger thinks the server is ALIVE of DEAD. Determines whether logger's normal operation loop is performed.
   
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
  
      // Begin a loop that will not stop until finished==true. Therefore the logger will only stop when the program is finished.
      do{
           
           /* BEHAVIOUR UNDER NORMAL OPERATION */
           
           // Only attempt to read packets (and perform actions based upon them) if the server is currently alive.
           if(this.serverAlive == true){
               
           try{
               temp = readPacket();     // Attempt to read a packet.
           }
           catch(Exception e){}
                
               gui.printToScreen("LOGGER: Confirmed Server is alive");
               
               if(sender.equals("NSW")){    // If the arriving packet was received from the North Side Wrap..
                   
                   // Check first position in packet's byte array, to ensure packet contains a readLength
                   if(temp[0] == readLengthFlag){
                       gui.printToScreen("LOGGER: Received a read length from NSW. Storing and ACKing.");
                       // Convert to int everything except flag in order to get readlength.
                       newReadLength = TCP.convertByteArrayToInt(temp, 1);  // Convert readLength to its storable form.
                       readLengthArray[readLengthCounter] = newReadLength;  // Store read length.            
                       sendACK(entity.NSW);   // Supply an acknowledgement
                       readLengthCounter++;   // Increment counter to indicate number of stored readLengths.
                   }
                   
               }else if(sender.equals("SSW")){  // Else, If the arriving packet was received from the South Side Wrap..
                   
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
                        
                        sendACK(entity.SSW);    // Supply an acknowledgement.
                        clientDataCounter++;    // Indicate that another item of client data has been stored.
                        gui.printToScreen("LOGGER: Now has " + clientDataCounter + " items of client data stored.");
                    }
               }
               else if(sender.equals("SRV")){
                   if(temp[0] == 1){
                       gui.printToScreen("LOGGER: Received a heartbeat from the server.");
                       heartbeatThread.beat();  // Let heartbeatThread know that a heartbeat has just been received.       
                   }
               }
               
            }
           else{    // If server is not alive, sleep before checking again.
                   System.out.println("Logger: Normal operation loop is not running (server is not alive)");
                   try{
                       this.sleep(1000);
                   }
                   catch(Exception e){}
                   
               }
      }while(!finished);
    }
    
    /**
     * clientInteraction is a method called by the heartbeatThread once the server has timed out and normal operation has been halted.
     */
    public void clientInteraction(){
        gui.printToScreen("LOGGER: Confirmed Server is dead.");
        gui.printToScreen("LOGGER: Interacting with client...");
        gui.clearServerBuffer();
        
        this.serverAlive = false;

         do{
            
           gui.printToScreen("Server is dead..");
           
           // Check for client data from the NSW
           try{
                temp = readPacket();
                // There must have been something in the buffer, or else try would have failed and gone to catch.
                
                if(sender.equals("NSW")){   // If packet came from North Side Wrap..
                    // This should not occur. Print warning.
                    System.out.println("Server is down, but still receiving data from NSW. Error!");
                }
                else if(sender.equals("SSW")){  //  If packet came from South Side Wrap..

                    // New client data to be stored. Data must be stored, even while server is dead.
                    
                    
                    if(temp[0] == 3){   // If the incoming data is the initial sequence number (during startup)..
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
                        sendACK(entity.SSW);    // Supply an acknowledgement.
                        clientDataCounter++;    // Indicate that another item of client data has been finished.
                    }

                }
                else if(sender.equals("SRV")){      //  If packet came from Server..
                    
                    /* Server is alive again. Beginning restarting behaviour. */
                    
                    m.setUnstable_reads(0);
                    
                    gui.printToScreen("LOGGER: Setting RESTARTING to TRUE");
                    m.setRestarting(true); 

                    System.out.println("LOGGER: Packet arrived from server. Server has restarted.");

                    // Loop through client data array, retrieving and sending (to the server buffer) all of the stored data.
                    for(int i = 0; i < clientDataCounter; i++){
                        System.out.println("LOGGER: Server is restarting. Sending an item of data to Server.");
                        // Create a new byte array to send to server.
                        byte[] catchupData = new byte[TCP.DATA_SIZE];
                        // For each position in the byte array, collect it's value from ClientData[][]
                        for(int j = 1; j < TCP.DATA_SIZE; j++){
                            catchupData[j-1] = ClientData[j][i];
                        }
                        
                        // Print retrieved data.
                        int receivedInt = ByteArray.getShort(catchupData, 0);
                        System.out.println("LOGGER: Retrieved " +  receivedInt);

                        // Send the packet to the server.
                        sendPacket(catchupData, m.getServerAddress());
                    }

                    System.out.println("LOGGER: Finished sending client data to restarting server.");
                    gui.printToScreen("LOGGER: Finished sending stored data to server. Server is up to speed.");
                    
                    // Switch normal operation back on.
                    this.setServerAlive(true);
                    heartbeatThread.setServerAlive(true);
                    heartbeatThread.setInteractingWithClient();
                    
                    // If the packet from the server was a heartbeat, let the heartbeatThread know.
                    if(temp[0] == 1){
                        heartbeatThread.beat();
                    }
                }
           }
           catch(Exception e){
                // There was nothing in the buffer or something else went wrong.
           }

       }while(heartbeatThread.getServerAlive() == false);   // End once a packet from the server is detected (and so serverAlive is set to true).
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
