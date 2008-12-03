/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;


/**
 *
 * @author James SYNCHRONIZED Bossingham 
 */
import java.io.*;
import org.knopflerfish.util.ByteArray;
import java.math.BigInteger;
import java.lang.InterruptedException;

abstract public class TCP extends Thread{
    protected Main m;
    protected GUI gui;
    protected String entity;
    protected String direction;
    protected String extraInfo;
    
    public static final int HEADER_SIZE = 24;
    public static final int DATA_SIZE = 24;
    public static final int PACKET_SIZE = TCP.HEADER_SIZE + TCP.DATA_SIZE;
    
    protected static final int NOT_IN_USE = -1;
    protected static final int CLOSED = 0;
    protected static final int LISTEN = 1;
    protected static final int SYN_RCVD = 2;
    protected static final int SYN_SENT = 3;
    protected static final int ESTABLISHED = 4;
    protected static final int CLOSE_WAIT = 5;
    protected static final int LAST_ACK = 6;
    protected static final int FIN_WAIT_1 = 7;
    protected static final int FIN_WAIT_2 = 8;
    protected static final int CLOSING = 8;
    
    protected String messageDetails = "";
    
    protected String sender = "x";
    protected String destination = "x";
    protected boolean heartbeat = false;
    
    /**
     * Constructor
     */
    
    public TCP(Main main, String e, GUI g){
        m = main;
        entity =e;
        gui = g;
    }
    
    /**
     * TCP thread
     */

    abstract public void run(); 
    
    protected static byte[] addHeader(byte[] data) {
        // create TCP segment
        byte[] seg = TCP.createTCPSegment();
        // add data to tcp segment
        TCP.setData(data,seg);
        
        return seg;
    }
    
    protected static byte[] stripHeader(byte[] seg) {
       return TCP.getData(seg);
    }
    
    protected byte[] readACKPacket(String entityBuffer, String dest, int sequenceNumber){
        boolean slept = false;
        
        boolean localHeartbeat = false;
        String localSender = "";
        String localDestination = "";
        String localDirection = "";
        String localExtraInfo = "";
        
        try{
            while(true){
                       
                FilenameFilter filter = new TCPFileFilter();
                File f = new File(entityBuffer);
                String[] files = f.list(filter);
                
                System.out.println("TCP "+entity+": Check files for ACK packet: files "+files.length);
                
                if(files != null && files.length != 0){
                
                    for (int iFile = 0; iFile < files.length; iFile++) {


                            // find out direction the TCP Layer is intercepting buffer data
                            // insteam - received (receive header)
                            // outstream - toSend (add header)
                            if (files[iFile].startsWith("received")) 
                                localDirection = "received";
                            else
                                localDirection = "toSend";

                            // check whether heartbeat signal (or not
                            if (files[iFile].indexOf("heartbeat",0) == -1) {
                                localHeartbeat = false;
                            } else {
                                localHeartbeat = true;
                            }

                            // store sender and receiver
                            String[] info = files[iFile].split("[.]");
                            if(info.length == 3 || info.length == 4){
                                localSender = info[1];
                                localDestination = info[2];
                            }

                            //This is to allow extra strings after file name.
                            if(info.length == 5){
                                    localExtraInfo = info[4];
                            }
                            else {
                                    localExtraInfo = null;
                            }
                            
                            FileInputStream fileinputstream = new FileInputStream(entityBuffer+"/"+files[iFile]);
                            int numberBytes = fileinputstream.available();
                            byte[] bytearray = new byte[numberBytes];
                            fileinputstream.read(bytearray);
                            fileinputstream.close();

                            
                            boolean isACKPacket = TCP.getACKFlag(bytearray);
                            int packetSequenceNumber = TCP.getSequenceNumber(bytearray);
                            
                            //System.out.println("file found from "+sender+" to "+destination+": ACK "+isACKPacket+" ACKnum found: "+packetAcknowledgementNumber+" ACKcomp "+acknowledgementNumber);
                            //System.out.println("dest: "+dest);
                            // check file for ACK flag and correct entity
                            if (isACKPacket && localSender.equals(dest)) {

                                //System.out.println("TCP "+entity+": SEQ packet found with required dest");

                                if (packetSequenceNumber < sequenceNumber) {
                                    // if less than SEQ number then assume duplicate => delete + ignore
                                    boolean hadDel = (new File(entityBuffer+"/"+files[iFile]).delete());

                                } else if (packetSequenceNumber == sequenceNumber) {
                                    // else delete and return data
                                    boolean hadDel = (new File(entityBuffer+"/"+files[iFile]).delete());
                                    //System.out.println("TCP "+entity+": ACK packet found return array "+bytearray);
                                    return bytearray;
                                }




                            }







                    }
                
                }
                
                if (slept) {
                    //System.out.println("TCP "+entity+": unable to find ACK packets - need to resend");
                    return null;
                }
                
                try{
                    //Sleep for 3 seconds, then look again for file
                    this.sleep(20000);
                    slept = true;
                }
                catch(java.lang.InterruptedException e){

                }
                
                // end for
                
                
                
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
     * Periodically check to see if data to be read, if so, read it, and return
     * @return Object Packet read
     */
    protected byte[] readPacket(String entityBuffer){
        try{
            while(true){
                       
                FilenameFilter filter = new TCPFileFilter();
                File f = new File(entityBuffer);
                String[] files = f.list(filter);
                java.util.Arrays.sort(files);
                
                //System.out.println("TCP "+entity+": Check for files: total "+files.length);
                
                if(files != null && files.length != 0){
                    //System.out.println("TCP "+entity+": "+"File found "+files[0]);
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
                    if(info.length == 3 || info.length == 4){
                        sender = info[1];
                        destination = info[2];
                    }
                    
                    //This is to allow extra strings after file name.
                    if(info.length == 5){
                            extraInfo = info[4];
                    }
                    else {
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
     */
    abstract protected void sendPacket(byte[] data);
    
    /**
     * Writes data array to given path
     * @param data byte[] to be written
     * @param path location to save file
     */
    protected void writeFile(byte[] data, String path){
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
            //System.out.println("SSW Cannot write file to: " + path);
        }
    }
    
    // format of string {0,X,1} X = don't overwrite that value (assumption string 8 characters in length)
    protected static void writeByteStringToByteArray(String data, byte bt[], int offset) {
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
    
    protected static String convertByteToBinaryString(byte b) {
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
    protected static String generateFlagByteString(boolean flag, int flagConsidered) {
        
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
    protected static boolean generateFlagBooleanValue(String flags, int flagConsidered) {
        
        boolean flag = false;
        char bit = flags.charAt(flagConsidered);
        
        if (bit == '1')
            flag = true;
        
        return flag;
    }
}
