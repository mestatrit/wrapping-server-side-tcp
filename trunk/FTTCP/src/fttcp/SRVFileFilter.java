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
class SRVFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        boolean accept = name.toLowerCase().endsWith(".srv") || name.toLowerCase().matches("received[.]nsw[.]srv[.][0-9]*");
        return accept;
    }
    
    public String getDescription() {
        return ".srv files";
    }
}
