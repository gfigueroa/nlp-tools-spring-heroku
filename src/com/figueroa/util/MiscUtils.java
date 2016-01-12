package com.figueroa.util;

import org.apache.commons.math.util.MathUtils;

/**
 * Miscellaneous utilities used in RankUp
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * October 2014
 */
public class MiscUtils {
    
    /**
     * Returns a '✓' if true or an 'x' if false
     * @param b
     * @return 
     */
    public static String getBooleanCheckString(Boolean b) {
        if (b == null) {
            return "-";
        }
        
        if (b) {
            return "\u2713";
        }
        else {
            return "x";
        }
    }
    
    /**
     * Add trailing 0s to a double to have a fixed number of characters  given
     * the desired precision and then convert to string
     * @param n
     * @param precision
     * @return String representation of a double with trailing 0s
     */
    public static String convertDoubleToFixedCharacterString(double n, int precision) {
        String string = String.valueOf(MathUtils.round(n, precision));
        
        if (string.contains("E")) {
            String eValue = string.substring(string.indexOf("E"));
            string = string.substring(0, string.indexOf(".") + precision + 1);
            string += eValue;
        }
        else {
            while (string.length() < 2 + precision) {
                string = string + "0";
            }
        }
        return string;
    }
    
}
