package com.figueroa.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Customized logger class.
 *
 * @author Gerardo Figueroa
 * Institute of Information Systems and Applications
 * National Tsing Hua University
 * Hsinchu, Taiwan
 * May 2013
 */
public class ExceptionLogger {

    public static enum DebugLevel {
        MORE_DETAIL, DETAIL, DEBUG, INFO, WARNING, ERROR
    }

    private final String logDirectory;
    private DebugLevel debugLevel;

    public ExceptionLogger(String logDir, DebugLevel debugLevel) {
        logDirectory = logDir;
        this.debugLevel = debugLevel;
    }
    
    /**
     * Get a DebugLevel enum type from a string
     * @param debugLevelString
     * @return a DebugLevel
     */
    public static DebugLevel getDebugLevelFromString(String debugLevelString) {
        for (DebugLevel debugLevel : DebugLevel.values()) {
            if (debugLevelString.equalsIgnoreCase(debugLevel.toString())) {
                return debugLevel;
            }
        }
        
        return null;
    }

    /**
     * Write a message to the log file.
     * @param message 
     */
    public void writeToLog(String message) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logDirectory, true));
            writer.write(now() + "\t" + message + "\n");
            writer.close();
        }
        catch (Exception e) {
            System.err.println("Exception in writeToLog: " + e.getMessage());
        }
    }

    /**
     * Print message on console. If debugLevel is ERROR, then the message is
     * written to the log file.
     * @param message
     * @param debugLevel 
     */
    public void debug(String message, DebugLevel debugLevel) {
        if (debugLevel.compareTo(this.debugLevel) >= 0) {
            String extraInfo = "";
            if (!message.isEmpty()) {
                extraInfo = "(" + debugLevel + ") ";
            }
            System.out.println(extraInfo + message);
        }

        if (debugLevel == DebugLevel.ERROR) {
            System.err.println(message);
            writeToLog(message);
        }
    }

    /**
     * Get a string representation of the current date and time.
     * Default format is yyyy-MM-dd HHmmss.
     * @return String with current datetime
     */
    public static String now() {
        String DATE_FORMAT_NOW = "yyyy-MM-dd HHmmss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public DebugLevel getDebugLevel() {
        return debugLevel;
    }

    public void setDebugLevel(DebugLevel debugLevel) {
        this.debugLevel = debugLevel;
    }
}