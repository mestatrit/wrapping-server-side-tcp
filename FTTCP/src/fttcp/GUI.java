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
        output.setText("Output:");
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
        canvas.setPreferredSize(new Dimension(650,500));
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
        output.setPreferredSize(new Dimension(350, 500));
        //output.setText("Output:");
        output.setEditable(false);
        jPanelCorr.add(output);

        GUIFrame.setJMenuBar(GUIMenu);
        GUIFrame.add(jScrollTab);
        GUIFrame.getContentPane().add(jPanelCorr, BorderLayout.EAST);
        
        Point[] servPoints = new Point[2];
        Point[] pcPoints = new Point[1];
        Point[] tcpPoints = new Point[3];
       
        servPoints[0] = new Point(180,40);
        servPoints[1] = new Point(500,80);

        pcPoints[0] = new Point(300,400);

        tcpPoints[0] = new Point(180,180);
        tcpPoints[1] = new Point(500,200);
        tcpPoints[2] = new Point(300,360);
        
        canvas.setTCPCoords(tcpPoints);
        canvas.setPcCoords(pcPoints);
        canvas.setServCoords(servPoints);
        //addStaff();

        Point[] stringPoints = new Point[1];
        String[] strings = new String[1];

        stringPoints[0] = new Point(480,45);

        strings[0] = "";


        canvas.setStringCoords(stringPoints);
        canvas.setStrings(strings);

        //gui = new GUI();
        GUIFrame.setTitle("Fault Tolerant TCP");
        GUIFrame.setLocation(0, 0);
        GUIFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GUIFrame.setSize(1100, 700);     
        GUIFrame.setVisible(true);
        
    }
    
    public void printToScreen(String str){
        output.append("\n" + str);
    }
}
