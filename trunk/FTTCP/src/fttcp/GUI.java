/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author James Bossingham
 */
public class GUI extends Thread{
    private JTextArea output = new JTextArea();;
    
        @Override
    public void run(){
        output.setText("Output:\n------------------------------------------------------------");
        initGUI();   
    }
    
    private void initGUI(){
        GUICanvas canvas = new GUICanvas(
                new ImageIcon("comp.jpg").getImage(), 
                new ImageIcon("serv.jpg").getImage(), 
                new ImageIcon("TCPLayer.jpg").getImage(),
                new ImageIcon("dot.png").getImage());
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
        
        Point[] servPoints = new Point[2];
        Point[] pcPoints = new Point[1];
        Point[] tcpPoints = new Point[3];
       
        //Server Location
        servPoints[0] = new Point(10,20);
        //Logger Location
        servPoints[1] = new Point(470,20);

        //Client Location
        pcPoints[0] = new Point(250,500);

        tcpPoints[0] = new Point(10,170);
        tcpPoints[1] = new Point(470,170);
        tcpPoints[2] = new Point(250,410);
        
        //addStaff();

        Point[] dotPoints = new Point[3];
        dotPoints[0] = new Point(0,180);
        dotPoints[1] = new Point(0,200);
        dotPoints[2] = new Point(0,360);
        
        Point[] stringPoints = new Point[3];
        String[] strings = new String[3];

        stringPoints[0] = new Point(40,15);
        stringPoints[1] = new Point(500,15);
        stringPoints[2] = new Point(280,620);

        strings[0] = "Server";
        strings[1] = "Logger";
        strings[2] = "Client";

        canvas.setTCPCoords(tcpPoints);
        canvas.setPcCoords(pcPoints);
        canvas.setServCoords(servPoints);
        canvas.setStringCoords(stringPoints);
        canvas.setStrings(strings);
        canvas.setDotCoords(dotPoints);
        
        //gui = new GUI();
        GUIFrame.setTitle("Fault Tolerant TCP");
        GUIFrame.setLocation(0, 0);
        GUIFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIFrame.setSize(850, 700);     
        GUIFrame.setVisible(true);
        
    }
    
    public void printToScreen(String str){
        output.append("\n" + str);
    }
}
