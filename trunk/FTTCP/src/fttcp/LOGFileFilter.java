/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fttcp;
import java.io.*;
/**
 *
 * @author James Bossingham
 */
class LOGFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".log");
    }
    
    public String getDescription() {
        return ".log files";
    }
}
