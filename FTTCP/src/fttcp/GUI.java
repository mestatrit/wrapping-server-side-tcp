/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 *
 * @author James Bossingham
 */
public class GUI extends Thread{
    private JTextArea output = new JTextArea();
    private JTextArea servCon = new JTextArea();
    private JTextArea cltCon = new JTextArea();
    private GUICanvas canvas;
    private int wait = 200;

    private int lines = 0;
    private int servLines = 0;
    private int cltLines = 0;
    private imageMap blankDot = new imageMap(0,0,(byte)-1);
    private JScrollPane jScrollCan;
    private Point[] stringPoints;
    private String[] strings;
    private boolean isInit = false;
    
        @Override
    public void run(){
        output.setText("");
        servCon.setText("");
        initGUI();   
    }

    public void clearBuffers() {
        clearServerBuffer();
        clearClientBuffer();
        clearLoggerBuffer();
    }

    private void clearClientBuffer() {
        File f = new File("clientBuffer");
        String[] files = f.list();
        if(files != null && files.length != 0){
            for(int i=0;i<files.length;i++){
                //Leave SVN folder
                if(!files[i].equalsIgnoreCase(".svn")){
                     boolean hadDel = (new File("clientBuffer/"+files[i]).delete());
                }
            }
        }
    }

    private void clearLoggerBuffer() {
        File f = new File("loggerBuffer");
        String[] files = f.list();
        if(files != null && files.length != 0){
            for(int i=0;i<files.length;i++){
                //Leave SVN folder
                if(!files[i].equalsIgnoreCase(".svn")){
                     boolean hadDel = (new File("loggerBuffer/"+files[i]).delete());
                }
            }
        }
    }

    private void clearServerBuffer() {
        File f = new File("serverBuffer");
        String[] files = f.list();
        if(files != null && files.length != 0){
            for(int i=0;i<files.length;i++){
                //Leave SVN folder
                if(!files[i].equalsIgnoreCase(".svn")){
                     boolean hadDel = (new File("serverBuffer/"+files[i]).delete());
                }
            }
        }
    }
    
    private void initGUI(){
        clearBuffers();
        canvas = new GUICanvas(new ImageIcon("dot2.png").getImage(),new ImageIcon("dot3.png").getImage(),54);
        
        JFrame GUIFrame = new JFrame();
        canvas.setStretched(false);
        canvas.setPreferredSize(new Dimension(550,500));
        jScrollCan = new JScrollPane(canvas);
        jScrollCan.setPreferredSize(new Dimension(550,500));
        jScrollCan.getVerticalScrollBar().setUnitIncrement(30);

        JMenuBar GUIMenu = new JMenuBar();
        GUIMenu.setName("GuiMenu");
        JMenu fileMenu = new JMenu();
        fileMenu.setText("File");
        JMenu editMenu = new JMenu();
        GUIMenu.add(fileMenu);
        editMenu.setText("Edit");
        JMenu speedMenu = new JMenu();
        GUIMenu.add(editMenu);
        JMenuItem killServer = new JMenuItem();
        killServer.setText("Kill Server");
        editMenu.add(killServer);
        killServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                killServerActionPerformed(evt);
            }
        });
        JMenuItem restartServer = new JMenuItem();
        restartServer.setText("Restart Server");
        editMenu.add(restartServer);
        restartServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartServerActionPerformed(evt);
            }
        });
        speedMenu.setText("Speed");
        editMenu.add(speedMenu);
        JMenuItem slowItem = new JMenuItem();
        slowItem.setText("1000");
        speedMenu.add(slowItem);
        slowItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                slowItemActionPerformed(evt);
            }
        });
        JMenuItem avgItem = new JMenuItem();
        avgItem.setText("500");
        speedMenu.add(avgItem);
        avgItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                avgItemActionPerformed(evt);
            }
        });
        JMenuItem fastItem = new JMenuItem();
        fastItem.setText("100");
        speedMenu.add(fastItem);
        fastItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fastItemActionPerformed(evt);
            }
        });
        JMenu helpMenu = new JMenu();
        helpMenu.setText("Help");
        GUIMenu.add(helpMenu);

        //Console on Righthand side
        JPanel jPanelCon = new JPanel();
        jPanelCon.setPreferredSize(new Dimension(255,690));
        JLabel header = new JLabel();
        header.setText("Output:");
        jPanelCon.add(header);
        output.setPreferredSize(new Dimension(250, 600));
        output.setEditable(false);
        output.setAutoscrolls(true);
        JScrollPane jScrollOut = new JScrollPane(output);
        jScrollOut.getVerticalScrollBar().setUnitIncrement(30);
        jPanelCon.add(jScrollOut);

        
        GUIFrame.setJMenuBar(GUIMenu);
        GUIFrame.add(jScrollCan);
        GUIFrame.getContentPane().add(jPanelCon, BorderLayout.EAST);
        


        Point[] dotPoints = new Point[3];
        dotPoints[0] = new Point(0,180);
        dotPoints[1] = new Point(0,200);
        dotPoints[2] = new Point(0,360);
        
        stringPoints = new Point[22];
        strings = new String[22];

        
        //Initialise string positions
        stringPoints[0] = new Point(10,145);
        stringPoints[1] = new Point(180,450);

        for(int i=2;i<stringPoints.length;i++){
            strings[i] = "";
            if(i<12){
                stringPoints[i] = new Point(10,162+(i-2)*16);
            }
            else{
                stringPoints[i] = new Point(180,310+(i-2)*16);
            }
        }
        strings[0] = "Server Console:";
        strings[1] = "Client Console:";

        canvas.setStringCoords(stringPoints);
        canvas.setStrings(strings);
        //canvas.setDotCoords(dotPoints);
        canvas.setBackgroundImage(new ImageIcon("background.jpg").getImage());
        
        //gui = new GUI();
        GUIFrame.setTitle("Fault Tolerant TCP");
        GUIFrame.setLocation(0, 0);
        GUIFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIFrame.setSize(850, 700);     
        GUIFrame.setVisible(true);
        
        isInit = true;
        
        /*while(true){
            //hclt2tcp();
            //hclt2srv();
            //hssw2tcp();
            //htcp2nsw();
            //hnsw2srv();
            hsrv2nsw();
            hnsw2tcp();
            htcp2ssw();
            hsrv2log();
            htcp2log();
           // hsrv2clt();
            //htcp2clt();
            /*srv2log();
            tcp2log();
            log2tcp();
            log2srv();
            srv2clt();
            tcp2clt();
        }*/
        
    }
    
    private void fastItemActionPerformed(java.awt.event.ActionEvent evt) {                                         
        wait = 100;
    }   
    
    private void avgItemActionPerformed(java.awt.event.ActionEvent evt) {                                         
        wait = 500;
    } 
    private void slowItemActionPerformed(java.awt.event.ActionEvent evt) {                                         
        wait = 1000;
    } 
    
    private void killServerActionPerformed(ActionEvent evt) {
        printToScreen("Killing Server...");
        printToScreen("...Not Really...Needs Implementing!");
    }
    
    private void restartServerActionPerformed(ActionEvent evt){
        
    }
    
    public void nsw2srv(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(294,82,(byte)50);
        dotPoints[1] = new imageMap(294,62,(byte)51);
        drawVector(dotPoints);
    }
    public void srv2nsw(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(331,62,(byte)53);
        dotPoints[1] = new imageMap(331,82,(byte)52);
        drawVector(dotPoints);
    }
    public void tcp2nsw(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(294,145,(byte)46);
        dotPoints[1] = new imageMap(294,125,(byte)47);
        drawVector(dotPoints);
    }
    public void nsw2tcp(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(331,125,(byte)49);
        dotPoints[1] = new imageMap(331,145,(byte)48);
        drawVector(dotPoints);
    }
    
    
    public void ssw2tcp(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(295,205,(byte)42);
        dotPoints[1] = new imageMap(295,185,(byte)43);
        drawVector(dotPoints);
    }
    public void tcp2ssw(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(330,185,(byte)45);
        dotPoints[1] = new imageMap(330,205,(byte)44);
        drawVector(dotPoints);
    }
    
    public void clt2tcp(){
        waitForInit();
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(65,485,(byte)0);
        dotPoints[1] = new imageMap(75,465,(byte)1);
        dotPoints[2] = new imageMap(85,442,(byte)2);
        drawVector(dotPoints);
    }
    
    public void log2tcp(){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(503,480,(byte)18);
        dotPoints[1] = new imageMap(492,460,(byte)19);
        dotPoints[2] = new imageMap(481,440,(byte)20);
        drawVector(dotPoints);
    }
    
    public void tcp2clt(){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(115,442,(byte)5);
        dotPoints[1] = new imageMap(106,462,(byte)4);
        dotPoints[2] = new imageMap(97,482,(byte)3);
        drawVector(dotPoints);
    }
    
    public void tcp2log(){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(451,440,(byte)23);
        dotPoints[1] = new imageMap(463,460,(byte)22);
        dotPoints[2] = new imageMap(473,480,(byte)21);
        drawVector(dotPoints);
    }
    public void clt2srv(){
        imageMap[] dotPoints = new imageMap[9];
        dotPoints[0] = new imageMap(100,400,(byte)6);
        dotPoints[1] = new imageMap(125,383,(byte)7);
        dotPoints[2] = new imageMap(150,366,(byte)8);
        dotPoints[3] = new imageMap(175,349,(byte)9);
        dotPoints[4] = new imageMap(200,332,(byte)10);
        dotPoints[5] = new imageMap(225,315,(byte)11);
        dotPoints[6] = new imageMap(295,300,(byte)36);
        dotPoints[7] = new imageMap(295,275,(byte)37);
        dotPoints[8] = new imageMap(295,250,(byte)38);
        drawVector(dotPoints);
    }
    public void srv2clt(){
        imageMap[] dotPoints = new imageMap[9];
        dotPoints[8] = new imageMap(134,400,(byte)12);
        dotPoints[7] = new imageMap(159,383,(byte)13);
        dotPoints[6] = new imageMap(184,366,(byte)14);
        dotPoints[5] = new imageMap(209,349,(byte)15);
        dotPoints[4] = new imageMap(234,332,(byte)16);
        dotPoints[3] = new imageMap(259,315,(byte)17);
        dotPoints[2] = new imageMap(330,300,(byte)39);
        dotPoints[1] = new imageMap(330,275,(byte)40);
        dotPoints[0] = new imageMap(330,250,(byte)41);
        drawVector(dotPoints);
    }
    
    public void log2srv(){
        imageMap[] dotPoints = new imageMap[9];
        dotPoints[0] = new imageMap(472,400,(byte)24);
        dotPoints[1] = new imageMap(459,384,(byte)25);
        dotPoints[2] = new imageMap(446,368,(byte)26);
        dotPoints[3] = new imageMap(433,352,(byte)27);
        dotPoints[4] = new imageMap(420,336,(byte)28);
        dotPoints[5] = new imageMap(405,318,(byte)29);
        dotPoints[6] = new imageMap(295,300,(byte)36);
        dotPoints[7] = new imageMap(295,275,(byte)37);
        dotPoints[8] = new imageMap(295,250,(byte)38);
        drawVector(dotPoints);
    }
    
    public void srv2log(){
        imageMap[] dotPoints = new imageMap[9];
        dotPoints[8] = new imageMap(442,400,(byte)30);
        dotPoints[7] = new imageMap(430,384,(byte)31);
        dotPoints[6] = new imageMap(418,368,(byte)32);
        dotPoints[5] = new imageMap(406,352,(byte)33);
        dotPoints[4] = new imageMap(394,336,(byte)34);
        dotPoints[3] = new imageMap(382,318,(byte)35);
        dotPoints[2] = new imageMap(330,300,(byte)39);
        dotPoints[1] = new imageMap(330,275,(byte)40);
        dotPoints[0] = new imageMap(330,250,(byte)41);
        drawVector(dotPoints);
    }
    

    public void hsrv2nsw(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(331,62,(byte)53);
        dotPoints[1] = new imageMap(331,82,(byte)52);
        drawHVector(dotPoints);
    }

    public void hnsw2tcp(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(331,125,(byte)49);
        dotPoints[1] = new imageMap(331,145,(byte)48);
        drawHVector(dotPoints);
    }
    

    public void htcp2ssw(){
        imageMap[] dotPoints = new imageMap[2];
        dotPoints[0] = new imageMap(330,185,(byte)45);
        dotPoints[1] = new imageMap(330,205,(byte)44);
        drawHVector(dotPoints);
    }
     
    public void htcp2log(){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(451,440,(byte)23);
        dotPoints[1] = new imageMap(463,460,(byte)22);
        dotPoints[2] = new imageMap(473,480,(byte)21);
        drawHVector(dotPoints);
    }
    
    public void hsrv2log(){
        imageMap[] dotPoints = new imageMap[9];
        dotPoints[8] = new imageMap(442,400,(byte)30);
        dotPoints[7] = new imageMap(430,384,(byte)31);
        dotPoints[6] = new imageMap(418,368,(byte)32);
        dotPoints[5] = new imageMap(406,352,(byte)33);
        dotPoints[4] = new imageMap(394,336,(byte)34);
        dotPoints[3] = new imageMap(382,318,(byte)35);
        dotPoints[2] = new imageMap(330,300,(byte)39);
        dotPoints[1] = new imageMap(330,275,(byte)40);
        dotPoints[0] = new imageMap(330,250,(byte)41);
        drawHVector(dotPoints);
    }
    
      private void drawHVector(imageMap[] img){
        for(int i=0;i<img.length;i++){
            canvas.setDot2Coord(img[i], img[i].getImageId());
            try {
                this.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            canvas.setDot2Coord(blankDot, img[i].getImageId());
        }
    }  
    
    private void drawVector(imageMap[] img){
        for(int i=0;i<img.length;i++){
            //printToServer("Printing " +i);
            //printToClient("Printing client " +i);
            canvas.setDotCoord(img[i], img[i].getImageId());
            try {
                this.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            canvas.setDotCoord(blankDot, img[i].getImageId());
            /*try {
                this.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }*/
        }
    }
    
    public void waitForInit(){
        try {
            while(!isInit){
                this.sleep(wait);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void printToServer(String str){
        waitForInit();
        if(servLines<10){
            strings[servLines+2] = str;
            servLines++;
        }
        else{
            //Shift all up one
            for(int i=3;i<12;i++){
                strings[i-1] = strings[i];
            }
            //Add new string at bottom
            strings[11] = str;
        }
        canvas.setStrings(strings);
    }
    
    public void printToClient(String str){
        waitForInit();
        if(cltLines<10){
            strings[cltLines+12] = str;
            cltLines++;
        }
        else{
            //Shift all up one
            for(int i=13;i<22;i++){
                strings[i-1] = strings[i];
            }
            //Add new string at bottom
            strings[21] = str;
        }
        canvas.setStrings(strings);
    }
    
    public void printToScreen(String str){
        waitForInit();
        output.append("\n" + str);
        lines++;
        if(lines > 36){
            Dimension oldSize = output.getPreferredSize();
            oldSize.setSize(oldSize.getWidth(),oldSize.getHeight()+17);
            output.setPreferredSize(oldSize);
            output.revalidate();
            output.setCaretPosition(output.getDocument().getLength());
        }
    }
}
