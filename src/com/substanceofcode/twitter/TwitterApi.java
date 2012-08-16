/*
 * TwitterApi.java
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
package com.substanceofcode.twitter;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import com.sugree.twitter.JSONTwitterParser;
import com.sugree.twitter.TwitterConsumer;
import com.sugree.twitter.TwitterException;
import com.sugree.utils.MultiPartFormOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import org.json.me.JSONObject;

/**
 * TwitterApi
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class TwitterApi {

    private String gateway;
    private String pictureGateway;
    private String source;
    private int count;
    private String username;
    private String password;
    public static final String DEFAULT_GATEWAY = "http://twitter.com/";
    public static final String DEFAULT_PICTURE_GATEWAY = "https://upload.twitter.com/1/statuses/update_with_media.json";
    private static final String PUBLIC_TIMELINE_URL = "statuses/public_timeline.json";
    private static final String HOME_TIMELINE_URL = "statuses/home_timeline.json";
    private static final String USER_TIMELINE_URL = "statuses/user_timeline.json";
    private static final String MENTIONS_TIMELINE_URL = "statuses/mentions.json";
    private static final String STATUS_UPDATE_URL = "statuses/update.json";
    private static final String DIRECT_MESSAGES_URL = "direct_messages.json";
    private static final String FAVORITES_URL = "favorites.json";
    private static final String FAVORITES_CREATE_URL = "favorites/create/%d.json";
    private static final String FAVORITES_DESTROY_URL = "favorites/destroy/%d.json";
    private static final String TEST_URL = "help/test.json";
    private static final String SCHEDULE_DOWNTIME_URL = "help/schedule_downtime.json";
    private static final String PICTURE_POST_URL = "";
    private static final String GLM_CELL_URL = "glm/cell";
    private static final String OAUTH_ECHO_JSON = "https://api.twitter.com/1/account/verify_credentials.json";
    private static final String OAUTH_ECHO_XML = "https://api.twitter.com/1/account/verify_credentials.xml";

    /**
     * Creates a new instance of TwitterApi
     */
    public TwitterApi(String source) {
        this.source = source;
        this.gateway = "http://twitter.com/";
        this.pictureGateway = "https://upload.twitter.com/1/statuses/update_with_media.json";
        this.count = 0;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setPictureGateway(String pictureGateway) {
        this.pictureGateway = pictureGateway;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setOAuth(TwitterConsumer oauth) {
        HttpUtil.setOAuth(oauth);
    }

    public void setAlternateAuthentication(boolean flag) {
        HttpUtil.setAlternateAuthentication(flag);
    }

    public void setOptimizeBandwidth(boolean flag) {
        HttpUtil.setOptimizeBandwidth(flag);
    }

    public void setForceNoHost(boolean flag) {
        HttpUtil.setForceNoHost(flag);
    }

    public void setGzip(boolean flag) {
        HttpUtil.setGzip(flag);
    }

    /**
     * Request public timeline from Twitter API.
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestPublicTimeline(String sinceId) throws TwitterException {
        HttpUtil.setBasicAuthentication("", "");
        return requestTimeline(gateway + PUBLIC_TIMELINE_URL, prepareParamSinceId(sinceId));
    }

    public Vector requestSearchTimeline(String sinceId, String q) throws TwitterException {
        if (q != null) {
            if (q.startsWith("@")) {
                return requestUserTimeline(sinceId, q.substring(1));
            }
            q = "q=" + StringUtil.urlEncode(q);
            String sm = since_or_max(sinceId);
            if (sm != null && sm.length() != 0) {
                q += "&" + sm;
            }
        }
        HttpUtil.setBasicAuthentication("", "");
        return requestSearch("http://search.twitter.com/search.json", prepareParamSearch(q));
    }

    public int followName(String screen_name) throws TwitterException {
        if (screen_name != null) {
            String q = "screen_name=" + StringUtil.urlEncode(screen_name);
            HttpUtil.setBasicAuthentication("", "");
            return requestFollow(gateway + "friendships/create.json", prepareParamCount(q));
        }
        return 0;
    }

    public int unFollowName(String screen_name) throws TwitterException {
        if (screen_name != null) {
            String q = "screen_name=" + StringUtil.urlEncode(screen_name);
            HttpUtil.setBasicAuthentication("", "");
            return requestFollow(gateway + "friendships/destroy.json", prepareParamCount(q));
        }
        return 0;
    }

    /**
     * Request home timeline from Twitter API.
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestHomeTimeline(String sinceId) throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway + HOME_TIMELINE_URL, prepareParamSinceId(sinceId));
    }

    /**
     * Request public timeline from Twitter API.
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestUserTimeline(String sinceId) throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway + USER_TIMELINE_URL, prepareParamSinceId(sinceId));
    }

    public Vector requestUserTimeline(String sinceId, String screenName) throws TwitterException {
        screenName = StringUtil.toAlphaNum(screenName);
        String par = "screen_name=" + StringUtil.urlEncode(screenName);
        if (sinceId != null && sinceId.length() > 0) {
            par += "&" + since_or_max(sinceId);
        }
        HttpUtil.setBasicAuthentication("", "");
        return requestTimeline(gateway + USER_TIMELINE_URL, prepareParamCount(par));
    }

    /**
     * Request responses timeline from Twitter API.{
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestMentionsTimeline(String sinceId) throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway + MENTIONS_TIMELINE_URL, prepareParamSinceId(sinceId));
    }

    /**
     * Request direct messages timeline from Twitter API.{
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestDirectMessagesTimeline(String sinceId) throws TwitterException {

        HttpUtil.setBasicAuthentication(username, password);
        Vector entries = new Vector();
        try {
            String response = HttpUtil.doGet(gateway + DIRECT_MESSAGES_URL, prepareParamSinceId(sinceId));
            if (response.length() > 0) {
                entries = JSONTwitterParser.parseDirectMessages(response);
            }
        } catch (IOException ex) {
            throw new TwitterException("request " + ex);
        } catch (Exception ex) {
            throw new TwitterException("request " + ex);
        }
        return entries;
    }

    /**
     * Request favorites timeline from Twitter API.{
     *
     * @return Vector containing StatusEntry items.
     */
    public Vector requestFavoritesTimeline() throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway + FAVORITES_URL, prepareParamCount(""));
    }

    public Status createFavorite(String id) throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway + FAVORITES_CREATE_URL, id);
    }

    public Status destroyFavorite(String id) throws TwitterException {
        HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway + FAVORITES_DESTROY_URL, id);
    }

    public Status updateStatus(String status) throws TwitterException {
        return updateStatus(status, 0);
    }

    public Status updateStatus(String status, long replyTo) throws TwitterException {
        String response = "";
        try {
            String query = "status=" + StringUtil.urlEncode(status);
            if (replyTo != 0) {
                query += "&in_reply_to_status_id=" + replyTo;
            }
            HttpUtil.setBasicAuthentication(username, password);
            response = HttpUtil.doPost(gateway + STATUS_UPDATE_URL, prepareParam(query));
        } catch (Exception ex) {
            Log.error("Error while updating status: " + ex.getMessage());
            throw new TwitterException("update " + ex.toString());
        }
        return null;
        //return JSONTwitterParser.parseStatus(response);
    }

    public void postPicture(String status, byte[] picture, String mimeType, String filen) throws TwitterException {
        String fileName = "TSurv";
        if (mimeType.indexOf("jpeg") >= 0 || mimeType.indexOf("jpg") >= 0) {
            fileName += ".jpg";
        } else if (mimeType.indexOf("png") >= 0) {
            fileName += ".png";
        } else if (mimeType.indexOf("gif") >= 0) {
            fileName += ".gif";
        }
        if (filen != null) {
            mimeType = "application/octet-stream";
            fileName = filen;
        }
        byte[] bs = null;
        try {
            bs = status.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        try {
            String url = postTwitPic(mimeType, fileName, picture, bs);
            if (url != null && !(url.length() == 0)) {
                updateStatus(status + " " + url, 0);
            }
        } catch (Exception ex) {
            HttpUtil.setOAuthEcho(null, null);
            HttpUtil.setContentType(null);
            Log.error("error while posting picture: " + ex.getMessage());
            throw new TwitterException("post " + ex.toString());
        }
        HttpUtil.setOAuthEcho(null, null);
        HttpUtil.setContentType(null);
    }

    public void postPicture(String status, String attachFileName, String mimeType) throws TwitterException {
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        try {
            String url = postTwitPic(status, attachFileName, mimeType);
            if (url != null && !(url.length() == 0)) {
                updateStatus(status + " " + url, 0);
            } else {
               throw new TwitterException("no 'media_url' returned by server");
         }
        } catch (Exception ex) {
            throw new TwitterException("post " + ex.toString());
        }
    }

    protected String postTwitPic(String mimeType, String fileName, byte[] picture, byte[] message) throws Exception {
        String boundary = MultiPartFormOutputStream.createBoundary();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        MultiPartFormOutputStream out = new MultiPartFormOutputStream(data, boundary);
        out.writeField("status", message);
        out.writeFile("media[]", mimeType, fileName, picture);
        out.close();
        HttpUtil.setContentType(null);
        HttpUtil.setOAuthEcho(null, null);
        Log.verbose("doing post for " + pictureGateway);
        HttpUtil.setBasicAuthentication(username, password);
        String response = HttpUtil.doPost(pictureGateway, data, out);
        String url = null;
        try {
            JSONObject jo = new JSONObject(response);
            if (jo != null) {
                url = jo.getString("media_url");
            }
        } catch (Exception e) {
        }
        return url;
    }

    public String postTwitPic(String status, String fileName, String mimeType) throws Exception {
        Log.verbose("posting to " + pictureGateway);
        HttpUtil.setBasicAuthentication(username, password);
        String response = HttpUtil.doFilePost(pictureGateway, status, fileName, mimeType);
        String url = null;
        try {
            JSONObject jo = new JSONObject(response);
            if (jo != null) {
                url = jo.getString("media_url");
            }
        } catch (Exception e) {
        }
        return url;
    }

    public String requestLocationByCellID(int cid, int lac) throws IOException, Exception {
        HttpUtil.setBasicAuthentication("", "");
        return HttpUtil.doGet(gateway + GLM_CELL_URL + "/" + cid + "/" + lac, "");
    }

    private Status requestObject(String url, String id) throws TwitterException {
        Status status = null;
        try {
            url = StringUtil.replace(url, "%d", id);
            HttpUtil.setBasicAuthentication(username, password);
            status = JSONTwitterParser.parseStatus(HttpUtil.doPost(url, ""));
        } catch (Exception ex) {
            throw new TwitterException(ex.getMessage());
        }
        return status;
    }

    private Vector requestTimeline(String timelineUrl, String param) throws TwitterException {
        Vector entries = new Vector();
        try {
            String response = HttpUtil.doGet(timelineUrl, param);
            if (response.length() > 0) {
                entries = JSONTwitterParser.parseStatuses(response);
            }
        } catch (IOException ex) {
            throw new TwitterException("request " + ex);
        } catch (Exception ex) {
            throw new TwitterException("request " + ex);
        }
        return entries;
    }

    private Vector requestSearch(String timelineUrl, String param) throws TwitterException {
        Vector entries = new Vector();
        try {
            String response = HttpUtil.doGet(timelineUrl, param);
            if (response.length() > 0) {
                entries = JSONTwitterParser.parseSearch(response);
            }
        } catch (IOException ex) {
            throw new TwitterException("request " + ex);
        } catch (Exception ex) {
            throw new TwitterException("request " + ex);
        }
        return entries;
    }

    private String prepareParam(String param) {
        String newParam = "";
        if (param.length() > 0) {
            newParam = param + "&";
        }
        newParam += "include_entities=1&include_rts=0";
        return newParam;
    }

    private String prepareParamCount(String param) {
        String newParam = prepareParam(param);
        if (count > 0) {
            if (newParam.length() > 0) {
                newParam += "&";
            }
            newParam += "count=" + count;
        }
        return newParam;
    }

    private String prepareParamSearch(String param) {
        if (param.length() > 0) {
            param += "&";
        }
        param += "include_entities=1&result_type=recent&page=1";
        if (count > 0) {
            param += "&rpp=" + count;
        }
        return param;
    }

    private String since_or_max(String sinceId) {
        if (sinceId == null) {
            return "";
        }
        String since_or_max_id = "since_id";
        if (sinceId.startsWith("-")) {
            sinceId = sinceId.substring(1);
            since_or_max_id = "max_id";
        }
        if (sinceId.length() > 0) {
            sinceId = since_or_max_id + "=" + StringUtil.urlEncode(sinceId);
        }
        return sinceId;
    }

    private String prepareParamSinceId(String sinceId) {
        return prepareParamCount(since_or_max(sinceId));
    }

    private String parseXML(String text, String prefix, String suffix) {
        String body = "";
        int i = text.indexOf(prefix);
        int j = text.indexOf(suffix);
        if (i >= 0 && j >= 0) {
            body = text.substring(i + prefix.length(), j);
        }
        return body;
    }

    private int requestFollow(String url, String param) throws TwitterException {
        try {
            String response = HttpUtil.doPost(url, param);
            if (response == null || response.length() == 0) {
                return 0;
            }
        } catch (Exception ex) {
            Log.error(ex.toString());
            throw new TwitterException(ex.getMessage());
        }
        return 1;
    }

    public void Delete(long id) throws TwitterException {
        String idstr = new Long(id).toString();
        try {
            HttpUtil.doPost("http://api.twitter.com/1/statuses/destroy/" + idstr + ".json", "");
        } catch (Exception ex) {
            Log.error(ex.toString());
            throw new TwitterException(ex.getMessage());
        }
    }
}
