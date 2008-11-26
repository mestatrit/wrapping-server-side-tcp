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
    
    private imageMap[] Coords;
    
    private Image[] images;
    
    /** Hold Value of property background image*/
    private Image backgroundImage;
    
    private Image dot;
    
    /**Hold Value of property stretched*/
    private boolean stretched = true;
    
    /**Hold values of coordinates of points */
    
    private Point[] stringCoords;
    
    private imageMap[] dotCoords;
    
    private String[] strings;
    
    
    /**Construct an empty image viewer*/
    public GUICanvas(Image[] image, imageMap[] map, Image dot, int size){
        this.images = image;
        this.Coords = map;
        this.dot = dot;
        this.dotCoords = new imageMap[size];
        byte flag = -1;
        for (int i = 0; i<this.dotCoords.length;i++){
            this.dotCoords[i] = new imageMap(0,0,flag);
        }
    }
    
    /**Construct an empty image viewer*/
    public GUICanvas(Image dot, int size){
        this.dot = dot;
        this.dotCoords = new imageMap[size];
        byte flag = -1;
        for (int i = 0; i<this.dotCoords.length;i++){
            this.dotCoords[i] = new imageMap(0,0,flag);
        }
    }
 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(backgroundImage != null){
            if(isStretched()){
                g.drawImage(backgroundImage, 0, 0,getSize().width,getSize().height,this);
            }
            else{
                g.drawImage(backgroundImage, 0, 0,this);
            }
        }
        try{
            if(Coords != null && images != null){
                for(int i=0;i<Coords.length;i++){
                    g.drawImage(images[Coords[i].getImageId()-1], Coords[i].getXCoord(), Coords[i].getYCoord(), this);
                }            
            }
        }
        catch(Exception e){
            System.out.println("Invalid image index");
        }
        
        if(dotCoords != null){
            if(dot != null){
                for(int i=0;i<dotCoords.length;i++){
                    if(dotCoords[i].getImageId() != -1){
                        g.drawImage(dot, dotCoords[i].getXCoord(), dotCoords[i].getYCoord(), this);
                    }
                }
            }
        }
       if(stringCoords != null){
            
                for(int i=0;i<stringCoords.length;i++){
                    if(strings[i] != null){
                        g.drawString(strings[i], stringCoords[i].x, stringCoords[i].y);
                    }
                }
        }

    }
    
    public void setImages(Image[] img){
        images = img;
    }
    
    public Image[] getImages(){
        return images;
    }
    
    public void setCoords(imageMap[] map){
        Coords = map;
    }
    
    public imageMap[] getCoords(){
        return Coords;
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
    
    
    /**Set background Image*/
    public void setBackgroundImage(Image image){
        this.backgroundImage = image;
        repaint();
    }

    /**Get image coordinates*/
    public imageMap[] getDotCoords(){
        return dotCoords;
    }
    /**Set image coordinates*/
    public void setDotCoords(imageMap[] coords){
        this.dotCoords = coords;
        repaint();
    }
    
    /**Set image coordinates*/
    public void setDotCoord(imageMap coords,int index){
        this.dotCoords[index] = coords;
        repaint();
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
    
}

