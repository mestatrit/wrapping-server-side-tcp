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
class NSWFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        boolean accept = name.toLowerCase().endsWith(".nsw") || name.toLowerCase().matches("received[.]log[.]srv[.]nsw.*");
        return accept;
    }
    
    public String getDescription() {
        return ".nsw files";
    }
}
