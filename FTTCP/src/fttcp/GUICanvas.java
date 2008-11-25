/*
 * GUICanvas.java
 *
 * Created on November 25, 2008, 12:13 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package fttcp;
import java.awt.*;
import javax.swing.*;

/**
 *
 * @author csugcp
 */
public class GUICanvas extends JPanel{
    
/** Hold Value of property image*/
    private Image pc;
    
    /** Hold Value of property image*/
    private Image serv;
    
    private Image TCP;
    
    /** Hold Value of property background image*/
    private Image backgroundImage;
    
    private Image dot;
    
    /**Hold Value of property stretched*/
    private boolean stretched = true;
    
    /**Hold values of coordinates of points */
    private Point[] pcCoords;
    
    /**Hold values of coordinates of points */
    private Point[] servCoords;
    
    private Point[] tcpCoords;
    
    private Point[] stringCoords;
    
    private String[] strings;
    
    /**Hold value of property xCoordinate*/
    private int xCoordinate;
    
    /**Hold value of property yCoordinate*/
    private int yCoordinate;
    
    /**Construct an empty image viewer*/
    public GUICanvas(Image image){
        this.pc = image;
    }
    /**Construct an empty image viewer*/
    public GUICanvas(Image pc, Image serv, Image tcp, Image dot){
        this.pc = pc;
        this.serv = serv;
        this.TCP = tcp;
        this.dot = dot;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(backgroundImage != null){
            g.drawImage(backgroundImage, 0, 0,getSize().width,getSize().height,this);
        }
        /*else{
            if( != null){
                if(isStretched()){
                    g.drawImage(, xCoordinate, yCoordinate, getSize().width, getSize().height, this);

import java.awt.*;
import javax.swing.*;

public class GUICanvas {
   
}                }
                else{
                    g.drawImage(, xCoordinate, yCoordinate, this);
                }
            }
        }*/
        if(pcCoords != null){
            if(pc != null){
                for(int i=0;i<pcCoords.length;i++){
                    g.drawImage(pc, pcCoords[i].x, pcCoords[i].y, this);
                }
            }
        }
       if(servCoords != null){
            if(serv != null){
                for(int i=0;i<servCoords.length;i++){
                    g.drawImage(serv, servCoords[i].x, servCoords[i].y, this);
                }
            }
        }
        if(tcpCoords != null){
            if(TCP != null){
                for(int i=0;i<tcpCoords.length;i++){
                    g.drawImage(TCP, tcpCoords[i].x, tcpCoords[i].y, this);
                }
            }
        }
       if(stringCoords != null){
            
                for(int i=0;i<stringCoords.length;i++){
                    if(strings[i] != null){
                        g.drawImage(dot, stringCoords[i].x-1, stringCoords[i].y-10, this);
                        if(strings[i].length()>1){
                            for(int j =1;j<strings[i].length();j++){
                                g.drawImage(dot, (stringCoords[i].x-1)+j*9, (stringCoords[i].y-10), this);
                            }
                        }
                        g.drawString(strings[i], stringCoords[i].x, stringCoords[i].y);
                    }
                }
        }

    }
    /**Return value of property image*/
    public Image getPc(){
        return pc;
    }
    
    /**Set a new value for property image*/
    public void setPc(Image image){
        this.pc = image;
        repaint();
    }
    
    /**Return value of property stretched*/
    public boolean isStretched(){
        return stretched;
    }    

    /**Set a new value for property stretched*/
    public void setStretched(boolean stretched){
        this.stretched = stretched;
        repaint();
    }
    
    /**Return value of property xCoordinate*/
    public int getXCoordinate(){
        return xCoordinate;
    }
    
    /**Set a new value for property xCoordinate*/
    public void setXCoordinate(int xCoordinate){
        this.xCoordinate = xCoordinate;
        repaint();
    }
    
    /**Return value of property yCoordinate*/
    public int getYCoordinate(){
        return yCoordinate;
    }
    
    /**Set a new value for property yCoordinate*/
    public void setYCoordinate(int yCoordinate){
        this.yCoordinate = yCoordinate;
        repaint();
    }
    
    /**Set background Image*/
    public void setBackgroundImage(Image image){
        this.backgroundImage = image;
        repaint();
    }
    
    /**Set image coordinates*/
    public void setPcCoords(Point[] coords){
        this.pcCoords = coords;
        repaint();
    }
    
    /**Get image coordinates*/
    public Point[] getPcCoords(){
        return pcCoords;
    }
    /**Set image coordinates*/
    public void setTCPCoords(Point[] coords){
        this.tcpCoords = coords;
        repaint();
    }
    
    /**Get image coordinates*/
    public Point[] getTCPCoords(){
        return tcpCoords;
    }
    
    /**Set image coordinates*/
    public void setServCoords(Point[] coords){
        this.servCoords = coords;
        repaint();
    }
    /**Get image coordinates*/
    public Point[] getServCoords(){
        return servCoords;
    }
    /**Set String coordinates*/
    public void setStringCoords(Point[] coords){
        this.stringCoords = coords;
    }
    /**Get String coordinates*/
    public Point[] getStringCoords(){
        return stringCoords;
    }
    /**Set image coordinates*/
    public void setStrings(String[] stringArr){
        this.strings = stringArr;
        repaint();
    }
    /**Get image coordinates*/
    public String[] getStrings(){
        return strings;
    }
    /**Wipes tab*/
    public void clearTab(){
        Point[] Points = new Point[2];
        Point[] staffPoints = new Point[1];
        Points[0] = new Point(142,30);
        Points[1] = new Point(142,107);
        staffPoints[0] = new Point(142,54);
        Point[] stringPoints = new Point[1];
        String[] newStrings = new String[1];
        stringPoints[0] = stringCoords[0];
        newStrings[0] = strings[0];
        
        this.pcCoords = Points;
        this.servCoords = staffPoints;
        this.stringCoords = stringPoints;
        this.strings = newStrings;
    }
    
}

