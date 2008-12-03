/*
 * LoggerTCP.java
 *
 * Created on December 1, 2008, 1:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author csuffk
 */
public class LoggerTCP extends TCP {
    
    private int loggerSequenceNumber = 0;
    
    /** Creates a new instance of LoggerTCP */
    public LoggerTCP(Main main, String e, GUI g) {
        super(main,e,g);
    }
    
    public void run() {
        
        while (true) {
            // read packet
            byte[] data = readPacket("loggerBuffer");

            // asumption: connection established + perfect connection + sequence number not needed

            if (direction.equals("received")) {
                byte[] segContents = TCP.stripHeader(data);
                sendPacket(segContents);
                gui.printToScreen("TCP " +entity +": Data receieved from "+sender);
            } else if (direction.equals("toSend")) { // if sendTo add header
                int sequenceNumber = TCP.getSequenceNumber(data);
                byte[] newSeg = TCP.createTCPSegment();
                TCP.setData(data,newSeg);
                TCP.setSequenceNumber(loggerSequenceNumber,newSeg);
                TCP.setAcknowledgementNumber(loggerSequenceNumber+1,newSeg);
                TCP.setWindowSize((short) TCP.DATA_SIZE,newSeg);
                sendPacket(newSeg);
                gui.printToScreen("TCP " +entity +": Sending data to "+destination);
            }
        }
     
    }
    
    protected void sendPacket(byte[] data) {
        
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
            writeFile(data,"serverBuffer/received.LOG.SRV.TCP");
        }
    }
    
}
