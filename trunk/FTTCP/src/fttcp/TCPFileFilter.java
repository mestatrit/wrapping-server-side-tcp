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
class TCPFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        boolean accept = name.toLowerCase().endsWith(".tcp") || name.toLowerCase().matches("received[.]log[.]srv[.]tcp.*");
        return accept;
    }
    
    public String getDescription() {
        return ".tcp files";
    }
}
