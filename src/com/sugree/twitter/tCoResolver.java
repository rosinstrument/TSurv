/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sugree.twitter;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class UrlTime {

    public String url;
    public long time;

    public UrlTime(String URL) {
        url = URL;
        time = new Date().getTime();
    }
}

/**
 *
 * @author mvlad
 */
public class tCoResolver {

    static Hashtable tco2url = new Hashtable();
    static Vector futureResolve = new Vector();
    private static int MAX_SIZE = 999;
    private static long MAX_CACHE_AGE_MS = 3600000 * 24; //10 hours
    private static String[] tcoStart = {"http://t.co/", "https://t.co/"};

    public static boolean isTCo(String chunk) {
        for (int i = 0; i < tcoStart.length; i++) {
            int stco = tcoStart[i].length();
            if (stco > chunk.length()) {
                continue;
            }
            if (tcoStart[i].equalsIgnoreCase(chunk.substring(0, stco))) {
                return true;
            }
        }
        return false;
    }

    public synchronized static String resolve(String chunk) {
        UrlTime r = (UrlTime) tco2url.get(chunk);
        if (r == null) {
            futureResolve.addElement(chunk);
            if (futureResolve.size() > MAX_SIZE) {
                futureResolve.removeElementAt(0);
            }
            return chunk;
        }
        if (tco2url.size() < MAX_SIZE) {
            return r.url;
        }
        Enumeration en = tco2url.keys();
        long now = new Date().getTime();
        while (en.hasMoreElements()) {
            String tco = (String) en.nextElement();
            UrlTime ut = (UrlTime) tco2url.get(tco);
            if (now - ut.time > MAX_CACHE_AGE_MS) {
                tco2url.remove(tco);
            }
        }
        return r.url;
    }

    public synchronized static void add(String tco, String expandedtco) {
        tco2url.put(tco, new UrlTime(expandedtco));
    }

    static void add4resolve(String tcourl) {
        /*
         if (!tco2url.containsKey(tcourl)) {
         synchronized (tCoResolver.class) {
         futureResolve.addElement(tcourl);
         }
         }
         int s = futureResolve.size();
         if (s > 9) {
         String req = "";
         for (int i = 0; i < s; i++) {
         String u = (String) futureResolve.elementAt(i);
         req += (req.length() == 0 ? "" : "&") + "urls%5B%5D=" + StringUtil.urlEncode(u);
         }
         synchronized (tCoResolver.class) {
         futureResolve.removeAllElements();
         }
         final String request = req;
         new Thread(new Runnable() {
         public void run() {
         Log.info(request);
         try {
         String response = HttpUtil.doGet("https://api.twitter.com/1/urls/resolve.json", request);
         Log.info(response);
         } catch (Exception ex) {
         Log.error(ex.toString());
         }

         }
         }).start();
         } */
    }
}
