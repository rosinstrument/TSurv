package com.sugree.twitter;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import java.util.Vector;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class JSONTwitterParser {

    public static Vector parseStatuses(String payload) throws TwitterException {
        Vector statuses = new Vector();
        try {
            JSONArray json = new JSONArray(payload);
            JSONObject status = null;
            JSONObject user = null;
            for (int i = 0; i < json.length(); i++) {
                try {
                    status = json.getJSONObject(i);
                } catch (JSONException je) {
                    throw new TwitterException("expect status object " + json.get(i));
                }
                statuses.addElement(new Status(null, null, status));
            }
        } catch (JSONException e) {
            throw new TwitterException(e);
        } catch (TwitterException e) {
            throw e;
        } catch (Exception e) {
            throw new TwitterException(e);
        }
        return statuses;
    }

    public static Status parseStatus(String payload) throws TwitterException {
        Status s = null;
        try {
            JSONObject status = new JSONObject(payload);
            s = new Status(null, null, status);
        } catch (JSONException e) {
            throw new TwitterException(e);
        }
        return s;
    }

    public static Vector parseDirectMessages(String payload) throws TwitterException {
        Vector statuses = new Vector();

        try {
            JSONArray json = new JSONArray(payload);
            JSONObject message = null, user = null;
            String screenName = "";
            for (int i = 0; i < json.length(); i++) {
                try {
                    message = json.getJSONObject(i);
                    user = message.getJSONObject("sender");
                } catch (JSONException je) {
                    throw new TwitterException("expect message object " + je + " " + json.get(i));
                }
                try {
                    screenName = message.getString("sender_screen_name");
                } catch (JSONException je) {
                    throw new TwitterException("expect sender screen name " + je + " " + json.get(i));
                }
                statuses.addElement(new Status(screenName, user, message));
            }
        } catch (JSONException e) {
            throw new TwitterException(e);
        } catch (TwitterException e) {
            throw e;
        } catch (Exception e) {
            throw new TwitterException(e);
        }
        return statuses;
    }

    /*    public static String parseTest(String payload) throws TwitterException {
     String s = null;
     try {
     JSONObject o = new JSONObject(payload);
     s = o.toString(2);
     } catch (JSONException e) {
     s = payload;
     } catch (Exception e) {
     throw new TwitterException(e);
     }
     return s;
     }

     public static String parseScheduleDowntime(String payload) throws TwitterException {
     String s = null;
     try {
     JSONObject o = new JSONObject(payload);
     s = o.toString(2);
     } catch (JSONException e) {
     s = payload;
     } catch (Exception e) {
     throw new TwitterException(e);
     }
     return s;
     }
     */
    public static Vector parseTwiErrorsResponse(String response) {
        Vector ret = new Vector();
        try {
            JSONObject o = new JSONObject(response);
            JSONArray a;
            try {
                a = o.getJSONArray("errors");
                if (a != null) {
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject jo = a.getJSONObject(i);
                        if (jo != null) {
                            int code = jo.getInt("code");
                            String message = jo.getString("message");
                            if (message != null) {
                                ret.addElement(message);
                            }
                        }
                    }
                }
            } catch (JSONException je) {
                String ser = o.getString("error");
                ret.addElement(ser);
            }
        } catch (JSONException e) {
        }
        return ret;
    }

    public static String parse400(String payload) throws TwitterException {
        String s = null;
        try {
            JSONObject o = new JSONObject(payload);
            s = o.getString("error");
        } catch (JSONException e) {
            s = payload;
        } catch (Exception e) {
            throw new TwitterException(e);
        }
        return s;
    }

    public static Vector parseSearch(String response) {
        Vector sa = new Vector();
        try {
            JSONArray ja = new JSONObject(response).getJSONArray("results");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject status = ja.getJSONObject(i);
                String screenName = StringUtil.decodeEntities(status.getString("from_user"));
                sa.addElement(new Status(screenName, null, status));
            }
        } catch (JSONException je) {
            Log.error(je.toString());
        }
        return sa;
    }
}
