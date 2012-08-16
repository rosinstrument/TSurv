/*
 * HttpUtil.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.substanceofcode.utils;

import com.java4ever.apime.io.GZIP;
import com.sugree.infrastructure.Device;
import com.sugree.twitter.JSONTwitterParser;
import com.sugree.twitter.TwitterConsumer;
import com.sugree.twitter.TwitterController;
import com.sugree.utils.MultiPartFormOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.ContentConnection;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Image;

/**
 *
 * @author Tommi Laukkanen
 */
public class HttpUtil extends HttpAbstractUtil {

    /**
     * Total bytes transfered
     */
    private static long totalBytes = 0;
    private static String userAgent = "";
    private static boolean alternateAuthen = false;
    private static boolean optimizeBandwidth = true;
    private static boolean forceNoHost = false;
    private static boolean gzip = true;
    private static String contentType = "application/x-www-form-urlencoded";
    private static TwitterController controller = null;
    private static TwitterConsumer oauth = null;
    private static String oauthEchoUrl = null;
    private static String oauthEchoMethod = null;

    private HttpUtil() {
    }

    public static void setTwitterController(TwitterController controller) {
        HttpUtil.controller = controller;
    }

    public static void setOAuth(TwitterConsumer oauth) {
        HttpUtil.oauth = oauth;
    }

    public static void setOAuthEcho(String url, String method) {
        HttpUtil.oauthEchoUrl = url;
        HttpUtil.oauthEchoMethod = method;
    }

    public static void setUserAgent(String userAgent) {
        HttpUtil.userAgent = userAgent + " gzip";
    }

    public static void setAlternateAuthentication(boolean flag) {
        HttpUtil.alternateAuthen = flag;
    }

    public static void setOptimizeBandwidth(boolean flag) {
        HttpUtil.optimizeBandwidth = flag;
    }

    public static void setForceNoHost(boolean flag) {
        HttpUtil.forceNoHost = flag;
    }

    public static void setGzip(boolean flag) {
        HttpUtil.gzip = flag;
    }

    public static void setContentType(String contentType) {
        if (contentType == null) {
            HttpUtil.contentType = "application/x-www-form-urlencoded";
        } else {
            HttpUtil.contentType = contentType;
        }
    }

    public static String getLocation(String url, String query) throws IOException, Exception {
        if (query.length() > 0) {
            url += "?" + query;
        }
        int status;
        String message;
        HttpConnection con;
        final String platform = Device.getPlatform();
        try {
            con = (HttpConnection) Connector.open(url);
            Log.verbose("opened connection to " + url);
            con.setRequestMethod(HttpConnection.GET);
            setRequestProperties(con, query.getBytes(), null);
            status = con.getResponseCode();
            message = con.getResponseMessage();
            Log.verbose("response code " + status + " " + message);
            String newUrl = con.getHeaderField("Location");
            if (newUrl != null) {
                url = newUrl;
            }
        } catch (IOException ioe) {
            throw ioe;
        }
        return url;
    }

    public static String doPost(String url, String query) throws IOException, Exception {
        return doRequest(url, prepareQuery(query), HttpConnection.POST);
    }

    public static String doPost(String url, ByteArrayOutputStream data, MultiPartFormOutputStream out) throws IOException {
        boolean redirected = false;
        String platform = "unknown";
        try {
            platform = Device.getPlatform();
        } catch (Exception e) {
        }
        long timeOffset = new Date().getTime();
        boolean basicAuth = false;
        if (url.startsWith("basic:")) {
            url = url.substring(6);
            basicAuth = true;
            Log.info("requested basic auth for " + url);
        }
        HttpConnection con = (HttpConnection) Connector.open(url);
        Log.verbose("opened connection to " + url);
        con.setRequestMethod(HttpConnection.POST);
        byte[] query = data.toByteArray();
        int n = query.length;
        String body = new String(query);
        if (basicAuth || oauth == null) {
            if (username != null && password != null && username.length() > 0) {
                Log.info("Basic auth");
                Base64 b64 = new Base64();
                String userPass = username + ":" + password;
                userPass = b64.encode(userPass.getBytes());
                con.setRequestProperty("Authorization", "Basic " + userPass);
            } else {
                Log.info("No user/password set for basic auth");
            }
        } else if (oauth != null) {
            oauth.sign(con, "");
        } else {
            Log.info("No auth");
        }
        setRequestProperties(con, query, out.getContentType());
        OutputStream os = con.openOutputStream();
        Log.info("output size: " + n);
        Log.setState("sending request");
        for (int i = 0; i < n; i++) {
            os.write(query[i]);
            if (i % 500 == 0 || i == n - 1) {
                Log.setProgress(i * 100 / n / 2);
            }
        }
        Log.setProgress(50);
        os.close();
        os = null;
        int status = con.getResponseCode();
        String message = con.getResponseMessage();
        timeOffset = con.getDate() - timeOffset + new Date().getTime();
        Log.setState("received response");
        Log.info(status + " " + message);
        Log.verbose("response code " + status + " " + message);
        InputStream is = con.openInputStream();
        String response = getUpdates(con, is, os);
        Hashtable headers = getResponseHeaders(con);
        controller.lastPostHeaders = headers;
        controller.autoSetSnaphotsFrequency(false);
        Log.verbose("get response:" + response);
        int depth = 0;
        switch (status) {
            case HttpConnection.HTTP_OK:
            case HttpConnection.HTTP_NOT_MODIFIED:
            case HttpConnection.HTTP_BAD_REQUEST:
                break;
            case HttpConnection.HTTP_MOVED_TEMP:
            case HttpConnection.HTTP_TEMP_REDIRECT:
            case HttpConnection.HTTP_MOVED_PERM:
                if (depth > 2) {
                    throw new IOException("Too many redirect");
                }
                redirected = true;
                url = con.getHeaderField("location");
                Log.verbose("redirected to " + url);
                con.close();
                con = null;
                Log.verbose("closed connection");
                depth++;
                break;
            case 100:
                throw new IOException("unexpected 100 Continue");
            default:
                con.close();
                con = null;
                Log.verbose("closed connection");
                throw new IOException("Response status not OK:" + status + " " + message + " " + response);
        }
        Log.setState("receiving data");
        if (redirected) {
            try {
                if (con != null) {
                    con.close();
                    Log.verbose("closed connection");
                }
                if (os != null) {
                    os.close();
                    Log.verbose("closed output stream");
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        if (status == HttpConnection.HTTP_BAD_REQUEST) {
            Log.error(response);
            String resp = "";
            try {
                resp = JSONTwitterParser.parse400(response);
            } catch (Exception e) {
            }
            throw new IOException("Response status not OK:" + status + " " + message + " " + resp);
        }
        controller.setServerTimeOffset(new Date().getTime() - timeOffset);
        return response;
    }

    public static String doPost(String url, byte[] query) throws IOException, Exception {
        return doRequest(url, query, HttpConnection.POST);
    }

    public static String doGet(String url, String query) throws IOException, Exception {
        String fullUrl = url;
        query = prepareQuery(query);
        if (query.length() > 0) {
            fullUrl += "?" + query;
        }
        return doRequest(fullUrl, "", HttpConnection.GET);
    }

    public static String doRequest(String url, String query, String requestMethod) throws IOException, Exception {
        Log.debug(requestMethod + " " + url + ": " + query);
        return doRequest(url, query.getBytes(), requestMethod);
    }

    private static void setRequestProperties(HttpConnection con, byte[] query, String cType) throws IOException {
        final String platform = Device.getPlatform();
        con.setRequestProperty("User-Agent", HttpUtil.userAgent);
        if (!platform.equals(Device.PLATFORM_NOKIA)
                && !platform.equals(Device.PLATFORM_WELLCOM)
                && !forceNoHost) {
            String host = con.getHost();
            if (con.getPort() != 80) {
                host += ":" + con.getPort();
            }
            con.setRequestProperty("Host", host);
        }
        con.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        if (gzip) {
            con.setRequestProperty("Accept-Encoding", "gzip");
        }
        if (cType != null) {
            con.setRequestProperty("Content-Type", cType);
        }
        if (query != null && query.length > 0) {
            con.setRequestProperty("Content-Length", "" + query.length);
        }
    }

    public static String doRequest(String url, byte[] query, String requestMethod) throws IOException, Exception {
        long timeOffset = new Date().getTime();
        boolean badStatus = false;
        Log.setState("connecting");
        boolean basicAuth = false;
        if (url.startsWith("basic:")) {
            url = url.substring(6);
            basicAuth = true;
            Log.info("requested basic auth for " + url);
        }
        HttpConnection con = (HttpConnection) Connector.open(url);
        Log.setState("connected");
        Log.verbose("opened connection to " + url);
        con.setRequestMethod(requestMethod);
        String body = "";
        if ("application/x-www-form-urlencoded".equals(contentType)) {
            body = new String(query, "ISO-8859-1");
        }
        if (basicAuth || oauth == null || !oauth.sign(con, oauthEchoUrl, oauthEchoMethod, body)) {
            if (!alternateAuthen && username != null && password != null && username.length() > 0) {
                Log.info("Basic authorization");
                Base64 b64 = new Base64();
                String userPass = username + ":" + password;
                userPass = b64.encode(userPass.getBytes());
                con.setRequestProperty("Authorization", "Basic " + userPass);
            }
        }
        setRequestProperties(con, query, contentType);
        if (url.indexOf("resolve.json") >= 0) {
            con.setRequestProperty("Referer", "https://api.twitter.com/receiver.html");
        }
        InputStream is;
        OutputStream os = null;
        if (query.length > 0) {
            os = con.openOutputStream();
            Log.verbose("opened output stream");
            int n = query.length;
            Log.info("Size: " + n);
            for (int i = 0; i < n; i++) {
                os.write(query[i]);
                if (i % 500 == 0 || i == n - 1) {
                    Log.setProgress(i * 100 / n / 2);
                }
            }
            os.close();
            os = null;
            Log.verbose("closed output stream");
        }
        Log.setProgress(50);
        Log.setState("sending request");
        int status = con.getResponseCode();
        String message = con.getResponseMessage();
        timeOffset = con.getDate() - timeOffset + new Date().getTime();
        Log.setState("received response");
        Log.info(status + " " + message);
        Log.verbose("response code " + status + " " + message);
        int depth = 0;
        boolean redirected = false;
        switch (status) {
            case HttpConnection.HTTP_OK:
            case HttpConnection.HTTP_NOT_MODIFIED:
                break;
            case HttpConnection.HTTP_MOVED_TEMP:
            case HttpConnection.HTTP_TEMP_REDIRECT:
            case HttpConnection.HTTP_MOVED_PERM:
                if (depth > 2) {
                    throw new IOException("Too many redirect");
                }
                redirected = true;
                url = con.getHeaderField("location");
                Log.verbose("redirected to " + url);
                con.close();
                con = null;
                Log.verbose("closed connection");
                depth++;
                break;
            case 100:
                throw new IOException("unexpected 100 Continue");
            case HttpConnection.HTTP_BAD_REQUEST:
            default:
                badStatus = true;
        }
        is = con.openInputStream();
        Log.setState("receiving data");
        String response = "";
        if (!redirected) {
            response = getUpdates(con, is, os);
        } else {
            if (con != null) {
                con.close();
                Log.verbose("closed connection");
            }
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
        if (badStatus) {
            Log.error(response);
            Vector m = JSONTwitterParser.parseTwiErrorsResponse(response);
            String parsed = "";
            for (int i = 0; i < m.size(); i++) {
                parsed += (parsed.length() == 0 ? "" : ", ") + (String) m.elementAt(i);
            }
            throw new IOException("Bad response:" + status + ", " + parsed);
        }
        controller.setServerTimeOffset(new Date().getTime() - timeOffset);
        return response;
    }

    private static Hashtable getResponseHeaders(HttpConnection con) {
        Hashtable h = new Hashtable();
        for (int i = 0; i < 99; i++) {
            try {
                String key = con.getHeaderFieldKey(i);
                if (key == null) {
                    break;
                }
                h.put(key, con.getHeaderField(key));
            } catch (IOException ioe) {
            }
        }
        return h;
    }

    private static String getUpdates(HttpConnection con, InputStream is, OutputStream os) throws IOException {
        Log.setProgress(0);
        StringBuffer stb = new StringBuffer();
        try {
            int n = (int) con.getLength(), ch;
            if (n >= 0) {
                Log.info("Size: " + n);
                for (int i = 0; i < n; i++) {
                    if ((ch = is.read()) != -1) {
                        stb.append((char) ch);
                    }
                    if (i % 100 == 0 || i == n - 1) {
                        Log.setProgress(50 + i * 100 / n / 2);
                    }
                }
            } else {
                while ((ch = is.read()) != -1) {
                    stb.append((char) ch);
                }
            }
            if ("gzip".equalsIgnoreCase(con.getEncoding())) {
                Log.verbose("encoding: gzip");
                byte[] decompressed = GZIP.inflate(stb.toString().getBytes("ISO-8859-1"));
                stb = new StringBuffer(new String(decompressed, "ISO-8859-1"));
                Log.verbose("size: " + decompressed.length + " " + stb.length());
            }
            Log.setProgress(100);
        } catch (UnsupportedEncodingException e) {
            Log.verbose("read response: unknown encoding");
        } catch (IOException ioe) {
            Log.verbose("read response: " + ioe.getMessage());
            throw ioe;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
                if (con != null) {
                    con.close();
                    Log.verbose("closed connection");
                }
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        return stb.toString();
    }

    private static String prepareQuery(String query) {
        if (alternateAuthen && username != null && password != null && username.length() > 0) {
            Base64 b64 = new Base64();
            String userPass = username + ":" + password;
            userPass = b64.encode(userPass.getBytes());
            if (query.length() > 0) {
                query += "&";
            }
            query += "__token__=" + StringUtil.urlEncode(userPass);
        }
        return query;
    }

    public static Image getImage(String url, int maxsize) throws IOException {
        byte[] imageData = getImageBytes(url, maxsize);
        if (imageData != null) {
            Image im = Image.createImage(imageData, 0, imageData.length);
            Log.info(imageData.length + " bytes");
            return im;
        }
        return null;
    }

    public static byte[] getImageBytes(String url, int maxsize) throws IOException {
        ByteArrayOutputStream bStrm;
        saveImageToStream(url, maxsize, bStrm = new ByteArrayOutputStream());
        return bStrm.toByteArray();
    }

    public static int saveImageToStream(String url, int maxsize, OutputStream bStrm) throws IOException {
        Log.info(url);
        ContentConnection connection = (ContentConnection) Connector.open(url);
        DataInputStream iStrm = connection.openDataInputStream();
        final String largeResMsg = "too big network file";
        Log.setState("fetching data..");
        int count = 0;
        try {
            int length = (int) connection.getLength();
            if (maxsize > 0 && length > 0 && length > maxsize) {
                throw new IOException(largeResMsg + " (" + length + "/" + maxsize + ")");
            }
            if (length > 0) {
                Log.info("Size: " + length);
            Log.setProgress(100);
            }
            int ch;
            Log.setProgress(0);
            while ((ch = iStrm.read()) != -1) {
                bStrm.write(ch);
                if (maxsize > 0 && count++ > maxsize) {
                    throw new IOException(largeResMsg + " (" + length + "/" + maxsize + ")");
                }
                if (count % 512 == 0) {
                    Log.setProgress(count * 100 / length);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ie) {
                    }
                }
            }
            Log.setProgress(100);
        } catch (Exception e) {
            Log.error(e.toString());
            throw new IOException(e.toString());
        } catch (Error er) {
            Log.error(er.toString());
            throw new IOException(er.toString());
        } finally {
            if (iStrm != null) {
                iStrm.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (bStrm != null) {
                bStrm.close();
            }
        }
        return count;
    }

    public static String doFilePost(String url, String status, String fileName, String mimeType) throws IOException {
        boolean redirected = false;
        String platform = "unknown";
        try {
            platform = Device.getPlatform();
        } catch (Exception e) {
        }
        long timeOffset = new Date().getTime();
        boolean basicAuth = false;
        if (url.startsWith("basic:")) {
            url = url.substring(6);
            basicAuth = true;
            Log.info("requested basic auth");
        }
        HttpConnection con = (HttpConnection) Connector.open(url);
        Log.verbose("opened connection");
        con.setRequestMethod(HttpConnection.POST);
        if (basicAuth || oauth == null) {
            if (username != null && password != null && username.length() > 0) {
                Base64 b64 = new Base64();
                String userPass = username + ":" + password;
                userPass = b64.encode(userPass.getBytes());
                con.setRequestProperty("Authorization", "Basic " + userPass);
            } else {
                Log.info("No user/password set for basic auth");
            }
        } else if (oauth != null) {
            oauth.sign(con, "");
        } else {
            Log.info("No auth");
        }
        String boundary = MultiPartFormOutputStream.createBoundary();
        setRequestProperties(con, null, MultiPartFormOutputStream.getContentType(boundary));
        Log.setState("sending request");
        OutputStream os;
        MultiPartFormOutputStream mpos =
                new MultiPartFormOutputStream(os = con.openOutputStream(), boundary);
        mpos.writeField("status", status);
        try {
            mpos.writeFile("media[]", fileName, mimeType);
        } catch (SecurityException se) {
            String msg = fileName + ": " + se.getMessage();
            throw new IOException(msg);
        }
        mpos.close();
        Log.setProgress(50);
        int rcode = con.getResponseCode();
        String message = con.getResponseMessage();
        timeOffset = con.getDate() - timeOffset + new Date().getTime();
        Log.setState("received response");
        Log.info(rcode + " " + message);
        Log.verbose("response code " + rcode + " " + message);
        InputStream is = con.openInputStream();
        String response = getUpdates(con, is, os);
        Hashtable headers = getResponseHeaders(con);
        controller.lastPostHeaders = headers;
        controller.autoSetSnaphotsFrequency(false);
        int depth = 0;
        switch (rcode) {
            case HttpConnection.HTTP_OK:
            case HttpConnection.HTTP_NOT_MODIFIED:
            case HttpConnection.HTTP_BAD_REQUEST:
                break;
            case HttpConnection.HTTP_MOVED_TEMP:
            case HttpConnection.HTTP_TEMP_REDIRECT:
            case HttpConnection.HTTP_MOVED_PERM:
                if (depth > 2) {
                    throw new IOException("Too many redirect");
                }
                redirected = true;
                url = con.getHeaderField("location");
                Log.verbose("redirected to " + url);
                con.close();
                con = null;
                depth++;
                break;
            case 100:
                throw new IOException("unexpected 100 Continue");
            default:
                con.close();
                con = null;
                throw new IOException("Response: " + rcode + " " + message);
        }
        if (redirected) {
            try {
                if (con != null) {
                    con.close();
                }
                if (os != null) {
                    os.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
            }
        }
        Log.setState("finished");
        if (rcode == HttpConnection.HTTP_BAD_REQUEST) {
            Log.error(response);
            String resp = "";
            try {
                resp = JSONTwitterParser.parse400(response);
            } catch (Exception e) {
            }
            throw new IOException("Response: " + rcode + " " + message + " " + resp);
        }
        controller.setServerTimeOffset(new Date().getTime() - timeOffset);
        return response;
    }
}
