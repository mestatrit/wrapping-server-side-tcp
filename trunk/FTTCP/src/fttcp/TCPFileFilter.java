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
class SSWFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".tcp");
    }
    
    public String getDescription() {
        return ".tcp files";
    }
}
