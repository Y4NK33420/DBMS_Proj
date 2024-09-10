package edu.upenn.cis.db.graphtrans;

import edu.upenn.cis.db.helper.Util;

/**
 * Console class to write messages to the console.
 * @author sbnet21
 *
 */
public class Console {
	private boolean enabled = true;
	
    public void setEnabled(boolean e) {
		enabled = e;
	}

	/**
     * Write prompt message.
     * @param msg message to write.
     */
    public void write(String msg) {
    	if (enabled == true) {
    		Util.Console.log(msg);
    	}
    }
	
}
