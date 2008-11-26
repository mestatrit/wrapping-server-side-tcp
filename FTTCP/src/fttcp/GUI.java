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
    private GUICanvas canvas;
    private int wait = 500;
    private imageMap blankDot = new imageMap(0,0,(byte)-1);
    
        @Override
    public void run(){
        output.setText("Output:\n------------------------------------------------------------");
        initGUI();   
    }
    
    private void initGUI(){
        /*Image[] images = new Image[5];
        images[0] = new ImageIcon("comp.jpg").getImage(); 
        images[1] = new ImageIcon("serv.jpg").getImage(); 
        images[2] = new ImageIcon("TCPLayer.jpg").getImage();
        images[3] = new ImageIcon("SSW.jpg").getImage();
        images[4] = new ImageIcon("NSW.jpg").getImage();
        
        byte pc = 1;
        byte serv = 2;
        byte tcp = 3;
        byte ssw = 4;
        byte nsw = 5;
        
        imageMap[] Coords = new imageMap[8];
        Coords[0] = new imageMap(250,500,pc);
        Coords[1] = new imageMap(10,20,serv);
        Coords[2] = new imageMap(470,20,serv);
        Coords[3] = new imageMap(40,240,tcp);
        Coords[4] = new imageMap(450,170,tcp);
        Coords[5] = new imageMap(250,410,tcp);
        Coords[6] = new imageMap(50,310,ssw);
        Coords[7] = new imageMap(50,170,nsw);*/
        
        canvas = new GUICanvas(new ImageIcon("dot2.png").getImage(),54);
        
        JFrame GUIFrame = new JFrame();
        canvas.setStretched(false);
        canvas.setPreferredSize(new Dimension(550,500));
        JScrollPane jScrollTab = new JScrollPane(canvas);
        jScrollTab.getVerticalScrollBar().setUnitIncrement(30);

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

        JPanel jPanelCorr = new JPanel();
        output.setPreferredSize(new Dimension(250, 500));
        output.setEditable(false);
        jPanelCorr.add(output);

        GUIFrame.setJMenuBar(GUIMenu);
        GUIFrame.add(jScrollTab);
        GUIFrame.getContentPane().add(jPanelCorr, BorderLayout.EAST);
        


        Point[] dotPoints = new Point[3];
        dotPoints[0] = new Point(0,180);
        dotPoints[1] = new Point(0,200);
        dotPoints[2] = new Point(0,360);
        
        /*Point[] stringPoints = new Point[3];
        String[] strings = new String[3];

        stringPoints[0] = new Point(40,15);
        stringPoints[1] = new Point(500,15);
        stringPoints[2] = new Point(280,620);

        strings[0] = "Server";
        strings[1] = "Logger";
        strings[2] = "Client";

        canvas.setStringCoords(stringPoints);
        canvas.setStrings(strings);*/
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
    public void printToScreen(String str){
        output.append("\n" + str);
    }
}
