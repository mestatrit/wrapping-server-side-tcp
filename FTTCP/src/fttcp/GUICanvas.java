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
    private Image spacer;
    
    /** Hold Value of property image*/
    private Image staff;
    
    /** Hold Value of property background image*/
    private Image backgroundImage;
    
    private Image textBack;
    
    /**Hold Value of property stretched*/
    private boolean stretched = true;
    
    /**Hold values of coordinates of points */
    private Point[] spacerCoords;
    
    /**Hold values of coordinates of points */
    private Point[] staffCoords;
    
    private Point[] stringCoords;
    
    private String[] strings;
    
    /**Hold value of property xCoordinate*/
    private int xCoordinate;
    
    /**Hold value of property yCoordinate*/
    private int yCoordinate;
    
    /**Construct an empty image viewer*/
    public GUICanvas(Image image){
        this.spacer = image;
    }
    /**Construct an empty image viewer*/
    public GUICanvas(Image spacer, Image staff, Image textBack){
        this.spacer = spacer;
        this.staff = staff;
        this.textBack = textBack;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(backgroundImage != null){
            g.drawImage(backgroundImage, 0, 0,getSize().width,getSize().height,this);
        }
        /*else{
            if(spacer != null){
                if(isStretched()){
                    g.drawImage(spacer, xCoordinate, yCoordinate, getSize().width, getSize().height, this);

import java.awt.*;
import javax.swing.*;

public class GUICanvas {
   
}                }
                else{
                    g.drawImage(spacer, xCoordinate, yCoordinate, this);
                }
            }
        }*/
        if(spacerCoords != null){
            if(spacer != null){
                for(int i=0;i<spacerCoords.length;i++){
                    g.drawImage(spacer, spacerCoords[i].x, spacerCoords[i].y, this);
                }
            }
        }
       if(staffCoords != null){
            if(staff != null){
                for(int i=0;i<staffCoords.length;i++){
                    g.drawImage(staff, staffCoords[i].x, staffCoords[i].y, this);
                }
            }
        }
       if(stringCoords != null){
            
                for(int i=0;i<stringCoords.length;i++){
                    if(strings[i] != null){
                        g.drawImage(textBack, stringCoords[i].x-1, stringCoords[i].y-10, this);
                        if(strings[i].length()>1){
                            for(int j =1;j<strings[i].length();j++){
                                g.drawImage(textBack, (stringCoords[i].x-1)+j*9, (stringCoords[i].y-10), this);
                            }
                        }
                        g.drawString(strings[i], stringCoords[i].x, stringCoords[i].y);
                    }
                }
        }

    }
    /**Return value of property image*/
    public Image getSpacer(){
        return spacer;
    }
    
    /**Set a new value for property image*/
    public void setSpacer(Image image){
        this.spacer = image;
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
    public void setSpacerCoords(Point[] coords){
        this.spacerCoords = coords;
        repaint();
    }
    
    /**Get image coordinates*/
    public Point[] getSpacerCoords(){
        return spacerCoords;
    }
    
    /**Set image coordinates*/
    public void setStaffCoords(Point[] coords){
        this.staffCoords = coords;
        repaint();
    }
    /**Get image coordinates*/
    public Point[] getStaffCoords(){
        return staffCoords;
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
        Point[] spacerPoints = new Point[2];
        Point[] staffPoints = new Point[1];
        spacerPoints[0] = new Point(142,30);
        spacerPoints[1] = new Point(142,107);
        staffPoints[0] = new Point(142,54);
        Point[] stringPoints = new Point[1];
        String[] newStrings = new String[1];
        stringPoints[0] = stringCoords[0];
        newStrings[0] = strings[0];
        
        this.spacerCoords = spacerPoints;
        this.staffCoords = staffPoints;
        this.stringCoords = stringPoints;
        this.strings = newStrings;
    }
    
}

