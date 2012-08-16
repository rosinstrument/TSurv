/*
 * StatusEntry.java
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
package com.substanceofcode.twitter.model;

import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import com.sugree.twitter.tCoResolver;
import com.sugree.utils.DateUtil;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 * StatusEntry
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class Status {

    private long id, user_id = -1;
    private String screenName;
    private String statusText;
    private Date date;
    private String source = "";
    private boolean favorited = false;
    private boolean following = false;
    private Vector media_url;
    private static Hashtable usersDb = new Hashtable();
    private static String userFields[] = {
        "screen_name",
        "name",
        "description",
        "verified",
        "lang",
        "time_zone",
        "created_at",
        "location",
        "url",
        "friends_count",
        "followers_count",
        "statuses_count",
        "favourites_count",
        "following",
        "profile_image_url",};
    private String showUserApi = "https://api.twitter.com/1/users/show.json";

    /**
     * Creates a new instance of StatusEntry
     *
     * @param screenName
     * @param statusText
     * @param date
     */
    public Status(String screen_name, JSONObject user, JSONObject status) {
        if (status != null) {
            try {
                id = status.getLong("id");
            } catch (JSONException e) {
            }
            try {
                statusText = StringUtil.decodeEntities(status.getString("text"));
            } catch (JSONException e) {
            }
            try {
                date = DateUtil.parseDate(status.getString("created_at"));
            } catch (JSONException e) {
            }
            try {
                String s = status.getString("source");
                if (s != null) {
                    source = StringUtil.removeHtml(StringUtil.decodeEntities(s));
                }
            } catch (JSONException e) {
            }
            try {
                favorited = status.getBoolean("favorited");
            } catch (JSONException e) {
            }
            try {
                following = status.getBoolean("following");
            } catch (JSONException e) {
            }
            if (user == null) {
                try {
                    user = status.getJSONObject("user");
                } catch (JSONException je) {
                }
            }
        }
        if (screen_name == null) {
            try {
                String sn = user.getString("screen_name");
                if (sn != null) {
                    screenName = StringUtil.decodeEntities(sn);
                }
            } catch (JSONException e) {
            }
        } else {
            screenName = screen_name;
        }
        if (user != null) {
            try {
                long userid = user.getLong("id");
                usersDb.put(new Long(userid), user);
                user_id = userid;
            } catch (Exception e) {
            }
        }
        Enumeration mediaUrl = findMedia(status);
        if (mediaUrl != null) {
            Vector v = new Vector();
            while (mediaUrl.hasMoreElements()) {
                v.addElement(mediaUrl.nextElement());
            }
            this.media_url = v;
        } else {
            media_url = null;
        }
    }

    private static Enumeration findMedia(JSONObject status) {
        JSONObject je;
        try {
            je = status.getJSONObject("entities");
        } catch (JSONException jsonex) {
            return null;
        }
        if (je == null) {
            return null;
        }
        try {
            JSONArray jsa = je.getJSONArray("urls");
            if (jsa != null) {
                for (int j = 0; j < jsa.length(); j++) {
                    JSONObject jo = jsa.getJSONObject(j);
                    String tco = jo.getString("url");
                    String exp = jo.getString("expanded_url");
                    if (tco != null && exp != null) {
                        tCoResolver.add(tco, exp);
                    }
                }
            }
        } catch (JSONException jsonex) {
            Log.error(jsonex.toString());
        }
        Vector media = new Vector();
        try {
            JSONArray jsa = je.getJSONArray("media");
            for (int j = 0; j < jsa.length(); j++) {
                try {
                    JSONObject jo = jsa.getJSONObject(j);
                    String mu = jo.getString("media_url");
                    if (mu != null) {
                        media.addElement(mu);
                        String url = jo.getString("url");
                        if (url != null) {
                            tCoResolver.add(url, mu);
                        }
                    }
                } catch (JSONException jsonex) {
                }
            }
        } catch (JSONException jsonex) {
        }
        return media.elements();
    }

    public long getId() {
        return id;
    }

    public Vector getMediaUrl() {
        return media_url;
    }

    public String getText() {
        return statusText;
    }

    public String getScreenName() {
        return screenName;
    }

    public Date getDate() {
        return date;
    }

    public String getSource() {
        return source;
    }

    public boolean getFavorited() {
        return favorited;
    }

    public boolean setFavorited(boolean isFavorited) {
        return favorited = isFavorited;
    }

    public String getRetweet(int maxLen) {
        int len = (maxLen - 6) - screenName.length(); // 6 is from "rt @" + ": "
        String text = statusText;
        if (text.length() > len) {
            String[] chunks = StringUtil.split(text, " ");
            int chunksLen = chunks.length;
            int urlLen = 0;
            for (int i = 0; i < chunksLen; i++) {
                if (StringUtil.isUrl(chunks[i])) {
                    urlLen = urlLen + chunks[i].length();
                }
            }
            int nonUrlLen = len - urlLen - 1; // available space for non-url chars
            StringBuffer buf = new StringBuffer(153);
            int i = 0;
            while ((buf.length() < len) && (i < chunksLen)) {
                String chunk = chunks[i];
                if (StringUtil.isUrl(chunk)) {
                    if (buf.length() > 0) {
                        buf.append(' ');
                        nonUrlLen = nonUrlLen - 1;
                    }
                    buf.append(chunk);
                    urlLen = urlLen - chunk.length();
                } else {
                    if (nonUrlLen > 3) {
                        if (buf.length() > 0) {
                            buf.append(' ');
                            nonUrlLen = nonUrlLen - 1;
                        }
                        if (nonUrlLen <= chunk.length()) {
                            chunk = chunk.substring(0, nonUrlLen - 3) + "..";
                        }
                        buf.append(chunk);
                        nonUrlLen = nonUrlLen - chunk.length();
                    }
                }
                i++;
            }
            // if it still exceed the possible max, signal it by ".."
            if (buf.length() > len) {
                text = buf.toString().substring(0, len - 2) + "..";
            } else {
                text = buf.toString();
            }
        }
        return "RT @" + screenName + ": " + text;
    }

    public Vector aboutUser() {
        if (user_id != -1) {
            JSONObject user = (JSONObject) usersDb.get(new Long(user_id));
            if (user != null) {
                return aboutVec(user);
            } else {
                try {
                    Long lid = new Long(user_id);
                    String resp = HttpUtil.doGet(showUserApi, "user_id=" + lid.toString());
                    JSONObject jo = new JSONObject(resp);
                    usersDb.put(lid, jo);
                    return aboutVec(jo);
                } catch (Exception e) {
                }
            }
        } else if (screenName != null) {
            try {
                String resp = HttpUtil.doGet(showUserApi, "screen_name=" + screenName);
                JSONObject jo = new JSONObject(resp);
                long lid = jo.getLong("id");
                user_id = lid;
                usersDb.put(new Long(lid), jo);
                return aboutVec(jo);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private Vector aboutVec(JSONObject user) {
        Vector accum = new Vector();
        for (int i = 0; i < userFields.length; i++) {
            String key = userFields[i];
            String value;
            try {
                if (key.endsWith("_count")) {
                    int v = user.getInt(key);
                    value = String.valueOf(v);
                } else {
                    value = user.getString(key);
                }
                String[] kv = {key, value};
                accum.addElement(kv);
            } catch (JSONException e) {
            }
        }
        return accum;
    }

    private String aboutStr(JSONObject user) {
        String accum = "";
        Vector a = aboutVec(user);
        for (int i = 0; i < a.size(); i++) {
            String[] kv = (String[]) a.elementAt(i);
            accum += kv[0] + ": " + kv[1] + "\n\n";
        }
        return accum;
    }

    public boolean getFollowing() {
        return following;
    }
}
