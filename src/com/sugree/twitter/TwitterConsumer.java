package com.sugree.twitter;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.rms.RecordStoreException;

import org.json.me.JSONException;

import net.oauth.j2me.BadTokenStateException;
import net.oauth.j2me.Consumer;
import net.oauth.j2me.ConsumerConfig;
import net.oauth.j2me.OAuthMessage;
import net.oauth.j2me.OAuthBadDataException;
import net.oauth.j2me.OAuthServiceProviderException;
import net.oauth.j2me.token.AccessToken;
import net.oauth.j2me.token.RequestToken;
import net.oauth.j2me.Util;

import com.substanceofcode.twitter.Settings;
import com.substanceofcode.utils.Log;

public class TwitterConsumer extends Consumer {

    public static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
    public static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
    public static final String AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";
    private RequestToken requestToken;
    private AccessToken accessToken;

    public TwitterConsumer(String key, String secret) {
        super(key, secret);
        setSignatureMethod("HMAC-SHA1");
    }

    public RequestToken getRequestToken(String endpoint, String callback) throws OAuthServiceProviderException {
        RequestToken token = null;
        ConsumerConfig config = getConfig();

        OAuthMessage requestMessage = new OAuthMessage();
        requestMessage.setRequestURL(endpoint);
        requestMessage.setConsumerKey(config.getKey());
        requestMessage.setCallback(callback);
        requestMessage.createSignature(getSignatureMethod(), config.getSecret());

        String url = endpoint + "?" + requestMessage.convertToUrlParameters();
        Log.verbose("oauth request " + url);

        String responseString = null;
        try {
            responseString = Util.getViaHttpsConnection(url);
        } catch (IOException e) {
            Log.error("getViaHttpsConnection " + e.getMessage());
            Log.info(e.toString());
            return null;
        }
        Log.verbose("oauth response " + responseString);

        OAuthMessage responseMessage = new OAuthMessage();
        try {
            responseMessage.parseResponseStringForToken(responseString);
        } catch (OAuthBadDataException e) {
            Log.error("parseResponseStringForToken " + e.getMessage());
            Log.info(e.toString());
            return null;
        }
        token = new RequestToken(responseMessage.getToken(), responseMessage.getTokenSecret());
        return token;
    }

    public RequestToken fetchNewRequestToken() throws OAuthServiceProviderException {
        requestToken = getRequestToken(REQUEST_TOKEN_URL, "oob");
        return requestToken;
    }

    public AccessToken fetchNewAccessToken(String pin) throws OAuthServiceProviderException, BadTokenStateException {
        if (requestToken == null) {
            throw new BadTokenStateException("No request token set");
        }
        Log.info("oauth pin " + pin);
        accessToken = getAccessToken(ACCESS_TOKEN_URL, pin, requestToken);
        requestToken = null;
        return accessToken;
    }

    public boolean sign(HttpConnection con, String body) throws IOException {
        if (accessToken == null) {
            return false;
        }
        Log.verbose("authorization: oauth");
        sign(con, body, accessToken);
        return true;
    }

    public boolean sign(HttpConnection con, String url, String method, String body) throws IOException {
        if (accessToken == null) {
            return false;
        }
        if (url == null) {
            Log.verbose("authorization: oauth");
            sign(con, body, accessToken);
        } else {
            Log.verbose("authorization: oauth echo");
            String cred = signEcho(url, method, body);
            con.setRequestProperty("X-Auth-Service-Provider", url);
            con.setRequestProperty("X-Verify-Credentials-Authorization", cred);
        }
        return true;
    }

    public String signEcho(String url, String method, String body) throws IOException {
        HttpConnection con = (HttpConnection) Connector.open(url);
        con.setRequestMethod(method);
        sign(con, body);
        return con.getRequestProperty("Authorization");
    }

    public RequestToken getRequestToken() {
        return requestToken;
    }

    public void loadRequestToken(Settings settings) {
        requestToken = new RequestToken(
                settings.getStringProperty(Settings.OAUTH_REQUEST_TOKEN, ""),
                settings.getStringProperty(Settings.OAUTH_REQUEST_SECRET, ""));
    }

    public void saveRequestToken(Settings settings) throws IOException, RecordStoreException, JSONException {
        if (requestToken != null) {
            settings.setStringProperty(Settings.OAUTH_REQUEST_TOKEN, requestToken.getToken());
            settings.setStringProperty(Settings.OAUTH_REQUEST_SECRET, requestToken.getSecret());
        } else {
            settings.setStringProperty(Settings.OAUTH_REQUEST_TOKEN, "");
            settings.setStringProperty(Settings.OAUTH_REQUEST_SECRET, "");
        }
        settings.save(false);
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void loadAccessToken(Settings settings) {
        accessToken = new AccessToken(
                settings.getStringProperty(Settings.OAUTH_ACCESS_TOKEN, ""),
                settings.getStringProperty(Settings.OAUTH_ACCESS_SECRET, ""));
        if ("".equals(accessToken.getToken()) || "".equals(accessToken.getSecret())) {
            accessToken = null;
        }
    }

    public void saveAccessToken(Settings settings) throws IOException, RecordStoreException, JSONException {
        if (accessToken != null) {
            settings.setStringProperty(Settings.OAUTH_ACCESS_TOKEN, accessToken.getToken());
            settings.setStringProperty(Settings.OAUTH_ACCESS_SECRET, accessToken.getSecret());
            settings.setBooleanProperty(Settings.OAUTH_AUTHORIZED, true);
            settings.setStringProperty(Settings.GATEWAY, "http://api.twitter.com/");
        } else {
            settings.setStringProperty(Settings.OAUTH_ACCESS_TOKEN, "");
            settings.setStringProperty(Settings.OAUTH_ACCESS_SECRET, "");
            settings.setBooleanProperty(Settings.OAUTH_AUTHORIZED, false);
        }
        settings.save(false);
    }

    public boolean isAuthorized() {
        return accessToken != null;
    }

    public String getAuthorizeUrl() {
        return AUTHORIZE_URL + "?oauth_token=" + requestToken.getToken();
    }
}
