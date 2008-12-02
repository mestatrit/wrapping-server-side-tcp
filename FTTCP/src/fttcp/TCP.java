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
import java.math.BigInteger;
import java.lang.InterruptedException;
import java.util.Arrays;

public class TCP extends Thread{
    private Main m;
    private GUI gui;
    private String entity;
    private String direction;
    private String extraInfo;
    
    public static final int HEADER_SIZE = 24;
    public static final int DATA_SIZE = 24;
    public static final int PACKET_SIZE = TCP.HEADER_SIZE + TCP.DATA_SIZE;
    
    private static final int NOT_IN_USE = -1;
    private static final int CLOSED = 0;
    private static final int LISTEN = 1;
    private static final int SYN_RCVD = 2;
    private static final int SYN_SENT = 3;
    private static final int ESTABLISHED = 4;
    private static final int CLOSE_WAIT = 5;
    private static final int LAST_ACK = 6;
    private static final int FIN_WAIT_1 = 7;
    private static final int FIN_WAIT_2 = 8;
    private static final int CLOSING = 8;
    
    private String messageDetails = "";
    
    private String sender = "x";
    private String destination = "x";
    private boolean heartbeat = false;
    private int status = TCP.CLOSED;
    private int serverTCPState = TCP.NOT_IN_USE; // assume tcp connection to logger already established (so only tcp connec to client)
    private int clientTCPState = TCP.NOT_IN_USE;
    private byte[] initialDataFromClient = null;
    private boolean send = true;
    
    /**
     * Constructor
     */
    
    public TCP(Main main, String e, GUI g){
        m = main;
        entity =e;
        gui = g;
        
        serverTCPState = TCP.LISTEN;
        clientTCPState = TCP.CLOSED;
        
        
        // if a passive entity then set status from CLOSED -> LISTEN
        
      
        
            
            
        // receieved - to go in 
        // send - to go out
        // next value: sender e.g. logger
        // next value: sender ultimately
        // who reads it next

        
        // E.G.     NSW and Logger
        // NSW serverBuffer/toSend.NSW.LOG.TCP -- sending inital  message to TCP (ultimately LOG)
        // TCP (in server) reads first - adds header - loggerBuffer/received.NSW.LOG.TCP (message is in server buffer need to process and put in logger buffer)
        // TCP (in logger) reads - strips header - label for logger - loggerBuffer/received.NSW.LOG
        // Logger does stuff need to send to server so passes data to TCP - loggerBuffer/toSend.LOG.NSW.TCP
        // TCP (in logger) adds header and marks for NSW in server - serverBuffer/received.LOG.NSW.TCP
        // TCP (in server) strips header and marks for NSW in server - serverBuffer/received.LOG.NSW
        
        // E.G. App (SRV) and Logger - in error recovery
        
        
        // E.G.    Client and Server
        
        // Client - send message to TCP ultimately Server - clientBuffer/toSend.CLT.SRV.TCP
        // TCP (in client) adds header and marks for SSW - serverBuffer/received.CLT.SRV.SSW
        // SSW (in server) plays with it sends to logger gets ack then marks for TCP - serverBuffer/received.CLT.SRV.TCP
        // TCP (in server) strips header and marks for NSW (ultimately server) - serverBuffer/received.CLT.SRV.NSW
        // * NSW (in server) passes info to server app - serverBuffer/received.CLT.SRV
        // * App client message (send first to NSW) serverBuffer/toSend.SRV.CLT.NSW
        // NSW (in server) send data to TCP serverBuffer/toSend.SRV.CLT.TCP
        // TCP (in server) adds header mark for SSW - serverBuffer/toSend.SRV.CLT.SSW
        // SSW (in server) sends stuff to logger gets ack does stuff mark for client TCP - clientBuffer/received.SRV.CLT.TCP
        // TCP (in client) strips header and marks for client - clientBuffer/received.SRV.CLT
        
        // E.G. SSW and Logger
        
        // SSW (in server) sends message to logger (first stop tcp) - loggerBuffer/receieved.SSW.LOG.TCP
        // TCP (in logger) strips header and makes available for logger - loggerBuffer/receieved.SSW.LOG
        // Logger sends ack (first to TCP) loggerBuffer/toSend.LOG.SSW.TCP
        // TCP (in logger) adds header and sends to SSW - serverBuffer/received.LOG.SSW
        
        // E.G. Heartbeat to Logger
        
        // Heartbeat (in server) sends data to logger (but TCP first) - serverBuffer/toSendHeartbeat.SRV.LOG.TCP
        // TCP (in server) adds header then sends to logger (first to TCP) - loggerBuffer/receievedHeartbeat.SRV.LOG.TCP
        // TCP (in logger) strips data and makes available to logger - loggerBuffer/receievedHeartbeat.SRV.LOG
        
    }
    
    /**
     * TCP thread
     */
    @Override
    public void run(){
        gui.printToScreen("TCP " +entity +" reporting in.");
        
        while (true) {
            send = true;
            byte[] buffer = readPacket();
            if (buffer != null) {
                byte[] data = null;
                
                // add or strip header depending on direction data is travelling
                if (direction.equals("toSend")) {
                    // add header
                    gui.printToScreen("TCP " +entity +": Read packet, adding header.");
                    data = TCP.createTCPSegment();
                    //System.out.println("add header/data index 0: "+data[1]+" buffer index 0: "+buffer[1]);
                    
                    TCP.setData(buffer,data);
                    //System.out.println("add header/data index 0: "+data[25]+" buffer index 0: "+buffer[1]);
                } else {
                    try{
                    // strip header
                    //System.out.println("send "+sender+" destination "+destination);
                        gui.printToScreen("TCP " +entity +": Read packet, stripping header.");
                        //System.out.println("strip header/buffer index 0: "+buffer[25]);
                        data = TCP.stripHeader(buffer);
                        //System.out.println("strip header/data index 0: "+data[1]+" buffer index 0: "+buffer[25]);
                    }
                    catch(Exception e){
                        gui.printToScreen("TCP: ERROR in " + entity + ": " + e);
                    }
                }
                
                // modify status in accordance to contents of data and current state
                if (entity.equals("SRV")) {
                    switch (serverTCPState) {
                        case TCP.LISTEN:
                            if (buffer.length == TCP.PACKET_SIZE) {
                                // check if SYN bit set
                                if (TCP.getSYNFlag(buffer)) {
                                    gui.printToScreen("TCP " +entity +": Received SYN Packet");
                                    // create new TCP seg with SYN, ACK
                                    byte[] response = TCP.createTCPSegment();
                                    TCP.setSYNFlag(true,response);
                                    TCP.setACKFlag(true,response);
                                    gui.printToScreen("TCP " +entity +": Creating/Sending response.");
                                    data = response; // set new seg as data to send
                                    send = false;
                                    sendPacket(data,m.getClientAddress(), true);
                                    // change state
                                    serverTCPState = TCP.SYN_RCVD;
                                }
                            }
                        break;
                        case TCP.SYN_RCVD:
                            if (buffer.length == TCP.PACKET_SIZE) {
                                if (TCP.getACKFlag(buffer)) {
                                    gui.printToScreen("TCP " +entity +": Connection established.");
                                    serverTCPState = TCP.ESTABLISHED;
                                    //Dont forward on
                                    send = false;
                                }
                            }
                        break;
                    }
                } else if (entity.equals("CLT")) {
                     switch (clientTCPState) {
                         case TCP.CLOSED:
                             if (buffer.length == TCP.DATA_SIZE) {
                                 // set SYN flag
                                 TCP.setSYNFlag(true,data);
                                 clientTCPState = TCP.SYN_SENT;
                                 gui.printToScreen("TCP " +entity +": Connection closed, sending SYN packet.");
                                 // store data from client to send when connection established
                                 initialDataFromClient = buffer; 
                             }
                         break;
                         case TCP.SYN_SENT:
                             if (buffer.length == TCP.PACKET_SIZE) {
                                 if (TCP.getSYNFlag(buffer) && TCP.getACKFlag(buffer)) {
                                     send = false;
                                     // create new TCP seg ACK
                                     byte[] response = TCP.createTCPSegment();
                                     TCP.setACKFlag(true,response);
                                     data = response; // set new seg as data to send
                                     clientTCPState = TCP.ESTABLISHED;
                                     gui.printToScreen("TCP " +entity +": Connection established, sending ACK.");
                                     sendPacket(data, m.getServerAddress(), true);
                                     
                                    // create seg from initial data
                                      byte[] dataToSend = TCP.createTCPSegment();
                                      TCP.setData(initialDataFromClient,dataToSend);
                                      
                                      //TO BE REMOVED, HERE TO MAKE SURE LOG ACK ARRIVES BEFORE THIS PACKET
                                      try{
                                            //Sleep for 5 seconds
                                            this.sleep(5000);
                                        }
                                        catch(java.lang.InterruptedException e){

                                        }
                                      gui.printToScreen("TCP " +entity +": Sending original client packet.");
                                      sendPacket(dataToSend, m.getServerAddress(), false); 
                                      initialDataFromClient = null;
                                     
                                     
                                 }
                             }
                         break;
                     }
                }
                if(send){
                    gui.printToScreen("TCP " +entity +": Sending packet.");
                    // send data to the correct buffer
                    sendPacket(data);
                }
            }
        }
 
    }
    
    private static byte[] addHeader(byte[] data) {
        // create TCP segment
        byte[] seg = TCP.createTCPSegment();
        // add data to tcp segment
        TCP.setData(data,seg);
        
        return seg;
    }
    
    private static byte[] stripHeader(byte[] seg) {
       return TCP.getData(seg);
    }
    
    /**
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    private byte[] readPacket(){
        try{
            while(true){
                
                String entityBuffer = "serverBuffer";
                
                if (entity.equals("CLT")) {
                        entityBuffer = "clientBuffer";
                } else if (entity.equals("LOG")) {
                    entityBuffer = "loggerBuffer";
                }
                
                FilenameFilter filter = new TCPFileFilter();
                File f = new File(entityBuffer);
                String[] files = f.list(filter);
                java.util.Arrays.sort(files);
                
                if(files != null && files.length != 0){
                    // find out direction the TCP Layer is intercepting buffer data
                    // insteam - received (receive header)
                    // outstream - toSend (add header)
                    if (files[0].startsWith("received")) 
                        direction = "received";
                    else
                        direction = "toSend";
                    
                    // check whether heartbeat signal (or not
                    if (files[0].indexOf("heartbeat",0) == -1) {
                        heartbeat = false;
                    } else {
                        heartbeat = true;
                    }
                    
                    // store sender and receiver
                    String[] info = files[0].split("[.]");
                    //if(info.length == 3 || info.length == 4){
                        sender = info[1];
                        destination = info[2];
                    //}
                        
                        //This is to allow extra strings after file name.
                    if(info.length == 5){
                            extraInfo = info[4];
                    }
                    else{
                            extraInfo = null;
                    }
                    
                    FileInputStream fileinputstream = new FileInputStream(entityBuffer+"/"+files[0]);
                    int numberBytes = fileinputstream.available();
                    byte[] bytearray = new byte[numberBytes];
                    fileinputstream.read(bytearray);
                    fileinputstream.close();
                    
                    boolean hadDel = (new File(entityBuffer+"/"+files[0]).delete());
                    
                    
                    
                    return bytearray;
                }
                else{
                    try{
                        //Sleep for 3 seconds, then look again for file
                        this.sleep(1000);
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
     
    private void sendPacket(byte[] data, short address, boolean tcp){
        if(address == m.getClientAddress()){
            gui.tcp2ssw();
            gui.srv2clt();
            writeFile(data, "clientBuffer/received.TCP.TCP.TCP");
        }
        else if(address == m.getServerAddress() && tcp){
            gui.clt2srv();
            gui.ssw2tcp();
            writeFile(data, "serverBuffer/received.TCP.TCP.TCP");
        }
        else if(address == m.getServerAddress() && !tcp){
            gui.clt2srv();
            writeFile(data, "serverBuffer/received.CLT.SRV.SSW");
        }
    }
    /**
     * Send packet to address
     * @param object Packet to be sent
     */
    private void sendPacket(byte[] data){
        
        if (entity.equals("SRV")) {
            
            // NSW serverBuffer/toSend.NSW.LOG.TCP -- sending inital  message to TCP (ultimately LOG)
            if (sender.equals("NSW") && destination.equals("LOG")) {
                // TCP (in server) reads first - adds header - loggerBuffer/received.NSW.LOG.TCP (message is in server buffer need to process and put in logger buffer)
                gui.tcp2ssw();
                gui.srv2log();
                writeFile(data,"loggerBuffer/received.NSW.LOG.TCP");
            }
            // TCP (in logger) adds header and marks for NSW in server - serverBuffer/received.LOG.NSW.TCP
            else if (sender.equals("LOG") && destination.equals("NSW")) {
                // TCP (in server) strips header and marks for NSW in server - serverBuffer/received.LOG.NSW
                gui.tcp2nsw();
                writeFile(data,"serverBuffer/received.LOG.NSW");
            }
            // SSW (in server) plays with it sends to logger gets ack then marks for TCP - serverBuffer/received.CLT.SRV.TCP
            else if (sender.equals("CLT") && destination.equals("SRV")) {
                // TCP (in server) strips header and marks for NSW (ultimately server) - serverBuffer/received.CLT.SRV.NSW
                gui.tcp2nsw();
                writeFile(data,"serverBuffer/received.CLT.SRV.NSW");
            }
            // NSW (in server) send data to TCP serverBuffer/toSend.SRV.CLT.TCP
            else if (sender.equals("SRV") && destination.equals("CLT")) {
                // TCP (in server) adds header mark for SSW - serverBuffer/toSend.SRV.CLT.SSW
                gui.tcp2ssw();
                writeFile(data,"serverBuffer/toSend.SRV.CLT.SSW");
            }  
            // handling heartbeat
            
            // Heartbeat (in server) sends data to logger (but TCP first) - serverBuffer/toSendHeartbeat.SRV.LOG.TCP
            else if (sender.equals("SRV") && destination.equals("LOG")) {
                // TCP (in server) adds header then sends to logger (first to TCP) - loggerBuffer/receivedHeartbeat.SRV.LOG.TCP
                gui.htcp2ssw();
                gui.hsrv2log();
                writeFile(data,"loggerBuffer/receivedHeartbeat.SRV.LOG.TCP");
            }
            else if (sender.equals("LOG") && destination.equals("SRV")) {
                // TCP (in server) adds header mark for SSW - serverBuffer/toSend.SRV.CLT.SSW
                gui.tcp2nsw();
                if(extraInfo != null){
                    String path = "serverBuffer/received.LOG.SRV.NSW." + extraInfo; 
                    writeFile(data,path);
                }
                else{
                    writeFile(data,"serverBuffer/received.LOG.SRV.NSW");
                }
                
            }  
            
            
            
        } else if (entity.equals("CLT")) {
            // Client - send message to TCP ultimately Server - clientBuffer/toSend.CLT.SRV.TCP
            if (sender.equals("CLT") && destination.equals("SRV")) {
                // TCP (in client) adds header and marks for SSW - serverBuffer/received.CLT.SRV.SSW
                gui.clt2srv();
                writeFile(data,"serverBuffer/received.CLT.SRV.SSW");
            } 
            // SSW (in server) sends stuff to logger gets ack does stuff mark for client TCP - clientBuffer/received.SRV.CLT.TCP
            else if (sender.equals("SRV") && destination.equals("CLT")) {
                // TCP (in client) strips header and marks for client - clientBuffer/received.SRV.CLT
                gui.tcp2clt();
                writeFile(data,"clientBuffer/received.SRV.CLT");
            }   
        } else { // LOG
            // TCP (in server) reads first - adds header - loggerBuffer/received.NSW.LOG.TCP (message is in server buffer need to process and put in logger buffer)
            if (sender.equals("NSW") && destination.equals("LOG")) {
                // TCP (in logger) reads - strips header - label for logger - loggerBuffer/received.NSW.LOG
                gui.tcp2log();
                writeFile(data,"loggerBuffer/received.NSW.LOG");
            }
            // Logger does stuff need to send to server so passes data to TCP - loggerBuffer/toSend.LOG.NSW.TCP
            else if (sender.equals("LOG") && destination.equals("NSW")) {
                // TCP (in logger) adds header and marks for NSW in server - serverBuffer/received.LOG.NSW.TCP
                gui.log2srv();
                gui.ssw2tcp();
                writeFile(data,"serverBuffer/received.LOG.NSW.TCP");
            }
            // SSW (in server) sends message to logger (first stop tcp) - loggerBuffer/receieved.SSW.LOG.TCP
            else if (sender.equals("SSW") && destination.equals("LOG")) {
                // TCP (in logger) strips header and makes available for logger - loggerBuffer/receieved.SSW.LOG
                gui.tcp2log();
                writeFile(data,"loggerBuffer/received.SSW.LOG");
            }
            // Logger sends ack (first to TCP) loggerBuffer/toSend.LOG.SSW.TCP
            else if (sender.equals("LOG") && destination.equals("SSW")) {
                // TCP (in logger) adds header and sends to SSW - serverBuffer/received.LOG.SSW
                gui.log2srv();
                writeFile(data,"serverBuffer/received.LOG.SSW");
            }       
            // handling hearbeat
            // TCP (in server) adds header then sends to logger (first to TCP) - loggerBuffer/receivedHeartbeat.SRV.LOG.TCP
            else if  (sender.equals("SRV") && destination.equals("LOG")) {
                // TCP (in logger) strips data and makes available to logger - loggerBuffer/receivedHeartbeat.SRV.LOG
                gui.htcp2log();
                writeFile(data,"loggerBuffer/receivedHeartbeat.SRV.LOG");
            }
            else if  (sender.equals("LOG") && destination.equals("SRV")) {
                // TCP (in logger) strips data and makes available to logger - loggerBuffer/receivedHeartbeat.SRV.LOG
                gui.log2srv();
                gui.ssw2tcp();
                if(extraInfo != null){
                    String path = "serverBuffer/received.LOG.SRV.TCP." + extraInfo; 
                    writeFile(data,path);
                }
                else{
                    writeFile(data,"serverBuffer/received.LOG.SRV.TCP");
                }
            }
            

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
    
    // format of string {0,X,1} X = don't overwrite that value (assumption string 8 characters in length)
    private static void writeByteStringToByteArray(String data, byte bt[], int offset) {
        // get string rep of byte needed to replace
        byte[] currentData = new byte[1];
        currentData[0] = bt[offset];
        String currentDataRepresentation = TCP.convertByteToBinaryString(currentData[0]);
        
        // convert string to array to replace required values
        char[] currentDataRepAsArray = currentDataRepresentation.toCharArray();
        
        
        for (int i = 0; i < 8; i++) {
            char c = data.charAt(i);
            
            switch (c) {
                case '0':
                case '1':
                    currentDataRepAsArray[i] = c;
            }
        }
        
        // update string
        currentDataRepresentation = String.copyValueOf(currentDataRepAsArray);
        
        // now convert new string to byte
        byte newData = (new BigInteger(currentDataRepresentation,2)).byteValue();
        // write new data to byte array
        ByteArray.setByte(newData,bt,offset);
    }
    
    public static void setData(byte[] data, byte[] seg) {  
        int newDataIndex = 0;
        
        // fill seg array (the data half) with data info
        for (int i = TCP.DATA_SIZE; i < TCP.PACKET_SIZE; i++) {
            seg[i] = data[newDataIndex];
            newDataIndex++;
        }
    }
    
    public static byte[] getData(byte[] seg) {
       byte[] data = new byte[TCP.DATA_SIZE];
       
       // get info from data segment index by index;
       int newDataIndex = 0;
       for (int i = TCP.DATA_SIZE; i < TCP.PACKET_SIZE; i++) {
           //System.out.println("data length "+data.length+" seg length "+seg.length+" dataindex "+newDataIndex+" i "+i);
           data[newDataIndex] = seg[i];
           newDataIndex++;
       }
       
       return data;
    }
    
    public static byte[] convertDataToByteArray(byte i) {
        byte[] bs = new byte[1];
        bs[0] = i;
        return bs;
    }
    
    public static byte[] convertDataToByteArray(short i) {
        return TCP.convertDataToByteArray((int) i);
    }
    
    public static byte[] convertDataToByteArray(int i) {
        BigInteger iAsBigInteger = new BigInteger(Integer.toString(i));
        return iAsBigInteger.toByteArray();
    }
    
    public static int convertByteArrayToInt(byte[] b, int offset) {
        return ByteArray.getInt(b,offset);
    }
    
    public static short convertByteArrayToShort(byte[] b, int offset) {
        return ByteArray.getShort(b,offset);
    }
    
    public static byte convertByteArrayToByte(byte[] b, int offset) {
        return ByteArray.getByte(b,offset);
    }
    
    private static String convertByteToBinaryString(byte b) {
        // get string rep of byte needed to replace
        byte[] bAsArray = new byte[1];
        bAsArray[0] = b;
        
        BigInteger bAsBigInteger;
        String bAsString;
        
        // essentially different calculation method is used if byte is negative (please see signum in BigInteger java docs)
        if (b < 0) {
            bAsBigInteger = new BigInteger(-1,bAsArray);
            bAsString = bAsBigInteger.toString(2).substring(1);
        } else {
            bAsBigInteger = new BigInteger(bAsArray);
            bAsString = bAsBigInteger.toString(2);
        }
        
        // check for missing leading zeros and add if neccessary
        int leadingZeros = 8 - bAsString.length();
        
        // add leading zeros if any
        if (leadingZeros > 0) {
            for (int zeros = 1; zeros <= leadingZeros; zeros++) {
                bAsString = "0"+bAsString;
            }
        }
        
        return bAsString;
    }
    
    public static byte[] createTCPSegment() {
        // create array and return pointer
        byte[] newTCPSegment = new byte[TCP.PACKET_SIZE];
        
        // initialize everything to zero
        
        for (int i = 0; i < newTCPSegment.length; i++) {
            newTCPSegment[i] = (byte) 0;
        }
        
        return newTCPSegment;
    }
    
    public static void setSourcePort(short sourcePort, byte[] seg) {
        ByteArray.setShort(sourcePort,seg,0);
    }
    
    public static short getSourcePort(byte[] seg) {
        return ByteArray.getShort(seg,0);
    }
    
    public static void setDestinationPort(short destinationPort, byte[] seg) {
        ByteArray.setShort(destinationPort,seg,2);
    }
    
    public static short getDestinationPort(byte[] seg) {
        return ByteArray.getShort(seg,2);
    }
    
    public static void setSequenceNumber(int sequenceNumber, byte[] seg) {
        ByteArray.setInt(sequenceNumber,seg,4);
    }
    
    public static int getSequenceNumber(byte[] seg) {
        return ByteArray.getInt(seg,4);
    }
    
    public static void setAcknowledgementNumber(int acknowledgementNumber, byte[] seg) {
        ByteArray.setInt(acknowledgementNumber,seg,8);
    }
    
    public static int getAcknowledgementNumber(byte[] seg) {
        return ByteArray.getInt(seg,8);
    }
    
    public static void setDataOffset(String dataOffset, byte[] seg) {
        // assumption dataoffset is of length 4
        dataOffset += "XXXX"; // overwriting byte at offset but not the latter 4 bits
        TCP.writeByteStringToByteArray(dataOffset,seg,12);
    }
    
    public static String getDataOffset(byte[] seg) {
        // get byte (dataoffset + reserved)
        byte dataOffset = ByteArray.getByte(seg,12);
        String dataOffsetAsString = TCP.convertByteToBinaryString(dataOffset);
        
        // return first 4 characters of string
        return dataOffsetAsString.substring(0,3);
    }
    
    public static void setReserved(String dataOffset, byte[] seg) {
        // assumption dataoffset is of length 4
        dataOffset = "XXXX"+dataOffset; // overwriting byte at offset but not the latter 4 bits
        TCP.writeByteStringToByteArray(dataOffset,seg,12);
    }
    
    public static String getReserved(byte[] seg) {
        // get byte (dataoffset + reserved)
        byte reserved = ByteArray.getByte(seg,12);
        String reservedAsString = TCP.convertByteToBinaryString(reserved);
        
        // return last 4 characters of string
        return reservedAsString.substring(4,7);
    }
    
    public static void setCWRFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,0),seg,13);
    }

    public static boolean getCWRFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
        // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,0);
    }
    
    public static void setECEFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,1),seg,13);
    }
    
    public static boolean getECEFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,1);
    }
    
    public static void setURGFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,2),seg,13);
    }
    
    public static boolean getURGFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,2);
    }
    
    public static void setACKFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,3),seg,13);
    }
    
    public static boolean getACKFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,3);
    }
    
    public static void setPSHFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,4),seg,13);
    }
    
    public static boolean getPSHFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,4);
    }
    
    public static void setRSTFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,5),seg,13);
    }
    
    public static boolean getRSTFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,5);
    }
    
    public static void setSYNFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,6),seg,13);
    }
    
    public static boolean getSYNFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,6);
    }
    
    public static void setFINFlag(boolean flag, byte[] seg) {
        // write byte string
        TCP.writeByteStringToByteArray(TCP.generateFlagByteString(flag,7),seg,13);
    }
    
    public static boolean getFINFlag(byte[] seg) {
        // get byte (all 8 flag bits)
        byte flags = ByteArray.getByte(seg,13);
        String flagsAsString = TCP.convertByteToBinaryString(flags);
        
       // return boolean equiv of character at index specified in string
        return TCP.generateFlagBooleanValue(flagsAsString,7);
    }
    
    public static void setWindowSize(short windowSize, byte[] seg) {
        ByteArray.setShort(windowSize,seg,14);
    }
    
    public static short getWindowSize(byte[] seg) {
        return ByteArray.getShort(seg,14);
    }
    
    public static void setChecksum(short checksum, byte[] seg) {
        ByteArray.setShort(checksum,seg,16);
    }
    
    public static short getChecksum(byte[] seg) {
        return ByteArray.getShort(seg,16);
    }
    
    public static void setUrgentPointer(short urgentPointer, byte[] seg) {
        ByteArray.setShort(urgentPointer,seg,18);
    }
    
    public static short getUrgentPointer(byte[] seg) {
        return ByteArray.getShort(seg,18);
    }
    
    public static void setOptions(int options, byte[] seg) {
        ByteArray.setInt(options,seg,20);
    }
    
    public static int getOptions(byte[] seg) {
        return ByteArray.getInt(seg,20);
    }
    
    // only overwritting single bit of byte so mark all other bits as 'don't care' (X)
    private static String generateFlagByteString(boolean flag, int flagConsidered) {
        
        String flagByteString = "";
        for (int i = 0; i < 8; i++) {
            // if bit is one needed to be considered insert new value
            if (i == flagConsidered) {
                if (flag)
                    flagByteString += "1";
                else
                    flagByteString += "0";
            } else {
                flagByteString += "X";
            }
        }
        
        return flagByteString;
    }
    // assumption string input is of length 8 {0,1}
    private static boolean generateFlagBooleanValue(String flags, int flagConsidered) {
        
        boolean flag = false;
        char bit = flags.charAt(flagConsidered);
        
        if (bit == '1')
            flag = true;
        
        return flag;
    }
}
