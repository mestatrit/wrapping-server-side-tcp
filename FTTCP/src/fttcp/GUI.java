/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author James Bossingham
 */
public class GUI extends Thread{
    private JTextArea output = new JTextArea();
    private JTextArea servCon = new JTextArea();
    private JTextArea cltCon = new JTextArea();
    private GUICanvas canvas;
    private int wait = 500;
    private int lines = 0;
    private int servLines = 0;
    private int cltLines = 0;
    private imageMap blankDot = new imageMap(0,0,(byte)-1);
    private JScrollPane jScrollCan;
    private Point[] stringPoints;
    private String[] strings;
    
        @Override
    public void run(){
        output.setText("");
        servCon.setText("");
        initGUI();   
    }
    
    private void initGUI(){
        
        canvas = new GUICanvas(new ImageIcon("dot2.png").getImage(),54);
        
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
        JMenu viewMenu = new JMenu();
        GUIMenu.add(editMenu);
        viewMenu.setText("View");
        GUIMenu.add(viewMenu);
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
       tcp2log();
        
    }
    
    public void clt2tcp(){
        waitForInit();
        while(true){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(65,485,(byte)0);
        dotPoints[1] = new imageMap(75,465,(byte)1);
        dotPoints[2] = new imageMap(85,442,(byte)2);
        drawVector(dotPoints);
        }
    }
    
    public void log2tcp(){
        while(true){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(503,480,(byte)18);
        dotPoints[1] = new imageMap(492,460,(byte)19);
        dotPoints[2] = new imageMap(481,440,(byte)20);
        drawVector(dotPoints);
        }
    }
    
    public void tcp2clt(){
        while(true){
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(115,442,(byte)5);
        dotPoints[1] = new imageMap(106,462,(byte)4);
        dotPoints[2] = new imageMap(97,482,(byte)3);
        drawVector(dotPoints);
        }
    }
    
    public void tcp2log(){
        while(true){
        
        imageMap[] dotPoints = new imageMap[3];
        dotPoints[0] = new imageMap(451,440,(byte)23);
        dotPoints[1] = new imageMap(463,460,(byte)22);
        dotPoints[2] = new imageMap(473,480,(byte)21);
        drawVector(dotPoints);
        }
    }
    
    private void drawVector(imageMap[] img){
        for(int i=0;i<img.length;i++){
            printToServer("Printing " +i);
            printToClient("Printing client " +i);
            canvas.setDotCoord(img[i], img[i].getImageId());
            try {
                this.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            canvas.setDotCoord(blankDot, img[i].getImageId());
            try {
                this.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void waitForInit(){
        try {
            this.sleep(wait);
        } catch (InterruptedException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void printToServer(String str){
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
        if(cltLines<10){
            strings[cltLines+12] = str;
            cltLines++;
        }
        else{
            //Shift all up one
            for(int i=12;i<22;i++){
                strings[i-1] = strings[i];
            }
            //Add new string at bottom
            strings[21] = str;
        }
        canvas.setStrings(strings);
    }
    
    public void printToScreen(String str){
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
