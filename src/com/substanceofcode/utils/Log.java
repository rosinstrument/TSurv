package com.substanceofcode.utils;

import com.sugree.utils.Loggable;
import java.util.Vector;

/**
 * Log
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class Log {

    private static int MAX_ENTRIES = 50;
    private static Loggable console = null;
    private static Vector log = new Vector();
    private static boolean log_off = true;

    public static boolean setLogOff(boolean newstate) {
        boolean ret = log_off;
        log_off = newstate;
        return ret;
    }

    public static void setConsole(Loggable console) {
        Log.console = console;
    }

    public static void setState(String text) {
        if (console != null) {
            console.setState(text);
        }
    }

    public static void setProgress(int value) {
        if (console != null) {
            console.setProgress(value);
        }
    }

    public static String getText() {
        String text = "";
        for (int i = 0; i < log.size(); i++) {
            text += (String) log.elementAt(i) + "\n";
        }
        return text;
    }

    public static void clear() {
        log.removeAllElements();
    }

    protected static void add(String entry) {
        System.out.println(entry);
        log.addElement(entry);
        if (log.size() > MAX_ENTRIES) {
            log.removeElementAt(0);
        }
    }

    public static void info(String entry) {
        if (!log_off) {
            add("INFO", entry);
        }
        if (console != null) {
            console.println(entry);
        }
    }

    public static void verbose(String entry) {
        if (log_off) {
            return;
        }
        add("LOG", entry);
    }

    public static void debug(String entry) {
        if (log_off) {
            return;
        }
        add("DBG", entry);
    }

    public static void error(String entry) {
        add("ERR", entry);
    }

    private static void add(String to, String entry) {
        if (entry == null) {
            entry = "null";
        }
        add(to + ": " + entry);
    }
}
