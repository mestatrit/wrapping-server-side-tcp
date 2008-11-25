/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;

/**
 *
 * @author James Bossingham
 */
public class imageMap {
    private int xCoord;
    private int yCoord;
    private byte imageID;
    
    public imageMap(int x, int y, byte id){
        xCoord = x;
        yCoord = y;
        imageID = id;
    }
    
    public int getYCoord(){
        return yCoord;
    }
    
    public int getXCoord(){
        return xCoord;
    }
    
    public byte getImageId(){
        return imageID;
    }
    
    public void setYCoord(int y){
        yCoord = y;
    }
    
    public void setXCoord(int x){
        xCoord = x;
    }
    
    public void setImageId(byte id){
        imageID = id;
    }    
}
