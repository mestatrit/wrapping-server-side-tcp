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

public class TCP extends Thread{
    private Main m;
    private String entity;
    private String direction;
    private static final int PACKET_SIZE = 48; // min size of 24 (would mean no data) 
    
    /**
     * Constructor
     */
    
    public TCP(Main main){
        m = main;
        entity ="test";
        
    }
    
    public TCP(Main main, String e){
        m = main;
        entity =e;
        
    }
    
    /**
     * TCP thread
     */
    @Override
    public void run(){
        System.out.println("test run thread");
        // when buffer is reabable i.e. sendToTCP then read and act on data
        // use entity and direction to indicate what action to take

        
        byte[] sampleTCPSegment = new byte[TCP.PACKET_SIZE];
        String sSampleTCPSegment = "";
        
        ByteArray.setShort((short) 23846,sampleTCPSegment,6);
        ByteArray.setByte((byte) 1, sampleTCPSegment,0);
        ByteArray.setByte((byte) 2, sampleTCPSegment,1);
        ByteArray.setByte((byte) 3, sampleTCPSegment,2);
        ByteArray.setByte((byte) 4, sampleTCPSegment,3);
        ByteArray.setByte((byte) 3, sampleTCPSegment,4);
        ByteArray.setByte((byte) 7, sampleTCPSegment,5);
        
        
        TCP.writeByteStringToByteArray("X1X110XX", sampleTCPSegment, 7);
        
        //byte[] test = intToByteArr(16908546);
        System.out.println("coversion is "+((byte)sampleTCPSegment[0]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[1]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[2]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[3]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[4]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[5]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[6]));
        System.out.println("coversion is "+((byte)sampleTCPSegment[7]));
        
        // test BigInteger
        
        BigInteger bi = new BigInteger("00000000",2);
        byte b = bi.byteValue();
        System.out.println("byte value "+b);
        byte[] ba = new byte[1];
        ba[0] = b;
        BigInteger bi2;
        String s;
        
        if (b < 0) {
            bi2 = new BigInteger(-1,ba);
            s = bi2.toString(2).substring(1);
        } else {
            bi2 = new BigInteger(ba);
            s = bi2.toString(2);
        }
        
        System.out.println("original string is "+s);
        
        
        // check TCP segment creation code
        byte[] seg = TCP.createTCPSegment();
        
        // add values to fields
        TCP.setSourcePort((short) 2000,seg);
        TCP.setDestinationPort((short) 1080,seg);
        TCP.setSequenceNumber(102456,seg);
        TCP.setAcknowledgementNumber(102455,seg);
        TCP.setDataOffset("1001",seg);
        TCP.setReserved("0111",seg);
        TCP.setCWRFlag(true,seg);
        TCP.setECEFlag(true,seg);
        TCP.setURGFlag(false,seg);
        TCP.setACKFlag(true,seg);
        TCP.setPSHFlag(false,seg);
        TCP.setRSTFlag(false,seg);
        TCP.setSYNFlag(true,seg);
        TCP.setFINFlag(false,seg);
        TCP.setWindowSize((short) 10000, seg);
        TCP.setChecksum((short) 0,seg);
        TCP.setUrgentPointer((short) 999, seg);
        TCP.setOptions(23797474,seg);
        
        
        // now read byte array and return values
        System.out.println("source port: "+TCP.getSourcePort(seg));
        System.out.println("dest port: "+TCP.getDestinationPort(seg));
        System.out.println("seq num: "+TCP.getSequenceNumber(seg));
        System.out.println("ack num: "+TCP.getAcknowledgementNumber(seg));
        System.out.println("dataoffset + reserved string "+TCP.convertByteToBinaryString(seg[12]));
        System.out.println("data offset: "+TCP.getDataOffset(seg));
        System.out.println("reserved: "+TCP.getReserved(seg));
        System.out.println("flags string "+TCP.convertByteToBinaryString(seg[13]));
        System.out.println("cwr: "+TCP.getCWRFlag(seg));
        System.out.println("ece: "+TCP.getECEFlag(seg));
        System.out.println("urg: "+TCP.getURGFlag(seg));
        System.out.println("ack: "+TCP.getACKFlag(seg));
        System.out.println("psh: "+TCP.getPSHFlag(seg));
        System.out.println("rst: "+TCP.getRSTFlag(seg));
        System.out.println("syn: "+TCP.getSYNFlag(seg));
        System.out.println("fin: "+TCP.getFINFlag(seg));
        System.out.println("window size: "+TCP.getWindowSize(seg));
        System.out.println("checksum: "+TCP.getChecksum(seg));
        System.out.println("urgent pointer: "+TCP.getUrgentPointer(seg));
        System.out.println("options: "+TCP.getOptions(seg));
        
        // conevrt int to byte array
        byte[] bTest = TCP.convertDataToByteArray((short) -3784);
        // -10110010101010011111100101
        
        for (int i = 0; i < bTest.length; i++) {
            System.out.println("index "+i+" "+bTest[i]+" "+TCP.convertByteToBinaryString(bTest[i]));
        }
        
        System.out.print("Value of byte array "+ByteArray.getShort(bTest,0));
 
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
